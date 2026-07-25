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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BlurOn
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.rockmusic.app.domain.model.LocalTrack
import com.rockmusic.app.player.PlayerUiState
import kotlinx.coroutines.delay

private enum class Destination(val label: String, val icon: ImageVector) {
    Home("Home", Icons.Rounded.Home),
    Search("Search", Icons.Rounded.Search),
    Library("Library", Icons.Rounded.LibraryMusic),
    EchoFind("Echo Find", Icons.Rounded.GraphicEq),
    Profile("Profile", Icons.Rounded.Person),
}

@Composable
fun RockMusicRoot(
    useBlurFrames: Boolean,
    onOpenAppearance: () -> Unit,
    onOpenConnections: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val library by viewModel.libraryState.collectAsStateWithLifecycle()
    val player by viewModel.playerState.collectAsStateWithLifecycle()
    var destination by remember { mutableStateOf(Destination.Home) }
    var showNowPlaying by remember { mutableStateOf(false) }

    val openDownloadedAudio = rememberLauncherForActivityResult(
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
    val chooseDownloadedAudio = {
        openDownloadedAudio.launch(arrayOf("audio/*"))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val useRail = maxWidth >= 840.dp
            if (useRail) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding(),
                ) {
                    GlassNavigationRail(
                        selected = destination,
                        useBlurFrames = useBlurFrames,
                        onSelect = { destination = it },
                    )
                    Box(Modifier.weight(1f)) {
                        DestinationContent(
                            destination = destination,
                            library = library,
                            viewModel = viewModel,
                            chooseDownloadedAudio = chooseDownloadedAudio,
                            onOpenAppearance = onOpenAppearance,
                            onOpenConnections = onOpenConnections,
                            useBlurFrames = useBlurFrames,
                        )
                        PlayerOverlay(
                            player = player,
                            useBlurFrames = useBlurFrames,
                            onOpen = { showNowPlaying = true },
                            onToggle = viewModel::togglePlayPause,
                            onDismissError = viewModel::clearPlayerError,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(18.dp),
                        )
                    }
                }
            } else {
                Scaffold(
                    containerColor = Color.Transparent,
                    bottomBar = {
                        Column(
                            modifier = Modifier
                                .navigationBarsPadding()
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            PlayerOverlay(
                                player = player,
                                useBlurFrames = useBlurFrames,
                                onOpen = { showNowPlaying = true },
                                onToggle = viewModel::togglePlayPause,
                                onDismissError = viewModel::clearPlayerError,
                            )
                            GlassBottomBar(
                                selected = destination,
                                useBlurFrames = useBlurFrames,
                                onSelect = { destination = it },
                            )
                        }
                    },
                ) { padding ->
                    Box(Modifier.padding(padding)) {
                        DestinationContent(
                            destination = destination,
                            library = library,
                            viewModel = viewModel,
                            chooseDownloadedAudio = chooseDownloadedAudio,
                            onOpenAppearance = onOpenAppearance,
                            onOpenConnections = onOpenConnections,
                            useBlurFrames = useBlurFrames,
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = showNowPlaying && player.hasMedia,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                NowPlayingScreen(
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
}

@Composable
private fun DestinationContent(
    destination: Destination,
    library: LocalLibraryState,
    viewModel: MainViewModel,
    chooseDownloadedAudio: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenConnections: () -> Unit,
    useBlurFrames: Boolean,
) {
    when (destination) {
        Destination.Home -> HomeScreen(
            state = library,
            onLoad = viewModel::loadLocalMusic,
            onPlay = viewModel::play,
            onOpenDownloadedAudio = chooseDownloadedAudio,
            useBlurFrames = useBlurFrames,
        )

        Destination.Search -> SearchScreen(library.tracks, viewModel::play)
        Destination.Library -> LibraryScreen(
            tracks = library.tracks,
            isImporting = library.isImporting,
            onPlay = viewModel::play,
            onOpenDownloadedAudio = chooseDownloadedAudio,
        )

        Destination.EchoFind -> EchoFindScreen()
        Destination.Profile -> ProfileScreen(
            onOpenAppearance = onOpenAppearance,
            onOpenConnections = onOpenConnections,
            useBlurFrames = useBlurFrames,
        )
    }
}

@Composable
private fun GlassBottomBar(
    selected: Destination,
    useBlurFrames: Boolean,
    onSelect: (Destination) -> Unit,
) {
    GlassFrame(
        useBlurFrames = useBlurFrames,
        shape = RoundedCornerShape(28.dp),
    ) {
        NavigationBar(containerColor = Color.Transparent) {
            Destination.entries.forEach { destination ->
                NavigationBarItem(
                    selected = selected == destination,
                    onClick = { onSelect(destination) },
                    icon = { Icon(destination.icon, contentDescription = destination.label) },
                    label = { Text(destination.label) },
                )
            }
        }
    }
}

@Composable
private fun GlassNavigationRail(
    selected: Destination,
    useBlurFrames: Boolean,
    onSelect: (Destination) -> Unit,
) {
    GlassFrame(
        useBlurFrames = useBlurFrames,
        modifier = Modifier
            .fillMaxHeight()
            .padding(start = 14.dp, bottom = 14.dp),
        shape = RoundedCornerShape(30.dp),
    ) {
        NavigationRail(containerColor = Color.Transparent) {
            Spacer(Modifier.height(24.dp))
            Destination.entries.forEach { destination ->
                NavigationRailItem(
                    selected = selected == destination,
                    onClick = { onSelect(destination) },
                    icon = { Icon(destination.icon, contentDescription = destination.label) },
                    label = { Text(destination.label) },
                )
            }
        }
    }
}

@Composable
private fun HomeScreen(
    state: LocalLibraryState,
    onLoad: () -> Unit,
    onPlay: (LocalTrack) -> Unit,
    onOpenDownloadedAudio: () -> Unit,
    useBlurFrames: Boolean,
) {
    val context = LocalContext.current
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
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasPermission = granted
        if (granted) onLoad()
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission && state.tracks.isEmpty() && !state.isLoading) onLoad()
    }

    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text("Rock Music", style = MaterialTheme.typography.displaySmall)
            Text(
                "Local music, downloaded audio, podcasts, and listening rooms.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        item {
            GlassFrame(
                useBlurFrames = useBlurFrames,
                shape = RoundedCornerShape(30.dp),
            ) {
                Column(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                    Color.Transparent,
                                ),
                            ),
                        )
                        .padding(22.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        Icons.Rounded.Download,
                        contentDescription = null,
                        modifier = Modifier.size(38.dp),
                    )
                    Text("Play downloaded audio", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Choose an MP3, M4A, FLAC, OGG, OPUS, WAV, AAC, AMR, or 3GP file. Rock Music keeps read access and plays it through Media3.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FilledTonalButton(
                        onClick = onOpenDownloadedAudio,
                        enabled = !state.isImporting,
                    ) {
                        if (state.isImporting) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(if (state.isImporting) "Opening…" else "Open downloaded audio")
                    }
                }
            }
        }

        item {
            Text("On this phone", style = MaterialTheme.typography.headlineSmall)
        }

        when {
            state.error != null -> item {
                ErrorCard(
                    title = "Audio unavailable",
                    message = state.error.orEmpty(),
                    primaryAction = "Choose another file",
                    onPrimaryAction = onOpenDownloadedAudio,
                    secondaryAction = if (hasPermission) "Scan again" else null,
                    onSecondaryAction = if (hasPermission) onLoad else null,
                )
            }

            !hasPermission -> item {
                ActionCard(
                    title = "Allow full library access",
                    body = "Grant audio permission to scan music already indexed on this phone. The file picker above works without full-library access.",
                    icon = Icons.Rounded.Storage,
                    button = "Allow music access",
                    onClick = { permissionLauncher.launch(permission) },
                )
            }

            state.isLoading -> item { CircularProgressIndicator() }

            state.tracks.isEmpty() -> item {
                ActionCard(
                    title = "No indexed tracks found",
                    body = "Open a downloaded file directly or rescan after Android finishes indexing it.",
                    icon = Icons.Rounded.Storage,
                    button = "Open audio file",
                    onClick = onOpenDownloadedAudio,
                )
            }

            else -> item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.tracks.take(12), key = { it.mediaUri }) { track ->
                        TrackCard(track, onPlay)
                    }
                }
            }
        }

        item {
            Text("Explore", style = MaterialTheme.typography.headlineSmall)
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item { SectionCard("Podcasts", "RSS feeds, progress, and permitted downloads", Icons.Rounded.Podcasts) }
                item { SectionCard("Listen Together", "Synchronized rooms with legal source access", Icons.Rounded.Headphones) }
                item { SectionCard("Offline", "Downloaded audio and provider-permitted files", Icons.Rounded.WifiOff) }
            }
        }
    }
}

