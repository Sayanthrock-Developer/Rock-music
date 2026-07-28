package com.rockmusic.app.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider

class FakeTokenVault : TokenVault(ApplicationProvider.getApplicationContext<Context>()) {
    private val store = mutableMapOf<String, String>()

    override fun put(key: String, value: String) {
        store[key] = value
    }

    override fun get(key: String): String? {
        return store[key]
    }

    override fun remove(key: String) {
        store.remove(key)
    }

    override fun clear() {
        store.clear()
    }
}
