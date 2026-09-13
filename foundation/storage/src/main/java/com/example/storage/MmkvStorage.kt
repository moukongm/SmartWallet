package com.example.storage

import com.tencent.mmkv.MMKV

object MmkvStorage {

    private fun storage(storageId: String): MMKV {
        return requireNotNull(MMKV.mmkvWithID(storageId)) {
            "MMKV 尚未初始化，请确认 Application 继承 BaseApplication"
        }
    }

    fun putBoolean(storageId: String, key: String, value: Boolean) {
        storage(storageId).encode(key, value)
    }

    fun getBoolean(
        storageId: String,
        key: String,
        defaultValue: Boolean = false
    ): Boolean {
        return storage(storageId).decodeBool(key, defaultValue)
    }

    fun putString(storageId: String, key: String, value: String?) {
        if (value == null) {
            storage(storageId).removeValueForKey(key)
        } else {
            storage(storageId).encode(key, value)
        }
    }

    fun getString(storageId: String, key: String): String? {
        return storage(storageId).decodeString(key)
    }

    fun remove(storageId: String, vararg keys: String) {
        storage(storageId).removeValuesForKeys(keys)
    }

    fun putLong(
        storageId: String,
        key: String,
        value: Long
    ) {
        storage(storageId).encode(key, value)
    }

    fun getLong(
        storageId: String,
        key: String,
        defaultValue: Long = 0L
    ): Long {
        return storage(storageId).decodeLong(
            key,
            defaultValue
        )
    }
}
