package com.rockmusic.app.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
fun RockMusicRoot(viewModel: MainViewModel = hiltViewModel()) {
    val library by viewModel.libraryState.collectAsStateWithLifecycle()
    val player by viewModel.playerState.collectAsStateWithLifecycle()
    var destination by remember { mutableStateOf(Destination.Home) }
    var showNowPlaying by remember { mutableStateOf(false) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 840.dp
        if (useRail) {
            Row(Modifier.fillMaxSize()) {
                RockNavigationRail(destination) { destination = it }
                Box(Modifier.weight(1f)) {
                    DestinationContent(destination, library, viewModel)
                    if (player.hasMedia) {
                        MiniPlayer(
                            player = player,
                            onOpen = { showNowPlaying = true },
                            onToggle = viewModel::togglePlayPause,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(16.dp),
                        )
                    }
                }
            }
        } else {
            Scaffold(
                bottomBar = {
                    Column {
                        if (player.hasMedia) {
                            MiniPlayer(
                                player = player,
                                onOpen = { showNowPlaying = true },
                                onToggle = viewModel::togglePlayPause,
                            )
                        }
                        RockBottomBar(destination) { destination = it }
                    }
                },
            ) { padding ->
                Box(Modifier.padding(padding)) {
                    DestinationContent(destination, library, viewModel)
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
private fun DestinationContent(
    destination: Destination,
    library: LocalLibraryState,
    viewModel: MainViewModel,
) {
    when (destination) {
        Destination.Home -> HomeScreen(library, viewModel::loadLocalMusic, viewModel::play)
        Destination.Search -> SearchScreen(library.tracks, viewModel::play)
        Destination.Library -> LibraryScreen(library.tracks, viewModel::play)
        Destination.EchoFind -> EchoFindScreen()
        Destination.Profile -> ProfileScreen()
    }
}

@Composable
private fun RockBottomBar(selected: Destination, onSelect: (Destination) -> Unit) {
    NavigationBar {
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

@Composable
private fun RockNavigationRail(selected: Destination, onSelect: (Destination) -> Unit) {
    NavigationRail(Modifier.fillMaxHeight()) {
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

@Composable
private fun HomeScreen(
    state: LocalLibraryState,
    onLoad: () -> Unit,
    onPlay: (LocalTrack) -> Unit,
) {
    val context = LocalContext.current
    val permission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        if (granted) onLoad()
    }

    LaunchedEffect(hasPermission) {
        if (hasPermission && state.tracks.isEmpty() && !state.isLoading) onLoad()
    }

    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text("Rock Music", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
            Text("Your music, podcasts, and listening rooms in one native Android app.")
        }
        item {
            SectionCard(
                title = "Continue listening",
                subtitle = "Your active queue and recent sessions will appear here.",
                icon = Icons.Rounded.Headphones,
            )
        }
        item {
            Text("Local music", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        when {
            !hasPermission -> item {
                PermissionCard(
                    title = "Allow local music access",
                    body = "Rock Music scans audio on this device only after you grant permission. Local files are never uploaded without consent.",
                    button = "Choose access",
                    onClick = { launcher.launch(permission) },
                )
            }
            state.isLoading -> item { CircularProgressIndicator() }
            state.error != null -> item {
                PermissionCard("Local library unavailable", state.error, "Retry", onLoad)
            }
            state.tracks.isEmpty() -> item {
                SectionCard("No local tracks found", "Add supported audio files or choose different folders in Settings.", Icons.Rounded.Storage)
            }
            else -> item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(state.tracks.take(12), key = { it.id }) { track ->
                        TrackCard(track, onPlay)
                    }
                }
            }
        }
        item {
            Text("Explore", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        }
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                item { SectionCard("Podcasts", "RSS feeds, progress, downloads", Icons.Rounded.Podcasts, Modifier.width(260.dp)) }
                item { SectionCard("Listen Together", "Synchronized rooms with legal source access", Icons.Rounded.Headphones, Modifier.width(260.dp)) }
                item { SectionCard("Offline", "Permitted downloads and local files", Icons.Rounded.WifiOff, Modifier.width(260.dp)) }
            }
        }
    }
}

@Composable
private fun SearchScreen(tracks: List<LocalTrack>, onPlay: (LocalTrack) -> Unit) {
    var query by remember { mutableStateOf("") }
    val results = remember(query, tracks) {
        if (query.isBlank()) emptyList() else tracks.filter {
            it.title.contains(query, true) || it.artist.contains(query, true) || it.album.contains(query, true)
        }
    }
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Search", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(16.dp))
        TextField(
            value = query,
            onValueChange = { query = it },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            placeholder = { Text("Songs, artists, albums, podcasts") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        if (query.isBlank()) {
            Text("Recent searches and provider suggestions will appear here.")
        } else if (results.isEmpty()) {
            Text("No local matches. Connected licensed services will be searched when configured.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(results, key = { it.id }) { TrackRow(it, onPlay) }
            }
        }
    }
}

@Composable
private fun LibraryScreen(tracks: List<LocalTrack>, onPlay: (LocalTrack) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { Text("Library", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black) }
        item { Text("Liked songs · Albums · Artists · Playlists · Downloads · Podcasts") }
        if (tracks.isEmpty()) {
            item { SectionCard("Library is empty", "Grant local music access or connect a compliant service.", Icons.Rounded.LibraryMusic) }
        } else {
            items(tracks, key = { it.id }) { TrackRow(it, onPlay) }
        }
    }
}

@Composable
private fun EchoFindScreen() {
    val context = LocalContext.current
    var status by remember { mutableStateOf("Tap to identify a song") }
    var listening by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
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
        modifier = Modifier.fillMaxSize().padding(24.dp),
    ) {
        Text("Echo Find", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(16.dp))
        Text(status)
        Spacer(Modifier.height(36.dp))
        FilledIconButton(
            onClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                    listening = true
                    status = "Listening for a short sample…"
                } else {
                    launcher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            modifier = Modifier.size(132.dp),
            shape = CircleShape,
        ) {
            if (listening) CircularProgressIndicator() else Icon(Icons.Rounded.Mic, "Start Echo Find", Modifier.size(48.dp))
        }
        Spacer(Modifier.height(24.dp))
        Text("Audio must be sent only with explicit consent and only to a licensed recognition provider.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ProfileScreen() {
    LazyColumn(
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { Text("Profile", style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Black) }
        item { SectionCard("Service connections", "Spotify OAuth, licensed catalogues, user cloud storage, and Discord activity are opt-in.", Icons.Rounded.Headphones) }
        item { SectionCard("Privacy", "Delete listening history, recognition history, tokens, social data, and account data.", Icons.Rounded.Settings) }
        item { SectionCard("About Rock Music", "Native Android foundation · version 0.1.0", Icons.Rounded.GraphicEq) }
    }
}

@Composable
private fun PermissionCard(title: String, body: String, button: String, onClick: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(body)
            Button(onClick = onClick) { Text(button) }
        }
    }
}

@Composable
private fun SectionCard(title: String, subtitle: String, icon: ImageVector, modifier: Modifier = Modifier.fillMaxWidth()) {
    Card(modifier = modifier, shape = RoundedCornerShape(24.dp)) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, Modifier.size(34.dp))
            Spacer(Modifier.width(14.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun TrackCard(track: LocalTrack, onPlay: (LocalTrack) -> Unit) {
    Card(
        modifier = Modifier.width(180.dp).clickable { onPlay(track) },
        shape = RoundedCornerShape(22.dp),
    ) {
        AsyncImage(
            model = track.artworkUri,
            contentDescription = "Artwork for ${track.title}",
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Column(Modifier.padding(12.dp)) {
            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
            Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TrackRow(track: LocalTrack, onPlay: (LocalTrack) -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).clickable { onPlay(track) }.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = track.artworkUri,
            contentDescription = null,
            modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(track.title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
            Text(track.artist, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.Rounded.PlayArrow, contentDescription = "Play ${track.title}")
    }
}

@Composable
private fun MiniPlayer(
    player: PlayerUiState,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).clickable(onClick = onOpen),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = player.artworkUri,
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(player.title.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Bold)
                Text(player.artist.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            IconButton(onClick = onToggle) {
                Icon(if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play or pause")
            }
        }
    }
}

@Composable
private fun NowPlayingScreen(
    player: PlayerUiState,
    onClose: () -> Unit,
    onToggle: () -> Unit,
    onSeek: (Long) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
) {
    BackHandler(onBack = onClose)
    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(
                listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), MaterialTheme.colorScheme.background),
            ),
        ),
    ) {
        IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).padding(18.dp)) {
            Icon(Icons.Rounded.Close, "Close now playing")
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize().padding(28.dp),
        ) {
            AsyncImage(
                model = player.artworkUri,
                contentDescription = "Album artwork",
                modifier = Modifier.fillMaxWidth().width(520.dp).aspectRatio(1f).clip(RoundedCornerShape(28.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Spacer(Modifier.height(28.dp))
            Text(player.title.orEmpty(), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
            Text(player.artist.orEmpty(), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(22.dp))
            Slider(
                value = player.positionMs.coerceAtMost(player.durationMs).toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..player.durationMs.coerceAtLeast(1L).toFloat(),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrevious, enabled = player.hasPrevious) { Icon(Icons.Rounded.SkipPrevious, "Previous") }
                FilledIconButton(onClick = onToggle, modifier = Modifier.size(72.dp)) {
                    Icon(if (player.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow, "Play or pause", Modifier.size(36.dp))
                }
                IconButton(onClick = onNext, enabled = player.hasNext) { Icon(Icons.Rounded.SkipNext, "Next") }
            }
        }
    }
}
