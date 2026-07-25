package com.rockmusic.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rockmusic.app.presentation.RockMusicRoot
import com.rockmusic.app.presentation.SongManagerScreen
import com.rockmusic.app.presentation.integrations.IntegrationConnectionsScreen
import com.rockmusic.app.presentation.settings.AppearanceScreen
import com.rockmusic.app.presentation.theme.AppearancePreferences
import com.rockmusic.app.presentation.theme.AppearanceSettingsSaver
import com.rockmusic.app.presentation.theme.RockMusicTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val appearancePreferences = remember { AppearancePreferences(applicationContext) }
            var appearance by rememberSaveable(stateSaver = AppearanceSettingsSaver) {
                mutableStateOf(appearancePreferences.load())
            }
            var showConnections by rememberSaveable { mutableStateOf(false) }
            var showAppearance by rememberSaveable { mutableStateOf(false) }
            var showSongManager by rememberSaveable { mutableStateOf(false) }

            RockMusicTheme(
                mode = appearance.themeMode,
                dynamicColor = appearance.useSystemColor,
                rockRedAccent = !appearance.useSystemColor,
            ) {
                BackHandler(enabled = showConnections || showAppearance || showSongManager) {
                    showConnections = false
                    showAppearance = false
                    showSongManager = false
                }

                Box(Modifier.fillMaxSize()) {
                    when {
                        showSongManager -> SongManagerScreen(
                            onClose = { showSongManager = false },
                        )

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

                        else -> {
                            RockMusicRoot(
                                useBlurFrames = appearance.useBlurFrames,
                                onOpenAppearance = { showAppearance = true },
                                onOpenConnections = { showConnections = true },
                            )
                            SmallFloatingActionButton(
                                onClick = { showSongManager = true },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .statusBarsPadding()
                                    .padding(top = 8.dp, end = 14.dp),
                            ) {
                                Icon(
                                    Icons.Rounded.LibraryMusic,
                                    contentDescription = "Open Song Manager",
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
