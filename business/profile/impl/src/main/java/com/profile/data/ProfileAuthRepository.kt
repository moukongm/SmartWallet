package com.profile.data

import com.example.network.SupabaseProvider
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.user.UserInfo
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

class ProfileAuthRepository {
    private val supabase = SupabaseProvider.client

    suspend fun register(
        email: String,
        password: String,
        nickname: String
    ): Boolean {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
            data = buildJsonObject {
                put("nickname", JsonPrimitive(nickname))
            }
        }
        val authenticated = isLoggedIn()
        if (authenticated) {
            ProfileLoginStateStorage.saveLoggedIn(currentUserId())
            ProfileStateHolder.update(currentProfile())
        }
        return authenticated
    }

    suspend fun login(
        email: String,
        password: String
    ): Boolean {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        val authenticated = isLoggedIn()
        if (authenticated) {
            ProfileLoginStateStorage.saveLoggedIn(currentUserId())
            ProfileStateHolder.update(currentProfile())
        }
        return authenticated
    }

    suspend fun logout() {
        try {
            supabase.auth.signOut()
        } finally {
            ProfileLoginStateStorage.clear()
            ProfileStateHolder.clear()
        }
    }

    fun isLoggedIn(): Boolean {
        return supabase.auth.currentUserOrNull() != null
    }

    fun currentUserId(): String? {
        return supabase.auth.currentUserOrNull()?.id
    }

    fun currentProfile(): UserProfile? {
        return supabase.auth
            .currentUserOrNull()
            ?.toUserProfile()
    }

    suspend fun updateNickname(
        nickname: String
    ): UserProfile {
        val user = supabase.auth.updateUser {
            data{
                put(
                    "nickname",
                    JsonPrimitive(nickname)
                )
            }
        }
        return user.toUserProfile().also(ProfileStateHolder::update)
    }

    suspend fun updateProfile(
        nickname: String,
        email: String,
        avatarUri: String
    ): UserProfile {
        val user = supabase.auth.updateUser {
            this.email = email
            data {
                put("nickname", JsonPrimitive(nickname))
                put("avatar_uri", JsonPrimitive(avatarUri))
            }
        }
        return user.toUserProfile().also(ProfileStateHolder::update)
    }

    private fun UserInfo.toUserProfile(): UserProfile {
        val nickname = userMetadata
            ?.get("nickname")
            ?.jsonPrimitive
            ?.contentOrNull
            .orEmpty()

        val avatarUri = userMetadata
            ?.get("avatar_uri")
            ?.jsonPrimitive
            ?.contentOrNull
            .orEmpty()

        return UserProfile(
            userId = id,
            nickname = nickname,
            email = email.orEmpty(),
            avatarUri = avatarUri
        )
    }
}
