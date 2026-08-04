package com.rockmusic.app.presentation.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.compose.runtime.saveable.SaverScope

class FakeAppearancePreferences(context: Context) : AppearancePreferences(context) {
    private val store = mutableMapOf<String, Any>()

    override val preferences: SharedPreferences = object : SharedPreferences {
        override fun getAll(): MutableMap<String, *> = store.toMutableMap()
        override fun getString(key: String, defValue: String?): String? = store[key] as? String ?: defValue
        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? { @Suppress("UNCHECKED_CAST") return store[key] as? MutableSet<String> ?: defValues }
        override fun getInt(key: String, defValue: Int): Int = store[key] as? Int ?: defValue
        override fun getLong(key: String, defValue: Long): Long = store[key] as? Long ?: defValue
        override fun getFloat(key: String, defValue: Float): Float = store[key] as? Float ?: defValue
        override fun getBoolean(key: String, defValue: Boolean): Boolean = store[key] as? Boolean ?: defValue
        override fun contains(key: String): Boolean = store.containsKey(key)

        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            override fun putString(key: String, value: String?): SharedPreferences.Editor {
                if (value == null) store.remove(key) else store[key] = value
                return this
            }
            override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor {
                if (values == null) store.remove(key) else store[key] = values
                return this
            }
            override fun putInt(key: String, value: Int): SharedPreferences.Editor { store[key] = value; return this }
            override fun putLong(key: String, value: Long): SharedPreferences.Editor { store[key] = value; return this }
            override fun putFloat(key: String, value: Float): SharedPreferences.Editor { store[key] = value; return this }
            override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor { store[key] = value; return this }
            override fun remove(key: String): SharedPreferences.Editor { store.remove(key); return this }
            override fun clear(): SharedPreferences.Editor { store.clear(); return this }
            override fun commit(): Boolean = true
            override fun apply() {}
        }

        override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {}
        override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {}
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AppearancePreferencesTest {

    private lateinit var preferences: FakeAppearancePreferences

    @Before
    fun setup() {
        preferences = FakeAppearancePreferences(ApplicationProvider.getApplicationContext())
    }

    @Test
    fun load_returnsDefaultSettings_whenNoPreferencesSaved() {
        val settings = preferences.load()

        assertEquals(RockThemeMode.SYSTEM, settings.themeMode)
        assertTrue(settings.useSystemColor)
        assertTrue(settings.useBlurFrames)
    }

    @Test
    fun save_and_load_restoresSavedSettings() {
        val customSettings = AppearanceSettings(
            themeMode = RockThemeMode.DARK,
            useSystemColor = false,
            useBlurFrames = false,
        )

        preferences.save(customSettings)

        val loadedSettings = preferences.load()

        assertEquals(RockThemeMode.DARK, loadedSettings.themeMode)
        assertFalse(loadedSettings.useSystemColor)
        assertFalse(loadedSettings.useBlurFrames)
    }

    @Test
    fun load_handlesInvalidEnumSafely() {
        preferences.preferences.edit().putString("theme_mode", "INVALID_ENUM_VALUE").commit()
        val loadedSettings = preferences.load()
        assertEquals(RockThemeMode.SYSTEM, loadedSettings.themeMode)
    }

    @Test
    fun saver_savesAndRestoresCorrectly() {
        val customSettings = AppearanceSettings(
            themeMode = RockThemeMode.LIGHT,
            useSystemColor = false,
            useBlurFrames = false,
        )

        val savedList = with(AppearanceSettingsSaver) {
            SaverScope { true }.save(customSettings)
        } as List<*>

        assertEquals(RockThemeMode.LIGHT.name, savedList[0])
        assertEquals(false, savedList[1])
        assertEquals(false, savedList[2])

        val restoredSettings = AppearanceSettingsSaver.restore(savedList as Any)

        assertEquals(customSettings, restoredSettings)
    }

    @Test
    fun saver_handlesInvalidEnumSafely() {
        val invalidList = listOf(
            "INVALID_ENUM_VALUE",
            true,
            true,
        )

        val restoredSettings = AppearanceSettingsSaver.restore(invalidList)

        assertEquals(RockThemeMode.SYSTEM, restoredSettings?.themeMode)
    }
}
