package com.profile.data

import com.example.storage.MmkvStorage

internal object ProfileLoginStateStorage {

    private const val STORAGE_ID = "smart_wallet_login_state"
    private const val KEY_IS_LOGGED_IN = "is_logged_in"
    private const val KEY_USER_ID = "user_id"

    fun saveLoggedIn(userId: String?) {
        MmkvStorage.putBoolean(STORAGE_ID, KEY_IS_LOGGED_IN, true)
        MmkvStorage.putString(STORAGE_ID, KEY_USER_ID, userId)
    }

    fun isLoggedIn(): Boolean {
        return MmkvStorage.getBoolean(STORAGE_ID, KEY_IS_LOGGED_IN)
    }

    fun currentUserId(): String? {
        return MmkvStorage.getString(STORAGE_ID, KEY_USER_ID)
    }

    fun clear() {
        MmkvStorage.remove(STORAGE_ID, KEY_IS_LOGGED_IN, KEY_USER_ID)
    }
}
