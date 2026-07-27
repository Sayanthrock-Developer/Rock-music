package com.rockmusic.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.rockmusic.app.data.integration.SpotifyAuthorizationCallbackHandler
import com.rockmusic.app.player.MediaSessionCommands
import com.rockmusic.app.presentation.FolderManagerScreen
import com.rockmusic.app.presentation.SongManagerScreen
import com.rockmusic.app.presentation.ValidatedSystemMediaExperience
import com.rockmusic.app.presentation.integrations.IntegrationConnectionsScreen
import com.rockmusic.app.presentation.settings.AppearanceScreen
import com.rockmusic.app.presentation.theme.AppearancePreferences
import com.rockmusic.app.presentation.theme.AppearanceSettingsSaver
import com.rockmusic.app.presentation.theme.RockMusicTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var spotifyAuthorizationCallbackHandler: SpotifyAuthorizationCallbackHandler

    private val externalPlayerSurface = mutableStateOf<String?>(null)
    private val openConnectionsFromCallback = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        consumePlayerSurface(intent)
        handleIntegrationCallback(intent)
        enableEdgeToEdge()
        setContent {
            val requestedPlayerSurface by externalPlayerSurface
            val callbackRequestedConnections by openConnectionsFromCallback

            RockMusicApp(
                requestedPlayerSurface = requestedPlayerSurface,
                onRequestedPlayerSurfaceConsumed = { externalPlayerSurface.value = null },
                callbackRequestedConnections = callbackRequestedConnections,
                onConnectionsCallbackConsumed = { openConnectionsFromCallback.value = false }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumePlayerSurface(intent)
        handleIntegrationCallback(intent)
    }

    private fun consumePlayerSurface(intent: Intent?) {
        externalPlayerSurface.value = intent?.getStringExtra(
            MediaSessionCommands.EXTRA_OPEN_PLAYER_SURFACE,
        )
        intent?.removeExtra(MediaSessionCommands.EXTRA_OPEN_PLAYER_SURFACE)
    }

    private fun handleIntegrationCallback(intent: Intent?) {
        val callbackUri = intent?.data?.takeIf(spotifyAuthorizationCallbackHandler::canHandle)
            ?: return
        intent.setData(null)
        lifecycleScope.launch {
            spotifyAuthorizationCallbackHandler.handle(callbackUri.toString())
                .onSuccess { message ->
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_LONG).show()
                    openConnectionsFromCallback.value = true
                }
                .onFailure { error ->
                    Toast.makeText(
                        this@MainActivity,
                        error.message ?: "Spotify authorization failed.",
                        Toast.LENGTH_LONG,
                    ).show()
                    openConnectionsFromCallback.value = true
                }
        }
    }
}

@Composable
private fun RockMusicApp(
    requestedPlayerSurface: String?,
    onRequestedPlayerSurfaceConsumed: () -> Unit,
    callbackRequestedConnections: Boolean,
    onConnectionsCallbackConsumed: () -> Unit
) {
    val applicationContext = LocalContext.current.applicationContext
    val appearancePreferences = remember { AppearancePreferences(applicationContext) }
    val lifecycleOwner = LocalLifecycleOwner.current
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

    LaunchedEffect(callbackRequestedConnections) {
        if (callbackRequestedConnections) {
            showConnections = true
            onConnectionsCallbackConsumed()
        }
    }

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
                    ValidatedSystemMediaExperience(
                        useBlurFrames = appearance.useBlurFrames,
                        requestedPlayerSurface = requestedPlayerSurface,
                        onRequestedPlayerSurfaceConsumed = onRequestedPlayerSurfaceConsumed,
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
