package com.rockmusic.app.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.VolumeDown
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.rockmusic.app.player.MediaSessionCommands
import com.rockmusic.app.player.PlayerQueueItem
import com.rockmusic.app.player.PlayerUiState
import com.rockmusic.app.player.SystemAudioRoute
import java.util.Locale

private enum class PlayerPanel(val label: String) {
    QUEUE("Queue"),
    LYRICS("Lyrics"),
    OUTPUT("Output"),
}

@Composable
fun SystemMediaExperience(
    useBlurFrames: Boolean,
    requestedPlayerSurface: String?,
    onRequestedPlayerSurfaceConsumed: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenConnections: () -> Unit,
    onOpenSongManager: () -> Unit,
    onOpenFolderManager: () -> Unit,
    onOpenEqualizer: () -> Unit,
    mainViewModel: MainViewModel = hiltViewModel(),
    controlsViewModel: SystemMediaViewModel = hiltViewModel(),
) {
    val playerState = mainViewModel.playerState.collectAsStateWithLifecycle()
    val lyricsState = controlsViewModel.lyricsState.collectAsStateWithLifecycle()
    val routesState = controlsViewModel.routeState.collectAsStateWithLifecycle()
    val expandedState = rememberSaveable { mutableStateOf(false) }
    val panelState = rememberSaveable { mutableStateOf(PlayerPanel.QUEUE) }

    LaunchedEffect(requestedPlayerSurface, playerState.value.hasMedia) {
        if (requestedPlayerSurface == MediaSessionCommands.SURFACE_LYRICS && playerState.value.hasMedia) {
            expandedState.value = true
            panelState.value = PlayerPanel.LYRICS
            controlsViewModel.loadLyrics(playerState.value.mediaUri)
            onRequestedPlayerSurfaceConsumed()
        }
    }

    Box(Modifier.fillMaxSize()) {
        RockMusicExperience(
            useBlurFrames = useBlurFrames,
            onOpenAppearance = onOpenAppearance,
            onOpenConnections = onOpenConnections,
            onOpenSongManager = onOpenSongManager,
            onOpenFolderManager = onOpenFolderManager,
            onOpenEqualizer = onOpenEqualizer,
            viewModel = mainViewModel,
        )

        if (playerState.value.hasMedia && !expandedState.value) {
            MediaPill(
                player = playerState.value,
                onOpen = { expandedState.value = true },
                onToggle = mainViewModel::togglePlayPause,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 8.dp),
            )
        }

        if (expandedState.value && playerState.value.hasMedia) {
            ExpandedSystemPlayer(
                player = playerState.value,
                selectedPanel = panelState.value,
                lyrics = lyricsState.value,
                routes = routesState.value,
                onClose = { expandedState.value = false },
                onPanel = { selected ->
                    panelState.value = selected
                    when (selected) {
                        PlayerPanel.QUEUE -> Unit
                        PlayerPanel.LYRICS -> controlsViewModel.loadLyrics(playerState.value.mediaUri)
                        PlayerPanel.OUTPUT -> controlsViewModel.refreshAudioRoutes()
                    }
                },
                onFavourite = controlsViewModel::toggleFavourite,
                onPrevious = mainViewModel::skipPrevious,
                onToggle = mainViewModel::togglePlayPause,
                onNext = mainViewModel::skipNext,
                onSeek = mainViewModel::seekTo,
                onVolume = controlsViewModel::setVolume,
                onQueueItem = controlsViewModel::playQueueIndex,
                onRoute = controlsViewModel::selectAudioRoute,
            )
        }
    }
}

