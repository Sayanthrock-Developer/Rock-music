package com.rockmusic.app.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.rockmusic.app.domain.model.LocalTrack
import com.rockmusic.app.player.PlayerUiState

private enum class ExperienceDestination(val label: String) {
    HOME("Home"),
    LIBRARY("Library"),
    SETTINGS("Settings"),
}

@Composable
fun RockMusicExperience(
    useBlurFrames: Boolean,
    onOpenAppearance: () -> Unit,
    onOpenConnections: () -> Unit,
    onOpenSongManager: () -> Unit,
    onOpenFolderManager: () -> Unit,
    onOpenEqualizer: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val library by viewModel.libraryState.collectAsStateWithLifecycle()
    val player by viewModel.playerState.collectAsStateWithLifecycle()
    val youtube by viewModel.youTubeState.collectAsStateWithLifecycle()
    var destination by remember { mutableStateOf(ExperienceDestination.HOME) }
    var showNowPlaying by remember { mutableStateOf(false) }

    val openAudio = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            viewModel.openDownloadedAudio(uri)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.13f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            bottomBar = {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ExperienceMiniPlayer(
                        player = player,
                        useBlurFrames = useBlurFrames,
                        onOpen = { showNowPlaying = true },
                        onToggle = viewModel::togglePlayPause,
                        onNext = viewModel::skipNext,
                    )
                    ExperienceBottomBar(
                        selected = destination,
                        useBlurFrames = useBlurFrames,
                        onSelect = { destination = it },
                    )
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                when (destination) {
                    ExperienceDestination.HOME -> ExperienceHome(
                        state = library,
                        youtubeState = youtube,
                        useBlurFrames = useBlurFrames,
                        onLoad = viewModel::loadLocalMusic,
                        onPlay = viewModel::play,
                        onPlayAll = viewModel::playAll,
                        onOpenAudio = { openAudio.launch(arrayOf("audio/*")) },
                        onOpenSongManager = onOpenSongManager,
                        onOpenFolderManager = onOpenFolderManager,
                        onOpenEqualizer = onOpenEqualizer,
                        onOpenConnections = onOpenConnections,
                        onOpenAppearance = onOpenAppearance,
                        onYouTubeSearch = viewModel::openOfficialYouTubeSearch,
                        onYouTubeLink = viewModel::openOfficialYouTubeLink,
                        onClearYouTubeStatus = viewModel::clearYouTubeStatus,
                    )

                    ExperienceDestination.LIBRARY -> ExperienceLibrary(
                        tracks = library.tracks,
                        isImporting = library.isImporting,
                        onPlay = viewModel::play,
                        onPlayAll = viewModel::playAll,
                        onOpenAudio = { openAudio.launch(arrayOf("audio/*")) },
                    )

                    ExperienceDestination.SETTINGS -> ExperienceSettings(
                        useBlurFrames = useBlurFrames,
                        onOpenAppearance = onOpenAppearance,
                        onOpenConnections = onOpenConnections,
                        onOpenSongManager = onOpenSongManager,
                        onOpenFolderManager = onOpenFolderManager,
                        onOpenEqualizer = onOpenEqualizer,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = showNowPlaying && player.hasMedia,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            ExperienceNowPlaying(
                player = player,
                useBlurFrames = useBlurFrames,
                onClose = { showNowPlaying = false },
                onToggle = viewModel::togglePlayPause,
                onSeek = viewModel::seekTo,
                onNext = viewModel::skipNext,
                onPrevious = viewModel::skipPrevious,
            )
        }
    }
}

@Composable
private fun ExperienceHome(
    state: LocalLibraryState,
    youtubeState: YouTubeHomeState,
    useBlurFrames: Boolean,
    onLoad: () -> Unit,
    onPlay: (LocalTrack) -> Unit,
    onPlayAll: (List<LocalTrack>) -> Unit,
    onOpenAudio: () -> Unit,
    onOpenSongManager: () -> Unit,
    onOpenFolderManager: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenConnections: () -> Unit,
    onOpenAppearance: () -> Unit,
    onYouTubeSearch: (String) -> Unit,
    onYouTubeLink: (String) -> Unit,
    onClearYouTubeStatus: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var source by remember { mutableStateOf(UnifiedHomeSource.ALL) }
    var query by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        if (granted) onLoad()
    }

    DisposableEffect(lifecycleOwner, context, permission) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    permission,
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission && state.tracks.isEmpty() && !state.isLoading) onLoad()
    }

    val visibleTracks = remember(state.tracks, query, source) {
        UnifiedHomeLibrary.localTracks(state.tracks, query, source)
    }
    val featuredTracks = remember(visibleTracks) {
        UnifiedHomeLibrary.featuredTracks(visibleTracks)
    }
    val speedDialRows = remember(visibleTracks) {
        UnifiedHomeLibrary.speedDialRows(visibleTracks)
    }
    val looksLikeLink = query.trim().startsWith("https://") || query.trim().startsWith("http://")

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Rock Music",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        "Songs and official YouTube Music in one place",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ExperienceHeaderAction(Icons.Rounded.Folder, "Open Folder Manager", onOpenFolderManager)
                ExperienceHeaderAction(Icons.Rounded.LibraryMusic, "Open Song Manager", onOpenSongManager)
                ExperienceHeaderAction(Icons.Rounded.Settings, "Open service connections", onOpenConnections)
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(UnifiedHomeSource.entries, key = UnifiedHomeSource::name) { option ->
                    FilterChip(
                        selected = source == option,
                        onClick = {
                            source = option
                            onClearYouTubeStatus()
                        },
                        label = { Text(option.label) },
                    )
                }
            }
        }

        item {
            OutlinedTextField(
                value = query,
                onValueChange = {
                    query = it
                    if (!youtubeState.isLaunching) onClearYouTubeStatus()
                },
                label = { Text("Search songs or YouTube Music") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    IconButton(
                        onClick = {
                            if (looksLikeLink) onYouTubeLink(query) else onYouTubeSearch(query)
                        },
                        enabled = query.isNotBlank() && !youtubeState.isLaunching,
                    ) {
                        if (youtubeState.isLaunching) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(20.dp)
                                    .semantics { contentDescription = "Opening YouTube Music" },
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                Icons.Rounded.PlayArrow,
                                contentDescription = "Open with YouTube Music",
                            )
                        }
                    }
                },
                placeholder = {
                    Text(
                        if (source == UnifiedHomeSource.YOUTUBE) {
                            "Search or paste an official YouTube link"
                        } else {
                            "Song, artist, album, or YouTube search"
                        },
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        state.statusMessage?.let { item { ExperienceStatus(it, isError = false) } }
        state.error?.let { item { ExperienceStatus(it, isError = true) } }
        youtubeState.message?.let { item { ExperienceStatus(it, isError = false) } }
        youtubeState.error?.let { item { ExperienceStatus(it, isError = true) } }

        if (source != UnifiedHomeSource.YOUTUBE) {
            when {
                state.isLoading -> item { ExperienceLoading("Scanning songs on this phone…") }
                state.tracks.isEmpty() -> item {
                    ExperienceEmptyLibrary(
                        hasPermission = hasPermission,
                        useBlurFrames = useBlurFrames,
                        onRequestPermission = { permissionLauncher.launch(permission) },
                        onOpenAudio = onOpenAudio,
                        onOpenSongManager = onOpenSongManager,
                    )
                }
                visibleTracks.isEmpty() -> item {
                    ExperienceStatus(
                        "No imported songs match “${query.trim()}”. You can search the same text on YouTube Music.",
                        isError = false,
                    )
                }
                else -> {
                    item {
                        ExperienceSectionHeading(
                            title = "Featured songs",
                            subtitle = "Swipe through imported and scanned music",
                        )
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            items(featuredTracks, key = LocalTrack::mediaUri) { track ->
                                ExperienceFeaturedCard(track, onPlay)
                            }
                        }
                    }
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ExperienceSectionHeading(
                                title = "Speed dial",
                                subtitle = "${visibleTracks.size} song${if (visibleTracks.size == 1) "" else "s"} on Home",
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = { onPlayAll(visibleTracks) }) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                                Text("Play all")
                            }
                        }
                    }
                    speedDialRows.forEachIndexed { index, rowTracks ->
                        item(key = "speed-$index") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                rowTracks.forEach { track ->
                                    ExperienceSpeedCard(
                                        track = track,
                                        onPlay = onPlay,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                repeat(3 - rowTracks.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }

        if (source != UnifiedHomeSource.SONGS) {
            item {
                ExperienceYouTubePanel(
                    query = query,
                    looksLikeLink = looksLikeLink,
                    isLaunching = youtubeState.isLaunching,
                    useBlurFrames = useBlurFrames,
                    onSearch = onYouTubeSearch,
                    onOpenLink = onYouTubeLink,
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onOpenAppearance, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Palette, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Appearance")
                }
                OutlinedButton(onClick = onOpenAudio, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Import song")
                }
            }
        }
    }
}

@Composable
private fun ExperienceYouTubePanel(
    query: String,
    looksLikeLink: Boolean,
    isLaunching: Boolean,
    useBlurFrames: Boolean,
    onSearch: (String) -> Unit,
    onOpenLink: (String) -> Unit,
) {
    ExperienceGlass(useBlurFrames, shape = RoundedCornerShape(30.dp)) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.padding(12.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("YouTube Music", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Open validated searches and playback in an official app or safe browser fallback.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Button(
                onClick = { if (looksLikeLink) onOpenLink(query) else onSearch(query) },
                enabled = query.isNotBlank() && !isLaunching,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (isLaunching) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(18.dp)
                            .semantics { contentDescription = "Opening YouTube Music" },
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                }
                Spacer(Modifier.width(8.dp))
                Text(if (looksLikeLink) "Open official YouTube link" else "Search YouTube Music")
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("New music", "Workout", "Live music")) { suggestion ->
                    FilterChip(
                        selected = false,
                        onClick = { onSearch(suggestion) },
                        enabled = !isLaunching,
                        label = { Text(suggestion) },
                    )
                }
            }
            Text(
                "Rock Music does not extract protected streams, remove advertisements, or bypass provider controls.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExperienceLibrary(
    tracks: List<LocalTrack>,
    isImporting: Boolean,
    onPlay: (LocalTrack) -> Unit,
    onPlayAll: (List<LocalTrack>) -> Unit,
    onOpenAudio: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val results = remember(tracks, query) {
        UnifiedHomeLibrary.localTracks(tracks, query, UnifiedHomeSource.SONGS)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Library", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                    Text(
                        "${tracks.size} imported and scanned song${if (tracks.size == 1) "" else "s"}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(onClick = onOpenAudio, enabled = !isImporting) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text("Import")
                }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text("Search your library") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotBlank()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                },
                placeholder = { Text("Songs, artists, or albums") },
                singleLine = true,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (results.isEmpty()) {
            item {
                ExperienceStatus(
                    if (tracks.isEmpty()) {
                        "Your library is empty. Import a song or scan the device from Song Manager."
                    } else {
                        "No songs match this search."
                    },
                    isError = false,
                )
            }
        } else {
            item {
                TextButton(onClick = { onPlayAll(results) }) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Text("Play all results")
                }
            }
            items(results, key = LocalTrack::mediaUri) { track ->
                ExperienceTrackRow(track, onPlay)
            }
        }
    }
}

