package com.rockmusic.app.player

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences

class FakeContext(private val prefs: SharedPreferences) : ContextWrapper(null) {
    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences = prefs
}

class FakeSharedPreferences : SharedPreferences {
    val map = mutableMapOf<String, Boolean>()

    override fun getAll(): MutableMap<String, *> = TODO()
    override fun getString(key: String?, defValue: String?): String? = TODO()
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? = TODO()
    override fun getInt(key: String?, defValue: Int): Int = TODO()
    override fun getLong(key: String?, defValue: Long): Long = TODO()
    override fun getFloat(key: String?, defValue: Float): Float = TODO()

    override fun getBoolean(key: String?, defValue: Boolean): Boolean = map[key ?: ""] ?: defValue

    override fun contains(key: String?): Boolean = TODO()
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = TODO()
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) = TODO()

    override fun edit(): SharedPreferences.Editor = FakeEditor()

    inner class FakeEditor : SharedPreferences.Editor {
        override fun putString(key: String?, value: String?): SharedPreferences.Editor = TODO()
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor = TODO()
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor = TODO()
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor = TODO()
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor = TODO()

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            map[key ?: ""] = value
            return this
        }

        override fun remove(key: String?): SharedPreferences.Editor = TODO()
        override fun clear(): SharedPreferences.Editor = TODO()
        override fun commit(): Boolean = TODO()

        override fun apply() {}
    }
}