@Composable
private fun MediaPill(
    player: PlayerUiState,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .width(220.dp)
            .height(60.dp)
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.96f),
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Artwork(player.artworkUri, player.title.orEmpty(), Modifier.size(44.dp), 15)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    player.title.orEmpty(),
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    player.artist.orEmpty(),
                    color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.68f),
                    style = MaterialTheme.typography.labelMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onToggle, enabled = !player.isPreparing) {
                if (player.isPreparing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.inverseOnSurface,
                    )
                } else {
                    Icon(
                        if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (player.isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.inverseOnSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandedSystemPlayer(
    player: PlayerUiState,
    selectedPanel: PlayerPanel,
    lyrics: LyricsUiState,
    routes: AudioRouteUiState,
    onClose: () -> Unit,
    onPanel: (PlayerPanel) -> Unit,
    onFavourite: () -> Unit,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolume: (Float) -> Unit,
    onQueueItem: (Int) -> Unit,
    onRoute: (Int) -> Unit,
) {
    val draggedPositionState = rememberSaveable(player.mediaUri) { mutableStateOf<Float?>(null) }
    val draggedVolumeState = rememberSaveable { mutableStateOf<Float?>(null) }

    BackHandler(onBack = onClose)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            )
            .statusBarsPadding(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            PlayerHeader(onOutput = { onPanel(PlayerPanel.OUTPUT) }, onClose = onClose)
        }
        item {
            PlayerCard(
                player = player,
                draggedPosition = draggedPositionState.value,
                onDraggedPosition = { draggedPositionState.value = it },
                onPositionFinished = {
                    draggedPositionState.value?.let { onSeek(it.toLong()) }
                    draggedPositionState.value = null
                },
                draggedVolume = draggedVolumeState.value,
                onDraggedVolume = { draggedVolumeState.value = it },
                onVolumeFinished = {
                    draggedVolumeState.value?.let(onVolume)
                    draggedVolumeState.value = null
                },
                onFavourite = onFavourite,
                onPrevious = onPrevious,
                onToggle = onToggle,
                onNext = onNext,
                onLyrics = { onPanel(PlayerPanel.LYRICS) },
            )
        }
        item {
            PanelSelector(selected = selectedPanel, onPanel = onPanel)
        }
        item {
            when (selectedPanel) {
                PlayerPanel.QUEUE -> QueuePanel(player.queue, player.currentQueueIndex, onQueueItem)
                PlayerPanel.LYRICS -> LyricsPanel(lyrics)
                PlayerPanel.OUTPUT -> OutputPanel(routes, onRoute)
            }
        }
    }
}

@Composable
private fun PlayerHeader(onOutput: () -> Unit, onClose: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("Media player", style = MaterialTheme.typography.headlineSmall)
            Text("Rock Music system controls", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        IconButton(onClick = onOutput) {
            Icon(Icons.Rounded.Speaker, contentDescription = "Choose audio output")
        }
        IconButton(onClick = onClose) {
            Icon(Icons.Rounded.Close, contentDescription = "Close media player")
        }
    }
}

@Composable
private fun PlayerCard(
    player: PlayerUiState,
    draggedPosition: Float?,
    onDraggedPosition: (Float) -> Unit,
    onPositionFinished: () -> Unit,
    draggedVolume: Float?,
    onDraggedVolume: (Float) -> Unit,
    onVolumeFinished: () -> Unit,
    onFavourite: () -> Unit,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onLyrics: () -> Unit,
) {
    val positionValue = draggedPosition
        ?: player.positionMs.coerceAtMost(player.durationMs).toFloat()
    val volumeValue = draggedVolume ?: player.volume

    Surface(
        shape = RoundedCornerShape(34.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Artwork(
                player.artworkUri,
                player.title.orEmpty(),
                Modifier
                    .fillMaxWidth()
                    .height(320.dp),
                28,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                player.title.orEmpty(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                player.artist.orEmpty(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(14.dp))
            Slider(
                value = positionValue,
                onValueChange = onDraggedPosition,
                onValueChangeFinished = onPositionFinished,
                valueRange = 0f..player.durationMs.coerceAtLeast(1L).toFloat(),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth()) {
                Text(duration(positionValue.toLong()))
                Spacer(Modifier.weight(1f))
                Text(duration(player.durationMs))
            }
            Spacer(Modifier.height(10.dp))
            PlaybackButtons(
                player = player,
                onFavourite = onFavourite,
                onPrevious = onPrevious,
                onToggle = onToggle,
                onNext = onNext,
                onLyrics = onLyrics,
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.VolumeDown, contentDescription = null)
                Slider(
                    value = volumeValue,
                    onValueChange = onDraggedVolume,
                    onValueChangeFinished = onVolumeFinished,
                    valueRange = 0f..1f,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                        .semantics { contentDescription = "Playback volume" },
                )
                Icon(Icons.Rounded.VolumeUp, contentDescription = null)
            }
        }
    }
}

@Composable
private fun PlaybackButtons(
    player: PlayerUiState,
    onFavourite: () -> Unit,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onLyrics: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onFavourite) {
            Icon(
                if (player.isFavourite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = if (player.isFavourite) {
                    "Remove from favourites"
                } else {
                    "Add to favourites"
                },
            )
        }
        IconButton(onClick = onPrevious, enabled = player.hasPrevious) {
            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous song")
        }
        FilledIconButton(
            onClick = onToggle,
            enabled = !player.isPreparing,
            modifier = Modifier.size(70.dp),
        ) {
            if (player.isPreparing) {
                CircularProgressIndicator(Modifier.size(30.dp), strokeWidth = 3.dp)
            } else {
                Icon(
                    if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (player.isPlaying) "Pause" else "Play",
                    modifier = Modifier.size(36.dp),
                )
            }
        }
        IconButton(onClick = onNext, enabled = player.hasNext) {
            Icon(Icons.Rounded.SkipNext, contentDescription = "Next song")
        }
        IconButton(onClick = onLyrics) {
            Icon(Icons.Rounded.Subtitles, contentDescription = "Open lyrics")
        }
    }
}

@Composable
private fun PanelSelector(selected: PlayerPanel, onPanel: (PlayerPanel) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(PlayerPanel.entries, key = PlayerPanel::name) { option ->
            FilterChip(
                selected = selected == option,
                onClick = { onPanel(option) },
                label = { Text(option.label) },
                leadingIcon = {
                    Icon(
                        when (option) {
                            PlayerPanel.QUEUE -> Icons.Rounded.QueueMusic
                            PlayerPanel.LYRICS -> Icons.Rounded.Subtitles
                            PlayerPanel.OUTPUT -> Icons.Rounded.Speaker
                        },
                        contentDescription = null,
                    )
                },
            )
        }
    }
}

