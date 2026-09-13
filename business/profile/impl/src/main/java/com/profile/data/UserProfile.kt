package com.profile.data

data class UserProfile(
    val userId: String,
    val nickname: String,
    val email: String,
    val avatarUri: String = ""
)
