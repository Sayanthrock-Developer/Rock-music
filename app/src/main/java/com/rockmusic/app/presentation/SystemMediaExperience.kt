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
import androidx.compose.foundation.layout.weight
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
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

private enum class SystemPlayerPanel(val label: String) {
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
    mainViewModel: MainViewModel = hiltViewModel(),
    systemMediaViewModel: SystemMediaViewModel = hiltViewModel(),
) {
    val player by mainViewModel.playerState.collectAsStateWithLifecycle()
    val lyricsState by systemMediaViewModel.lyricsState.collectAsStateWithLifecycle()
    val routeState by systemMediaViewModel.routeState.collectAsStateWithLifecycle()
    var showSystemPlayer by rememberSaveable { mutableStateOf(false) }
    var selectedPanel by rememberSaveable { mutableStateOf(SystemPlayerPanel.QUEUE) }

    LaunchedEffect(requestedPlayerSurface, player.hasMedia) {
        if (
            requestedPlayerSurface == MediaSessionCommands.SURFACE_LYRICS &&
            player.hasMedia
        ) {
            showSystemPlayer = true
            selectedPanel = SystemPlayerPanel.LYRICS
            systemMediaViewModel.loadLyrics(player.mediaUri)
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
            viewModel = mainViewModel,
        )

        if (player.hasMedia && !showSystemPlayer) {
            CompactMediaPill(
                player = player,
                onOpen = { showSystemPlayer = true },
                onToggle = mainViewModel::togglePlayPause,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 8.dp),
            )
        }

        if (showSystemPlayer && player.hasMedia) {
            SystemPlayerCard(
                player = player,
                selectedPanel = selectedPanel,
                lyricsState = lyricsState,
                routeState = routeState,
                onClose = { showSystemPlayer = false },
                onPanelSelected = { panel ->
                    selectedPanel = panel
                    when (panel) {
                        SystemPlayerPanel.LYRICS -> systemMediaViewModel.loadLyrics(player.mediaUri)
                        SystemPlayerPanel.OUTPUT -> systemMediaViewModel.refreshAudioRoutes()
                        SystemPlayerPanel.QUEUE -> Unit
                    }
                },
                onToggleFavourite = systemMediaViewModel::toggleFavourite,
                onPrevious = mainViewModel::skipPrevious,
                onToggle = mainViewModel::togglePlayPause,
                onNext = mainViewModel::skipNext,
                onSeek = mainViewModel::seekTo,
                onVolume = systemMediaViewModel::setVolume,
                onQueueItem = systemMediaViewModel::playQueueIndex,
                onRoute = systemMediaViewModel::selectAudioRoute,
            )
        }
    }
}