@Composable
private fun QueuePanel(
    queue: List<PlayerQueueItem>,
    currentIndex: Int,
    onQueueItem: (Int) -> Unit,
) {
    PanelSurface("Up next") {
        if (queue.isEmpty()) {
            Text("The current queue is empty.")
        } else {
            queue.forEachIndexed { index, item ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onQueueItem(index) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (index == currentIndex) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Artwork(item.artworkUri, item.title, Modifier.size(48.dp), 14)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                item.title,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                item.artist,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        if (index == currentIndex) {
                            Icon(Icons.Rounded.GraphicEq, contentDescription = "Currently playing")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LyricsPanel(state: LyricsUiState) {
    PanelSurface("Local lyrics") {
        when {
            state.isLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Looking for a matching .lrc or .txt file…")
            }
            state.error != null -> Text(state.error, color = MaterialTheme.colorScheme.error)
            state.lyrics != null -> Text(state.lyrics)
            else -> Text(
                state.message
                    ?: "Place a same-named .lrc or .txt file beside the local song to show lyrics.",
            )
        }
    }
}

@Composable
private fun OutputPanel(state: AudioRouteUiState, onRoute: (Int) -> Unit) {
    PanelSurface("Audio output") {
        state.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (state.routes.isEmpty() && state.error == null) {
            Text("No selectable Android audio routes are currently available.")
        }
        state.routes.forEach { route -> RouteRow(route, onRoute) }
    }
}

@Composable
private fun RouteRow(route: SystemAudioRoute, onRoute: (Int) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = route.isEnabled) { onRoute(route.index) },
        shape = RoundedCornerShape(18.dp),
        color = if (route.isSelected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Speaker, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(route.name, fontWeight = FontWeight.Bold)
                route.description?.takeIf(String::isNotBlank)?.let {
                    Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (route.isSelected) Text("Active", color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun PanelSurface(title: String, content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            content()
        }
    }
}

@Composable
private fun Artwork(uri: String?, title: String, modifier: Modifier, radius: Int) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(radius.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.tertiaryContainer,
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (uri.isNullOrBlank()) {
            Icon(Icons.Rounded.LibraryMusic, contentDescription = null, modifier = Modifier.size(48.dp))
        } else {
            AsyncImage(
                model = uri,
                contentDescription = "Artwork for $title",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun duration(valueMs: Long): String {
    val seconds = valueMs.coerceAtLeast(0L) / 1_000L
    return if (seconds >= 3_600L) {
        String.format(
            Locale.ROOT,
            "%d:%02d:%02d",
            seconds / 3_600L,
            (seconds % 3_600L) / 60L,
            seconds % 60L,
        )
    } else {
        String.format(Locale.ROOT, "%d:%02d", seconds / 60L, seconds % 60L)
    }
}
