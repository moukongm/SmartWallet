package com.profile.data

import android.content.Context
import com.alibaba.android.arouter.facade.annotation.Route
import com.example.common.router.RouterPath
import com.profile.api.AuthService
import com.profile.api.model.UserProfileSnapshot
import kotlinx.coroutines.flow.StateFlow

@Route(path = RouterPath.USER_AUTH_SERVICE)
class AuthServiceImpl : AuthService {

    private val repository = ProfileAuthRepository()

    override fun init(context: Context?) = Unit

    override fun isLoggedIn(): Boolean {
        return ProfileLoginStateStorage.isLoggedIn()
    }

    override fun currentUserId(): String? {
        return ProfileLoginStateStorage.currentUserId()
    }

    override fun observeProfile(): StateFlow<UserProfileSnapshot?> {
        return ProfileStateHolder.profile
    }

    override fun refreshProfile() {
        ProfileStateHolder.update(repository.currentProfile())
    }
}