@Composable
private fun SearchScreen(tracks: List<LocalTrack>, onPlay: (LocalTrack) -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = remember(query, tracks) {
        if (query.isBlank()) {
            emptyList()
        } else {
            tracks.filter {
                it.title.contains(query, true) ||
                    it.artist.contains(query, true) ||
                    it.album.contains(query, true)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        Text("Search", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(16.dp))
        TextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            placeholder = { Text("Songs, artists, albums, podcasts") },
            singleLine = true,
            shape = RoundedCornerShape(22.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        when {
            query.isBlank() -> Text(
                "Search local and manually opened audio. Licensed providers appear after configuration.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            results.isEmpty() -> Text("No local matches.")

            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(results, key = { it.mediaUri }) { TrackRow(it, onPlay) }
            }
        }
    }
}

@Composable
private fun LibraryScreen(
    tracks: List<LocalTrack>,
    isImporting: Boolean,
    onPlay: (LocalTrack) -> Unit,
    onOpenDownloadedAudio: () -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { Text("Library", style = MaterialTheme.typography.displaySmall) }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Songs · Albums · Artists · Downloads",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(onClick = onOpenDownloadedAudio, enabled = !isImporting) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Open file")
                }
            }
        }
        if (tracks.isEmpty()) {
            item {
                ActionCard(
                    title = "Library is empty",
                    body = "Choose a downloaded audio file or grant local music access from Home.",
                    icon = Icons.Rounded.LibraryMusic,
                    button = "Open downloaded audio",
                    onClick = onOpenDownloadedAudio,
                )
            }
        } else {
            items(tracks, key = { it.mediaUri }) { TrackRow(it, onPlay) }
        }
    }
}