@Composable
private fun CompactMediaPill(
    player: PlayerUiState,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .width(210.dp)
            .height(58.dp)
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.96f),
        shadowElevation = 10.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SystemArtwork(
                artworkUri = player.artworkUri,
                title = player.title.orEmpty(),
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(16.dp),
            )
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
                        if (player.isPlaying) Icons.Rounded.GraphicEq else Icons.Rounded.PlayArrow,
                        contentDescription = "Play or pause",
                        tint = MaterialTheme.colorScheme.inverseOnSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun SystemPlayerCard(
    player: PlayerUiState,
    selectedPanel: SystemPlayerPanel,
    lyricsState: LyricsUiState,
    routeState: AudioRouteUiState,
    onClose: () -> Unit,
    onPanelSelected: (SystemPlayerPanel) -> Unit,
    onToggleFavourite: () -> Unit,
    onPrevious: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolume: (Float) -> Unit,
    onQueueItem: (Int) -> Unit,
    onRoute: (Int) -> Unit,
) {
    BackHandler(onBack = onClose)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.34f),
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            )
            .statusBarsPadding(),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 18.dp,
                bottom = 32.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Media player", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "Rock Music system controls",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onPanelSelected(SystemPlayerPanel.OUTPUT) }) {
                        Icon(Icons.Rounded.Speaker, contentDescription = "Choose audio output")
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, contentDescription = "Close media player")
                    }
                }
            }

            item {
                Surface(
                    shape = RoundedCornerShape(36.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shadowElevation = 8.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        SystemArtwork(
                            artworkUri = player.artworkUri,
                            title = player.title.orEmpty(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(320.dp),
                            shape = RoundedCornerShape(30.dp),
                        )
                        Spacer(Modifier.height(18.dp))
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
                        Spacer(Modifier.height(18.dp))
                        Slider(
                            value = player.positionMs.coerceAtMost(player.durationMs).toFloat(),
                            onValueChange = { onSeek(it.toLong()) },
                            valueRange = 0f..player.durationMs.coerceAtLeast(1L).toFloat(),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(Modifier.fillMaxWidth()) {
                            Text(systemDuration(player.positionMs))
                            Spacer(Modifier.weight(1f))
                            Text(systemDuration(player.durationMs))
                        }
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = onToggleFavourite) {
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
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(30.dp),
                                        strokeWidth = 3.dp,
                                    )
                                } else {
                                    Icon(
                                        if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                        contentDescription = "Play or pause",
                                        modifier = Modifier.size(36.dp),
                                    )
                                }
                            }
                            IconButton(onClick = onNext, enabled = player.hasNext) {
                                Icon(Icons.Rounded.SkipNext, contentDescription = "Next song")
                            }
                            IconButton(onClick = { onPanelSelected(SystemPlayerPanel.LYRICS) }) {
                                Icon(Icons.Rounded.Subtitles, contentDescription = "Open lyrics")
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Rounded.VolumeDown, contentDescription = null)
                            Slider(
                                value = player.volume,
                                onValueChange = onVolume,
                                valueRange = 0f..1f,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 10.dp),
                            )
                            Icon(Icons.Rounded.VolumeUp, contentDescription = null)
                        }
                    }
                }
            }

            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(SystemPlayerPanel.entries, key = SystemPlayerPanel::name) { panel ->
                        FilterChip(
                            selected = selectedPanel == panel,
                            onClick = { onPanelSelected(panel) },
                            label = { Text(panel.label) },
                            leadingIcon = {
                                Icon(
                                    when (panel) {
                                        SystemPlayerPanel.QUEUE -> Icons.Rounded.QueueMusic
                                        SystemPlayerPanel.LYRICS -> Icons.Rounded.Subtitles
                                        SystemPlayerPanel.OUTPUT -> Icons.Rounded.Speaker
                                    },
                                    contentDescription = null,
                                )
                            },
                        )
                    }
                }
            }

            when (selectedPanel) {
                SystemPlayerPanel.QUEUE -> item {
                    QueuePanel(
                        queue = player.queue,
                        currentIndex = player.currentQueueIndex,
                        onQueueItem = onQueueItem,
                    )
                }

                SystemPlayerPanel.LYRICS -> item { LyricsPanel(lyricsState) }
                SystemPlayerPanel.OUTPUT -> item {
                    OutputPanel(routeState = routeState, onRoute = onRoute)
                }
            }
        }
    }
}

@Composable
private fun QueuePanel(
    queue: List<PlayerQueueItem>,
    currentIndex: Int,
    onQueueItem: (Int) -> Unit,
) {
    Surface(shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Up next", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
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
                            SystemArtwork(
                                artworkUri = item.artworkUri,
                                title = item.title,
                                modifier = Modifier.size(48.dp),
                                shape = RoundedCornerShape(14.dp),
                            )
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
}

@Composable
private fun LyricsPanel(state: LyricsUiState) {
    Surface(shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Embedded lyrics", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            when {
                state.isLoading -> Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Reading lyrics from this audio file…")
                }

                state.error != null -> Text(state.error, color = MaterialTheme.colorScheme.error)
                state.lyrics != null -> Text(state.lyrics)
                else -> Text(state.message ?: "Open Lyrics to read embedded local lyrics.")
            }
        }
    }
}

@Composable
private fun OutputPanel(
    routeState: AudioRouteUiState,
    onRoute: (Int) -> Unit,
) {
    Surface(shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Audio output", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            routeState.message?.let { Text(it, color = MaterialTheme.colorScheme.primary) }
            routeState.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (routeState.routes.isEmpty() && routeState.error == null) {
                Text("No selectable Android audio routes are currently available.")
            }
            routeState.routes.forEach { route ->
                AudioRouteRow(route = route, onClick = { onRoute(route.index) })
            }
        }
    }
}

@Composable
private fun AudioRouteRow(route: SystemAudioRoute, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = route.isEnabled, onClick = onClick),
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
private fun SystemArtwork(
    artworkUri: String?,
    title: String,
    modifier: Modifier,
    shape: RoundedCornerShape,
) {
    Box(
        modifier = modifier
            .clip(shape)
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
        if (artworkUri.isNullOrBlank()) {
            Icon(
                Icons.Rounded.LibraryMusic,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
            )
        } else {
            AsyncImage(
                model = artworkUri,
                contentDescription = "Artwork for $title",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun systemDuration(valueMs: Long): String {
    val totalSeconds = valueMs.coerceAtLeast(0L) / 1_000L
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
