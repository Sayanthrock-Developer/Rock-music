package com.rockmusic.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.rockmusic.app.player.MediaSessionCommands
import com.rockmusic.app.presentation.FolderManagerScreen
import com.rockmusic.app.presentation.RockMusicExperience
import com.rockmusic.app.presentation.SongManagerScreen
import com.rockmusic.app.presentation.integrations.IntegrationConnectionsScreen
import com.rockmusic.app.presentation.settings.AppearanceScreen
import com.rockmusic.app.presentation.theme.AppearancePreferences
import com.rockmusic.app.presentation.theme.AppearanceSettingsSaver
import com.rockmusic.app.presentation.theme.RockMusicTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val externalPlayerSurface = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        externalPlayerSurface.value = intent?.getStringExtra(
            MediaSessionCommands.EXTRA_OPEN_PLAYER_SURFACE,
        )
        enableEdgeToEdge()
        setContent {
            val appearancePreferences = remember { AppearancePreferences(applicationContext) }
            val lifecycleOwner = LocalLifecycleOwner.current
            val requestedPlayerSurface by externalPlayerSurface
            val audioPermission = if (Build.VERSION.SDK_INT >= 33) {
                Manifest.permission.READ_MEDIA_AUDIO
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
            var audioPermissionGranted by remember {
                mutableStateOf(
                    ContextCompat.checkSelfPermission(
                        applicationContext,
                        audioPermission,
                    ) == PackageManager.PERMISSION_GRANTED,
                )
            }
            var appearance by rememberSaveable(stateSaver = AppearanceSettingsSaver) {
                mutableStateOf(appearancePreferences.load())
            }
            var showConnections by rememberSaveable { mutableStateOf(false) }
            var showAppearance by rememberSaveable { mutableStateOf(false) }
            var showSongManager by rememberSaveable { mutableStateOf(false) }
            var showFolderManager by rememberSaveable { mutableStateOf(false) }

            DisposableEffect(lifecycleOwner, audioPermission) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        audioPermissionGranted = ContextCompat.checkSelfPermission(
                            applicationContext,
                            audioPermission,
                        ) == PackageManager.PERMISSION_GRANTED
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            RockMusicTheme(
                mode = appearance.themeMode,
                dynamicColor = appearance.useSystemColor,
                rockRedAccent = !appearance.useSystemColor,
            ) {
                BackHandler(
                    enabled = showConnections || showAppearance || showSongManager || showFolderManager,
                ) {
                    showConnections = false
                    showAppearance = false
                    showSongManager = false
                    showFolderManager = false
                }

                Box(Modifier.fillMaxSize()) {
                    when {
                        showFolderManager -> FolderManagerScreen(
                            onClose = { showFolderManager = false },
                        )

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

                        else -> key(audioPermissionGranted) {
                            RockMusicExperience(
                                useBlurFrames = appearance.useBlurFrames,
                                requestedPlayerSurface = requestedPlayerSurface,
                                onRequestedPlayerSurfaceConsumed = {
                                    externalPlayerSurface.value = null
                                },
                                onOpenAppearance = { showAppearance = true },
                                onOpenConnections = { showConnections = true },
                                onOpenSongManager = { showSongManager = true },
                                onOpenFolderManager = { showFolderManager = true },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        externalPlayerSurface.value = intent.getStringExtra(
            MediaSessionCommands.EXTRA_OPEN_PLAYER_SURFACE,
        )
    }
}
