package com.rockmusic.app.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val RockRed = Color(0xFFFF3347)
private val AmoledBlack = Color(0xFF000000)
private val DarkBackground = Color(0xFF090A0D)
private val DarkSurface = Color(0xFF12141A)
private val DarkSurfaceVariant = Color(0xFF20232B)
private val LightBackground = Color(0xFFF7F7FA)
private val LightSurface = Color(0xFFFFFFFF)
private val LightSurfaceVariant = Color(0xFFE9EAF0)

enum class RockThemeMode { SYSTEM, LIGHT, DARK, AMOLED }

private val RockTypography = Typography().let { base ->
    base.copy(
        displaySmall = base.displaySmall.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = (-1.2).sp,
        ),
        headlineSmall = base.headlineSmall.copy(
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.35).sp,
        ),
        titleLarge = base.titleLarge.copy(fontWeight = FontWeight.Bold),
    )
}

@Composable
fun RockMusicTheme(
    mode: RockThemeMode = RockThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    rockRedAccent: Boolean = !dynamicColor,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val useDark = mode == RockThemeMode.DARK || mode == RockThemeMode.AMOLED ||
        (mode == RockThemeMode.SYSTEM && systemDark)
    val context = LocalContext.current

    val scheme = when {
        mode == RockThemeMode.AMOLED -> darkColorScheme(
            primary = RockRed,
            secondary = RockRed,
            background = AmoledBlack,
            surface = AmoledBlack,
            surfaceVariant = DarkSurface,
        )

        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (useDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        useDark -> darkColorScheme(
            primary = RockRed,
            secondary = Color(0xFFFF8692),
            background = DarkBackground,
            surface = DarkSurface,
            surfaceVariant = DarkSurfaceVariant,
            surfaceContainer = Color(0xFF171920),
            surfaceContainerHigh = Color(0xFF1D2028),
        )

        else -> lightColorScheme(
            primary = RockRed,
            secondary = Color(0xFF9D2330),
            background = LightBackground,
            surface = LightSurface,
            surfaceVariant = LightSurfaceVariant,
            surfaceContainer = Color(0xFFF0F1F5),
            surfaceContainerHigh = Color(0xFFE8E9EE),
        )
    }

    val finalScheme = if (rockRedAccent && mode != RockThemeMode.AMOLED) {
        scheme.copy(primary = RockRed, secondary = RockRed)
    } else {
        scheme
    }

    MaterialTheme(
        colorScheme = finalScheme,
        typography = RockTypography,
        content = content,
    )
}
