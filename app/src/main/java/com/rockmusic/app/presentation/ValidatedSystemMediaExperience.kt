package com.rockmusic.app.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rockmusic.app.player.MediaSessionCommands

@Composable
fun ValidatedSystemMediaExperience(
    useBlurFrames: Boolean,
    requestedPlayerSurface: String?,
    onRequestedPlayerSurfaceConsumed: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenConnections: () -> Unit,
    onOpenSongManager: () -> Unit,
    onOpenFolderManager: () -> Unit,
    mainViewModel: MainViewModel = hiltViewModel(),
) {
    val player by mainViewModel.playerState.collectAsStateWithLifecycle()
    val validatedSurface = requestedPlayerSurface?.takeIf {
        it == MediaSessionCommands.SURFACE_LYRICS
    }

    LaunchedEffect(requestedPlayerSurface, validatedSurface, player.hasMedia) {
        if (requestedPlayerSurface != null && (validatedSurface == null || !player.hasMedia)) {
            onRequestedPlayerSurfaceConsumed()
        }
    }

    SystemMediaExperience(
        useBlurFrames = useBlurFrames,
        requestedPlayerSurface = validatedSurface.takeIf { player.hasMedia },
        onRequestedPlayerSurfaceConsumed = onRequestedPlayerSurfaceConsumed,
        onOpenAppearance = onOpenAppearance,
        onOpenConnections = onOpenConnections,
        onOpenSongManager = onOpenSongManager,
        onOpenFolderManager = onOpenFolderManager,
        mainViewModel = mainViewModel,
    )
}
