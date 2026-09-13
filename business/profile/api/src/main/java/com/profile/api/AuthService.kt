package com.profile.api

import com.alibaba.android.arouter.facade.template.IProvider
import com.profile.api.model.UserProfileSnapshot
import kotlinx.coroutines.flow.StateFlow

interface AuthService : IProvider {

    fun isLoggedIn(): Boolean

    fun currentUserId(): String?

    fun observeProfile(): StateFlow<UserProfileSnapshot?>

    fun refreshProfile()
}
