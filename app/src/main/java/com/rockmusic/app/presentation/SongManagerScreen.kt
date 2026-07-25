package com.rockmusic.app.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rockmusic.app.domain.model.LocalTrack

@Composable
fun SongManagerScreen(
    onClose: () -> Unit,
    viewModel: MainViewModel = hiltViewModel(),
) {
    BackHandler(onBack = onClose)

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by viewModel.libraryState.collectAsStateWithLifecycle()
    var selectedUris by remember { mutableStateOf<Set<String>>(emptySet()) }
    val allTrackUris = remember(state.tracks) {
        state.tracks.mapTo(mutableSetOf(), LocalTrack::mediaUri)
    }
    val allSelected = allTrackUris.isNotEmpty() && selectedUris == allTrackUris

    val audioPermission = if (Build.VERSION.SDK_INT >= 33) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    var hasAudioPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, audioPermission) == PackageManager.PERMISSION_GRANTED,
        )
    }
    var permissionMessage by remember { mutableStateOf<String?>(null) }

    fun persistReadAccess(uri: Uri) {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }
    }

    val openOneAudio = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            persistReadAccess(uri)
            viewModel.openDownloadedAudio(uri)
        }
    }

    val openManyAudio = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach(::persistReadAccess)
            viewModel.addDownloadedAudio(uris, autoPlay = true)
        }
    }

    val openSongFolder = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
    ) { treeUri ->
        if (treeUri != null) {
            persistReadAccess(treeUri)
            viewModel.openSongFolder(treeUri, autoPlay = true)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasAudioPermission = granted
        if (granted) {
            permissionMessage = null
            viewModel.addAllSongs(autoPlay = true)
        } else {
            permissionMessage =
                "Music access was denied. Grant audio access in App settings to use Add All Songs."
        }
    }

    DisposableEffect(lifecycleOwner, context, audioPermission) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAudioPermission = ContextCompat.checkSelfPermission(
                    context,
                    audioPermission,
                ) == PackageManager.PERMISSION_GRANTED
                if (hasAudioPermission) permissionMessage = null
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(allTrackUris) {
        selectedUris = selectedUris.intersect(allTrackUris)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Song Manager", style = MaterialTheme.typography.displaySmall)
                    Text(
                        "Open, scan, select, add, and play local audio.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close Song Manager")
                }
            }
        }

        item {
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Storage, contentDescription = null)
                        Spacer(Modifier.width(10.dp))
                        Text("Import songs", style = MaterialTheme.typography.titleLarge)
                    }
                    Text(
                        "Folder scans, multi-file imports, and Add All Songs start a Media3 queue automatically.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FilledTonalButton(
                        onClick = { openOneAudio.launch(arrayOf("audio/*")) },
                        enabled = !state.isImporting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open downloaded audio")
                    }
                    OutlinedButton(
                        onClick = { openManyAudio.launch(arrayOf("audio/*")) },
                        enabled = !state.isImporting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.LibraryMusic, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Select multiple songs")
                    }
                    OutlinedButton(
                        onClick = { openSongFolder.launch(null) },
                        enabled = !state.isImporting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Rounded.Storage, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Select song folder")
                    }
                    Button(
                        onClick = {
                            if (hasAudioPermission) {
                                permissionMessage = null
                                viewModel.addAllSongs(autoPlay = true)
                            } else {
                                permissionLauncher.launch(audioPermission)
                            }
                        },
                        enabled = !state.isLoading && !state.isImporting,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("Add All Songs")
                    }
                }
            }
        }

        if (permissionMessage != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            permissionMessage.orEmpty(),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        OutlinedButton(
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:${context.packageName}"),
                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            },
                        ) {
                            Text("Open App settings")
                        }
                    }
                }
            }
        }

        if (state.statusMessage != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Text(
                        state.statusMessage.orEmpty(),
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }

        if (state.error != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                ) {
                    Text(
                        state.error.orEmpty(),
                        modifier = Modifier.padding(14.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("Songs", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "${state.tracks.size} available · ${selectedUris.size} selected",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                OutlinedButton(
                    onClick = {
                        selectedUris = if (allSelected) emptySet() else allTrackUris
                    },
                    enabled = allTrackUris.isNotEmpty(),
                ) {
                    Text(if (allSelected) "Clear all" else "Select all")
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilledTonalButton(
                    onClick = {
                        viewModel.playAll(state.tracks.filter { it.mediaUri in selectedUris })
                    },
                    enabled = selectedUris.isNotEmpty(),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Play selected")
                }
                OutlinedButton(
                    onClick = { viewModel.playAll(state.tracks) },
                    enabled = state.tracks.isNotEmpty(),
                ) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Play all")
                }
            }
        }

        if (state.tracks.isEmpty() && !state.isLoading && !state.isImporting) {
            item {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Rounded.LibraryMusic, contentDescription = null)
                        Text("No songs added", style = MaterialTheme.typography.titleLarge)
                        Text(
                            "Choose one file, select multiple songs, pick a folder, or use Add All Songs.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        } else {
            items(state.tracks, key = LocalTrack::mediaUri) { track ->
                val selected = track.mediaUri in selectedUris
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedUris = if (selected) {
                                selectedUris - track.mediaUri
                            } else {
                                selectedUris + track.mediaUri
                            }
                        },
                    shape = RoundedCornerShape(18.dp),
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainer
                    },
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = selected,
                            onCheckedChange = { checked ->
                                selectedUris = if (checked) {
                                    selectedUris + track.mediaUri
                                } else {
                                    selectedUris - track.mediaUri
                                }
                            },
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                track.title,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                "${track.artist} · ${track.album}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        IconButton(onClick = { viewModel.play(track) }) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play ${track.title}")
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}
