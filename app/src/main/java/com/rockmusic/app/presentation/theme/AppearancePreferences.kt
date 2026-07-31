package com.rockmusic.app.presentation.theme

import android.content.Context
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

private const val PREFERENCES_NAME = "rock_music_appearance"
private const val KEY_THEME_MODE = "theme_mode"
private const val KEY_SYSTEM_COLOR = "system_color"
private const val KEY_BLUR_FRAMES = "blur_frames"

data class AppearanceSettings(
    val themeMode: RockThemeMode = RockThemeMode.SYSTEM,
    val useSystemColor: Boolean = true,
    val useBlurFrames: Boolean = true,
)

val AppearanceSettingsSaver: Saver<AppearanceSettings, Any> = listSaver(
    save = { settings ->
        listOf(
            settings.themeMode.name,
            settings.useSystemColor,
            settings.useBlurFrames,
        )
    },
    restore = { values ->
        AppearanceSettings(
            themeMode = runCatching {
                RockThemeMode.valueOf(values[0] as String)
            }.getOrDefault(RockThemeMode.SYSTEM),
            useSystemColor = values[1] as Boolean,
            useBlurFrames = values[2] as Boolean,
        )
    },
)

class AppearancePreferences(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val preferences = EncryptedSharedPreferences.create(
        context,
        PREFERENCES_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    fun load(): AppearanceSettings = AppearanceSettings(
        themeMode = runCatching {
            RockThemeMode.valueOf(
                preferences.getString(KEY_THEME_MODE, RockThemeMode.SYSTEM.name)
                    ?: RockThemeMode.SYSTEM.name,
            )
        }.getOrDefault(RockThemeMode.SYSTEM),
        useSystemColor = preferences.getBoolean(KEY_SYSTEM_COLOR, true),
        useBlurFrames = preferences.getBoolean(KEY_BLUR_FRAMES, true),
    )

    fun save(settings: AppearanceSettings) {
        preferences.edit()
            .putString(KEY_THEME_MODE, settings.themeMode.name)
            .putBoolean(KEY_SYSTEM_COLOR, settings.useSystemColor)
            .putBoolean(KEY_BLUR_FRAMES, settings.useBlurFrames)
            .apply()
    }
}