@Composable
private fun ExperienceSettings(
    useBlurFrames: Boolean,
    onOpenAppearance: () -> Unit,
    onOpenConnections: () -> Unit,
    onOpenSongManager: () -> Unit,
    onOpenFolderManager: () -> Unit,
    onOpenEqualizer: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(Modifier.statusBarsPadding()) {
                Text("Settings", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
                Text("Library, providers, and visual preferences", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            ExperienceSettingsCard(
                "Song Manager",
                "Import individual files, multiple songs, or a complete document folder.",
                Icons.Rounded.LibraryMusic,
                onOpenSongManager,
            )
        }
        item {
            ExperienceSettingsCard(
                "Folder Manager",
                "Include or exclude MediaStore folders and apply a clean rescan.",
                Icons.Rounded.Folder,
                onOpenFolderManager,
            )
        }
        item {
            ExperienceSettingsCard(
                "Service connections",
                "Review YouTube, Spotify, lyrics, cloud, Discord, and provider readiness.",
                Icons.Rounded.Settings,
                onOpenConnections,
            )
        }
        item {
            ExperienceSettingsCard(
                "Equalizer",
                "Adjust audio frequencies and effects for playback.",
                Icons.Rounded.Equalizer,
                onOpenEqualizer,
            )
        }
        item {
            ExperienceSettingsCard(
                "Appearance",
                "Theme mode, system colour, and ${if (useBlurFrames) "enabled" else "disabled"} glass frames.",
                Icons.Rounded.Palette,
                onOpenAppearance,
            )
        }
    }
}

@Composable
private fun ExperienceFeaturedCard(track: LocalTrack, onPlay: (LocalTrack) -> Unit) {
    Surface(
        modifier = Modifier
            .width(282.dp)
            .clickable { onPlay(track) },
        shape = RoundedCornerShape(34.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        shadowElevation = 2.dp,
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(354.dp)) {
            ExperienceArtwork(track.artworkUri, track.title, Modifier.fillMaxSize(), 34.dp)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Transparent, Color.Black.copy(alpha = 0.88f)),
                        ),
                    ),
            )
            FilledIconButton(
                onClick = { onPlay(track) },
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp),
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "Play ${track.title}")
            }
            Column(modifier = Modifier.align(Alignment.BottomStart).padding(20.dp)) {
                Text(
                    track.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    track.artist,
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ExperienceSpeedCard(
    track: LocalTrack,
    onPlay: (LocalTrack) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.clickable { onPlay(track) },
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column {
            ExperienceArtwork(track.artworkUri, track.title, Modifier.fillMaxWidth().aspectRatio(1f), 22.dp)
            Column(Modifier.padding(10.dp)) {
                Text(track.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    track.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun ExperienceTrackRow(track: LocalTrack, onPlay: (LocalTrack) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onPlay(track) },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.82f),
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            ExperienceArtwork(track.artworkUri, track.title, Modifier.size(58.dp), 16.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(track.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    track.album,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play ${track.title}")
        }
    }
}

@Composable
private fun ExperienceMiniPlayer(
    player: PlayerUiState,
    useBlurFrames: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onNext: () -> Unit,
) {
    if (!player.hasMedia && player.errorMessage == null) return
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        player.errorMessage?.let { ExperienceStatus(it, isError = true) }
        if (player.hasMedia) {
            ExperienceGlass(
                useBlurFrames = useBlurFrames,
                modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
                shape = RoundedCornerShape(25.dp),
            ) {
                Row(modifier = Modifier.padding(9.dp), verticalAlignment = Alignment.CenterVertically) {
                    ExperienceArtwork(player.artworkUri, player.title.orEmpty(), Modifier.size(56.dp), 16.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            player.title.orEmpty(),
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            player.artist.orEmpty(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    IconButton(onClick = onToggle, enabled = !player.isPreparing) {
                        if (player.isPreparing) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(22.dp)
                                    .semantics { contentDescription = "Preparing audio" },
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = "Play or pause",
                            )
                        }
                    }
                    IconButton(onClick = onNext, enabled = player.hasNext) {
                        Icon(Icons.Rounded.SkipNext, contentDescription = "Next song")
                    }
                }
            }
        }
    }
}

@Composable
private fun ExperienceBottomBar(
    selected: ExperienceDestination,
    useBlurFrames: Boolean,
    onSelect: (ExperienceDestination) -> Unit,
) {
    ExperienceGlass(useBlurFrames, shape = RoundedCornerShape(30.dp)) {
        NavigationBar(containerColor = Color.Transparent) {
            ExperienceDestination.entries.forEach { destination ->
                NavigationBarItem(
                    selected = selected == destination,
                    onClick = { onSelect(destination) },
                    icon = {
                        Icon(
                            when (destination) {
                                ExperienceDestination.HOME -> Icons.Rounded.Home
                                ExperienceDestination.LIBRARY -> Icons.Rounded.LibraryMusic
                                ExperienceDestination.SETTINGS -> Icons.Rounded.Settings
                            },
                            contentDescription = destination.label,
                        )
                    },
                    label = { Text(destination.label) },
                )
            }
        }
    }
}

@Composable
private fun ExperienceNowPlaying(
    player: PlayerUiState,
    useBlurFrames: Boolean,
    onClose: () -> Unit,
    onToggle: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    BackHandler(onBack = onClose)
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.72f),
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.38f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .statusBarsPadding(),
    ) {
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopStart).padding(16.dp)) {
            Icon(Icons.Rounded.Close, contentDescription = "Close Now Playing")
        }
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("Now Playing", style = MaterialTheme.typography.titleLarge)
            Text(
                player.title.orEmpty(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(24.dp))
            ExperienceGlass(useBlurFrames, shape = RoundedCornerShape(38.dp)) {
                ExperienceArtwork(
                    player.artworkUri,
                    player.title.orEmpty(),
                    Modifier.fillMaxWidth().aspectRatio(1f),
                    38.dp,
                )
            }
            Spacer(Modifier.height(28.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
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
                    )
                }
                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
                    Text(
                        "LOCAL • MEDIA3",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Slider(
                value = player.positionMs.coerceAtMost(player.durationMs).toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..player.durationMs.coerceAtLeast(1L).toFloat(),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(experienceDuration(player.positionMs))
                Spacer(Modifier.weight(1f))
                Text(experienceDuration(player.durationMs))
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(30.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious, enabled = player.hasPrevious) {
                    Icon(Icons.Rounded.SkipPrevious, "Previous song", Modifier.size(40.dp))
                }
                FilledIconButton(
                    onClick = onToggle,
                    enabled = !player.isPreparing,
                    modifier = Modifier.size(76.dp),
                ) {
                    if (player.isPreparing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(32.dp)
                                .semantics { contentDescription = "Preparing audio" },
                            strokeWidth = 3.dp,
                        )
                    } else {
                        Icon(
                            if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            "Play or pause",
                            Modifier.size(40.dp),
                        )
                    }
                }
                IconButton(onClick = onNext, enabled = player.hasNext) {
                    Icon(Icons.Rounded.SkipNext, "Next song", Modifier.size(40.dp))
                }
            }
        }
    }
}

