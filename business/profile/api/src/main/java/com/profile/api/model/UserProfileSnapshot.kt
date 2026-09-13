package com.profile.api.model

data class UserProfileSnapshot(
    val userId: String,
    val nickname: String,
    val email: String,
    val avatarUri: String = ""
)
