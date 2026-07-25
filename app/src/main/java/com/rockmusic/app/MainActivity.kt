package com.rockmusic.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.rockmusic.app.presentation.RockMusicRoot
import com.rockmusic.app.presentation.integrations.IntegrationConnectionsScreen
import com.rockmusic.app.presentation.settings.AppearanceScreen
import com.rockmusic.app.presentation.theme.AppearancePreferences
import com.rockmusic.app.presentation.theme.RockMusicTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appearancePreferences = remember { AppearancePreferences(applicationContext) }
            var appearance by remember { mutableStateOf(appearancePreferences.load()) }
            var showConnections by rememberSaveable { mutableStateOf(false) }
            var showAppearance by rememberSaveable { mutableStateOf(false) }

            RockMusicTheme(
                mode = appearance.themeMode,
                dynamicColor = appearance.useSystemColor,
                rockRedAccent = !appearance.useSystemColor,
            ) {
                BackHandler(enabled = showConnections || showAppearance) {
                    showConnections = false
                    showAppearance = false
                }

                Box(Modifier.fillMaxSize()) {
                    when {
                        showAppearance -> AppearanceScreen(
                            settings = appearance,
                            onChange = { updated ->
                                appearance = updated
                                appearancePreferences.save(updated)
                            },
                            onClose = { showAppearance = false },
                        )

                        showConnections -> IntegrationConnectionsScreen(
                            onClose = { showConnections = false },
                        )

                        else -> RockMusicRoot(
                            useBlurFrames = appearance.useBlurFrames,
                            onOpenAppearance = { showAppearance = true },
                            onOpenConnections = { showConnections = true },
                        )
                    }
                }
            }
        }
    }
}