@Composable
private fun ExperienceEmptyLibrary(
    hasPermission: Boolean,
    useBlurFrames: Boolean,
    onRequestPermission: () -> Unit,
    onOpenAudio: () -> Unit,
    onOpenSongManager: () -> Unit,
) {
    ExperienceGlass(useBlurFrames, shape = RoundedCornerShape(30.dp)) {
        Column(modifier = Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Rounded.Storage, contentDescription = null, modifier = Modifier.size(36.dp))
            Text("Add songs to Home", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Every scanned or imported song appears in Featured songs and Speed Dial.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (!hasPermission) {
                Button(onClick = onRequestPermission, modifier = Modifier.fillMaxWidth()) {
                    Text("Allow music access")
                }
            }
            OutlinedButton(onClick = onOpenAudio, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Import one song")
            }
            OutlinedButton(onClick = onOpenSongManager, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.LibraryMusic, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Open Song Manager")
            }
        }
    }
}

@Composable
private fun ExperienceLoading(message: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .size(24.dp)
                .semantics { contentDescription = message },
            strokeWidth = 2.dp,
        )
        Spacer(Modifier.width(12.dp))
        Text(message)
    }
}

@Composable
private fun ExperienceStatus(message: String, isError: Boolean) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = if (isError) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
    ) {
        Text(
            message,
            modifier = Modifier.padding(14.dp),
            color = if (isError) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}

@Composable
private fun ExperienceSectionHeading(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ExperienceHeaderAction(icon: ImageVector, description: String, onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(icon, contentDescription = description) }
}

@Composable
private fun ExperienceSettingsCard(
    title: String,
    body: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                Icon(icon, contentDescription = null, modifier = Modifier.padding(12.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ExperienceArtwork(
    artworkUri: String?,
    title: String,
    modifier: Modifier,
    cornerRadius: Dp,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius))
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
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
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

@Composable
private fun ExperienceGlass(
    useBlurFrames: Boolean,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier.then(
            if (useBlurFrames) {
                Modifier.drawWithCache {
                    val sheenGradient = Brush.linearGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.15f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.05f)
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    )
                    onDrawWithContent {
                        drawContent()
                        drawRect(brush = sheenGradient)
                    }
                }
            } else {
                Modifier
            }
        ),
        shape = shape,
        color = if (useBlurFrames) MaterialTheme.colorScheme.surface.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface,
        border = if (useBlurFrames) {
            BorderStroke(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset.Infinite
                )
            )
        } else {
            null
        },
        shadowElevation = if (useBlurFrames) 14.dp else 2.dp,
        content = content,
    )
}

private fun experienceDuration(durationMs: Long): String {
    val totalSeconds = durationMs.coerceAtLeast(0L) / 1_000L
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
