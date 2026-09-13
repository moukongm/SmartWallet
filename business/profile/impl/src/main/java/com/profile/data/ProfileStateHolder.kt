package com.profile.data

import com.profile.api.model.UserProfileSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal object ProfileStateHolder {
    private val mutableProfile = MutableStateFlow<UserProfileSnapshot?>(null)
    val profile: StateFlow<UserProfileSnapshot?> = mutableProfile.asStateFlow()

    fun update(profile: UserProfile?) {
        mutableProfile.value = profile?.let {
            UserProfileSnapshot(
                userId = it.userId,
                nickname = it.nickname,
                email = it.email,
                avatarUri = it.avatarUri
            )
        }
    }

    fun clear() {
        mutableProfile.value = null
    }
}
