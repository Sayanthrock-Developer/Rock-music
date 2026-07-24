package com.rockmusic.app.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val RockRed = Color(0xFFE21D2D)
private val AmoledBlack = Color(0xFF000000)
private val DarkSurface = Color(0xFF151515)
private val LightSurface = Color(0xFFFFFBFE)

enum class RockThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

@Composable
fun RockMusicTheme(
    mode: RockThemeMode = RockThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    rockRedAccent: Boolean = true,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = mode == RockThemeMode.DARK || mode == RockThemeMode.AMOLED ||
        (mode == RockThemeMode.SYSTEM && systemDark)
    val context = LocalContext.current

    val scheme = when {
        mode == RockThemeMode.AMOLED -> darkColorScheme(
            primary = RockRed,
            background = AmoledBlack,
            surface = AmoledBlack,
            surfaceVariant = DarkSurface,
        )
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        useDark -> darkColorScheme(
            primary = RockRed,
            background = Color(0xFF0D0D0F),
            surface = DarkSurface,
        )
        else -> lightColorScheme(
            primary = RockRed,
            background = LightSurface,
            surface = Color.White,
        )
    }

    val finalScheme = if (rockRedAccent && mode != RockThemeMode.AMOLED) {
        scheme.copy(primary = RockRed, secondary = RockRed)
    } else {
        scheme
    }

    MaterialTheme(
        colorScheme = finalScheme,
        typography = MaterialTheme.typography,
        content = content,
    )
}
