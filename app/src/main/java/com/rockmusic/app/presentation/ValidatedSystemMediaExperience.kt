package com.rockmusic.app.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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

    LaunchedEffect(requestedPlayerSurface, player.hasMedia) {
        if (requestedPlayerSurface != null && !player.hasMedia) {
            onRequestedPlayerSurfaceConsumed()
        }
    }

    SystemMediaExperience(
        useBlurFrames = useBlurFrames,
        requestedPlayerSurface = requestedPlayerSurface.takeIf { player.hasMedia },
        onRequestedPlayerSurfaceConsumed = onRequestedPlayerSurfaceConsumed,
        onOpenAppearance = onOpenAppearance,
        onOpenConnections = onOpenConnections,
        onOpenSongManager = onOpenSongManager,
        onOpenFolderManager = onOpenFolderManager,
        mainViewModel = mainViewModel,
    )
}