@Composable
private fun EchoFindScreen() {
    val context = LocalContext.current
    var status by remember { mutableStateOf("Tap to identify a song") }
    var listening by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            listening = true
            status = "Listening for a short sample…"
        } else {
            status = "Microphone permission is required only while Echo Find is active."
        }
    }

    LaunchedEffect(listening) {
        if (listening) {
            delay(4_000)
            listening = false
            status = "Connect a licensed recognition provider to match this sample."
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Text("Echo Find", style = MaterialTheme.typography.displaySmall)
        Spacer(Modifier.height(16.dp))
        Text(status)
        Spacer(Modifier.height(36.dp))
        FilledIconButton(
            onClick = {
                if (
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO,
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    listening = true
                    status = "Listening for a short sample…"
                } else {
                    launcher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            modifier = Modifier.size(132.dp),
            shape = CircleShape,
        ) {
            if (listening) {
                CircularProgressIndicator()
            } else {
                Icon(Icons.Rounded.Mic, "Start Echo Find", Modifier.size(48.dp))
            }
        }
        Spacer(Modifier.height(24.dp))
        Text(
            "Audio is sent only with explicit consent and only to a licensed recognition provider.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ProfileScreen(
    onOpenAppearance: () -> Unit,
    onOpenConnections: () -> Unit,
    useBlurFrames: Boolean,
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 26.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { Text("Settings", style = MaterialTheme.typography.displaySmall) }
        item {
            ActionCard(
                title = "Appearance",
                body = "Dark, light, system colour, and ${if (useBlurFrames) "enabled" else "disabled"} blur frames.",
                icon = Icons.Rounded.Palette,
                button = "Customize design",
                onClick = onOpenAppearance,
            )
        }
        item {
            ActionCard(
                title = "Service connections",
                body = "Spotify, licensed catalogues, lyrics, cloud storage, Discord, and provider status.",
                icon = Icons.Rounded.Settings,
                button = "Open connections",
                onClick = onOpenConnections,
            )
        }
        item {
            SectionCard(
                "Navigation design",
                "Responsive bottom navigation on phones and rounded rail navigation on tablets.",
                Icons.Rounded.BlurOn,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            SectionCard(
                "About Rock Music",
                "Native Android music player · GPL-3.0-only",
                Icons.Rounded.GraphicEq,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun ActionCard(
    title: String,
    body: String,
    icon: ImageVector,
    button: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(34.dp))
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick = onClick) { Text(button) }
        }
    }
}

@Composable
private fun ErrorCard(
    title: String,
    message: String,
    primaryAction: String,
    onPrimaryAction: () -> Unit,
    secondaryAction: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.errorContainer,
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(message)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onPrimaryAction) { Text(primaryAction) }
                if (secondaryAction != null && onSecondaryAction != null) {
                    OutlinedButton(onClick = onSecondaryAction) { Text(secondaryAction) }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier.width(270.dp),
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(34.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TrackCard(track: LocalTrack, onPlay: (LocalTrack) -> Unit) {
    Surface(
        modifier = Modifier
            .width(180.dp)
            .clickable { onPlay(track) },
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column {
            AsyncImage(
                model = track.artworkUri,
                contentDescription = "Artwork for ${track.title}",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Column(Modifier.padding(14.dp)) {
                Text(
                    track.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                )
                Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun TrackRow(track: LocalTrack, onPlay: (LocalTrack) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onPlay(track) },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = track.artworkUri,
                contentDescription = null,
                modifier = Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    track.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                )
                Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    track.album,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play ${track.title}")
        }
    }
}

@Composable
private fun PlayerOverlay(
    player: PlayerUiState,
    useBlurFrames: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onDismissError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val playbackError = player.errorMessage
        if (playbackError != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("Playback failed", fontWeight = FontWeight.Bold)
                        Text(playbackError, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = onDismissError) {
                        Icon(Icons.Rounded.Close, contentDescription = "Dismiss playback error")
                    }
                }
            }
        }

        if (player.hasMedia) {
            MiniPlayer(
                player = player,
                useBlurFrames = useBlurFrames,
                onOpen = onOpen,
                onToggle = onToggle,
            )
        }
    }
}

@Composable
private fun MiniPlayer(
    player: PlayerUiState,
    useBlurFrames: Boolean,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
) {
    GlassFrame(
        useBlurFrames = useBlurFrames,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        shape = RoundedCornerShape(22.dp),
    ) {
        Row(
            modifier = Modifier.padding(9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = player.artworkUri,
                contentDescription = null,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    player.title.orEmpty(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = FontWeight.Bold,
                )
                Text(player.artist.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (player.isPreparing) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(8.dp))
            }
            IconButton(onClick = onToggle, enabled = !player.isPreparing) {
                Icon(
                    if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = "Play or pause",
                )
            }
        }
    }
}

@Composable
private fun NowPlayingScreen(
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
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.52f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .statusBarsPadding(),
    ) {
        IconButton(
            onClick = onClose,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(18.dp),
        ) {
            Icon(Icons.Rounded.Close, contentDescription = "Close now playing")
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
        ) {
            GlassFrame(
                useBlurFrames = useBlurFrames,
                shape = RoundedCornerShape(32.dp),
            ) {
                AsyncImage(
                    model = player.artworkUri,
                    contentDescription = "Album artwork",
                    modifier = Modifier
                        .fillMaxWidth()
                        .width(520.dp)
                        .aspectRatio(1f)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                )
            }
            Spacer(Modifier.height(28.dp))
            Text(
                player.title.orEmpty(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black,
            )
            Text(player.artist.orEmpty(), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(22.dp))
            Slider(
                value = player.positionMs.coerceAtMost(player.durationMs).toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..player.durationMs.coerceAtLeast(1L).toFloat(),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPrevious, enabled = player.hasPrevious) {
                    Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous")
                }
                FilledIconButton(
                    onClick = onToggle,
                    enabled = !player.isPreparing,
                    modifier = Modifier.size(72.dp),
                ) {
                    if (player.isPreparing) {
                        CircularProgressIndicator(Modifier.size(30.dp), strokeWidth = 3.dp)
                    } else {
                        Icon(
                            if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = "Play or pause",
                            modifier = Modifier.size(36.dp),
                        )
                    }
                }
                IconButton(onClick = onNext, enabled = player.hasNext) {
                    Icon(Icons.Rounded.SkipNext, contentDescription = "Next")
                }
            }
        }
    }
}

@Composable
private fun GlassFrame(
    useBlurFrames: Boolean,
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(26.dp),
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = shape,
        color = if (useBlurFrames) {
            MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        tonalElevation = if (useBlurFrames) 0.dp else 4.dp,
        shadowElevation = if (useBlurFrames) 0.dp else 2.dp,
        border = if (useBlurFrames) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
        } else {
            null
        },
        content = content,
    )
}
