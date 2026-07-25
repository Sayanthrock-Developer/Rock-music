package com.rockmusic.app.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.SettingsBrightness
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rockmusic.app.presentation.theme.AppearanceSettings
import com.rockmusic.app.presentation.theme.RockThemeMode

@Composable
fun AppearanceScreen(
    settings: AppearanceSettings,
    onChange: (AppearanceSettings) -> Unit,
    onClose: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(PaddingValues(horizontal = 20.dp, vertical = 28.dp)),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.displaySmall,
                )
                Text(
                    text = "Theme, system colour, blur frames, and navigation style.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.Close, contentDescription = "Close appearance settings")
            }
        }

        Text("Theme mode", style = MaterialTheme.typography.titleLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeChip(
                label = "System",
                icon = Icons.Rounded.SettingsBrightness,
                selected = settings.themeMode == RockThemeMode.SYSTEM,
                onClick = { onChange(settings.copy(themeMode = RockThemeMode.SYSTEM)) },
            )
            ThemeChip(
                label = "Light",
                icon = Icons.Rounded.LightMode,
                selected = settings.themeMode == RockThemeMode.LIGHT,
                onClick = { onChange(settings.copy(themeMode = RockThemeMode.LIGHT)) },
            )
            ThemeChip(
                label = "Dark",
                icon = Icons.Rounded.DarkMode,
                selected = settings.themeMode == RockThemeMode.DARK,
                onClick = { onChange(settings.copy(themeMode = RockThemeMode.DARK)) },
            )
        }

        SettingToggle(
            title = "System colour",
            description = "Use Android wallpaper colours on supported devices.",
            icon = Icons.Rounded.Palette,
            checked = settings.useSystemColor,
            onCheckedChange = { onChange(settings.copy(useSystemColor = it)) },
        )

        SettingToggle(
            title = "Blur frame navigation",
            description = "Use translucent rounded frames for navigation, cards, and the mini player.",
            icon = Icons.Rounded.BlurOn,
            checked = settings.useBlurFrames,
            onCheckedChange = { onChange(settings.copy(useBlurFrames = it)) },
        )

        Text("Preview", style = MaterialTheme.typography.titleLarge)
        AppearancePreview(useBlurFrames = settings.useBlurFrames)
    }
}

@Composable
private fun ThemeChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = { Icon(icon, contentDescription = null) },
    )
}

@Composable
private fun SettingToggle(
    title: String,
    description: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(shape = RoundedCornerShape(24.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.padding(12.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(
                    description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun AppearancePreview(useBlurFrames: Boolean) {
    val shape = RoundedCornerShape(30.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.2f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .padding(18.dp),
    ) {
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (useBlurFrames) Color.White.copy(alpha = 0.24f) else Color.Transparent,
                    shape = RoundedCornerShape(22.dp),
                ),
            shape = RoundedCornerShape(22.dp),
            color = if (useBlurFrames) {
                MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)
            } else {
                MaterialTheme.colorScheme.surface
            },
            tonalElevation = if (useBlurFrames) 0.dp else 4.dp,
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .height(54.dp)
                        .weight(0.22f),
                ) {}
                Column(
                    modifier = Modifier
                        .weight(0.78f)
                        .padding(start = 14.dp),
                ) {
                    Text("Rock Music", fontWeight = FontWeight.Black)
                    Text(
                        "New visual system",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
