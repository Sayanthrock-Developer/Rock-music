package com.rockmusic.app.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rockmusic.app.data.local.LocalMusicRepository
import com.rockmusic.app.domain.model.LocalTrack
import com.rockmusic.app.domain.policy.MediaActionDecision
import com.rockmusic.app.domain.policy.MediaActionPolicyEngine
import com.rockmusic.app.domain.policy.MediaActionRequest
import com.rockmusic.app.domain.policy.MediaOperation
import com.rockmusic.app.domain.policy.MediaOrigin
import com.rockmusic.app.player.PlayerConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MainViewModel @Inject constructor(
    private val localMusicRepository: LocalMusicRepository,
    private val playerConnection: PlayerConnection,
    private val mediaActionPolicy: MediaActionPolicyEngine,
) : ViewModel() {
    val playerState = playerConnection.state

    private val _libraryState = MutableStateFlow(LocalLibraryState())
    val libraryState: StateFlow<LocalLibraryState> = _libraryState.asStateFlow()

    private val _lastActionDecision = MutableStateFlow<MediaActionDecision?>(null)
    val lastActionDecision: StateFlow<MediaActionDecision?> = _lastActionDecision.asStateFlow()

    fun loadLocalMusic() {
        scanDeviceMusic(
            loadingMessage = "Scanning songs on this phone…",
            successMessage = { count -> "Found $count songs on this phone." },
            autoPlay = false,
        )
    }

    fun rescanDeviceLibrary() {
        scanDeviceMusic(
            loadingMessage = "Applying folder exclusions and rescanning…",
            successMessage = { count -> "Library updated with $count included songs." },
            autoPlay = false,
        )
    }

    fun addAllSongs(autoPlay: Boolean = true) {
        scanDeviceMusic(
            loadingMessage = "Scanning and adding all included songs…",
            successMessage = { count -> "Added $count included songs." },
            autoPlay = autoPlay,
        )
    }

    private fun scanDeviceMusic(
        loadingMessage: String,
        successMessage: (Int) -> String,
        autoPlay: Boolean,
    ) {
        if (_libraryState.value.isLoading || _libraryState.value.isImporting) return
        viewModelScope.launch {
            _libraryState.value = _libraryState.value.copy(
                isLoading = true,
                error = null,
                statusMessage = loadingMessage,
            )
            runCatching { localMusicRepository.scan() }
                .onSuccess { scanned ->
                    val current = _libraryState.value
                    val manuallyImported = current.tracks.filterNot { track ->
                        track.mediaUri in current.deviceTrackUris
                    }
                    _libraryState.value = current.copy(
                        tracks = mergeTracks(manuallyImported, scanned),
                        deviceTrackUris = scanned.mapTo(mutableSetOf(), LocalTrack::mediaUri),
                        isLoading = false,
                        error = null,
                        statusMessage = successMessage(scanned.size),
                    )
                    if (autoPlay) playAll(scanned)
                }
                .onFailure { error ->
                    _libraryState.value = _libraryState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Unable to scan local music",
                        statusMessage = null,
                    )
                }
        }
    }

    fun openDownloadedAudio(uri: Uri) {
        addDownloadedAudio(listOf(uri), autoPlay = true)
    }

    fun addDownloadedAudio(uris: List<Uri>, autoPlay: Boolean = true) {
        if (uris.isEmpty() || _libraryState.value.isImporting) return
        viewModelScope.launch {
            _libraryState.value = _libraryState.value.copy(
                isImporting = true,
                error = null,
                statusMessage = "Opening ${uris.size} audio file${if (uris.size == 1) "" else "s"}…",
            )
            runCatching { localMusicRepository.resolveAll(uris) }
                .onSuccess { imported ->
                    _libraryState.value = _libraryState.value.copy(
                        tracks = mergeTracks(_libraryState.value.tracks, imported),
                        isImporting = false,
                        error = null,
                        statusMessage = "Added ${imported.size} downloaded song${if (imported.size == 1) "" else "s"}.",
                    )
                    if (autoPlay) playAll(imported)
                }
                .onFailure {
                    _libraryState.value = _libraryState.value.copy(
                        isImporting = false,
                        error = it.message ?: "Unable to open the downloaded audio files",
                        statusMessage = null,
                    )
                }
        }
    }

    fun openSongFolder(treeUri: Uri, autoPlay: Boolean = true) {
        if (_libraryState.value.isImporting) return
        viewModelScope.launch {
            _libraryState.value = _libraryState.value.copy(
                isImporting = true,
                error = null,
                statusMessage = "Scanning the selected song folder…",
            )
            runCatching { localMusicRepository.scanFolder(treeUri) }
                .onSuccess { imported ->
                    _libraryState.value = _libraryState.value.copy(
                        tracks = mergeTracks(_libraryState.value.tracks, imported),
                        isImporting = false,
                        error = null,
                        statusMessage = "Added ${imported.size} songs from the selected folder.",
                    )
                    if (autoPlay) playAll(imported)
                }
                .onFailure {
                    _libraryState.value = _libraryState.value.copy(
                        isImporting = false,
                        error = it.message ?: "Unable to scan the selected song folder",
                        statusMessage = null,
                    )
                }
        }
    }

    fun play(track: LocalTrack) {
        playAll(listOf(track))
    }

    fun playAll(tracks: List<LocalTrack>) {
        val decisions = tracks.distinctBy(LocalTrack::mediaUri)
            .map { track -> track to mediaActionPolicy.decide(track.toPlayRequest()) }

        _lastActionDecision.value = decisions
            .firstOrNull { (_, decision) -> decision != MediaActionDecision.ExecuteInApp }
            ?.second
            ?: decisions.firstOrNull()?.second

        val approvedTracks = decisions
            .filter { (_, decision) -> decision == MediaActionDecision.ExecuteInApp }
            .map(Pair<LocalTrack, MediaActionDecision>::first)

        if (approvedTracks.isNotEmpty()) {
            playerConnection.playQueue(approvedTracks)
        }
    }

    fun togglePlayPause() = playerConnection.togglePlayPause()
    fun seekTo(positionMs: Long) = playerConnection.seekTo(positionMs)
    fun skipNext() = playerConnection.skipNext()
    fun skipPrevious() = playerConnection.skipPrevious()
    fun clearPlayerError() = playerConnection.clearError()

    private fun LocalTrack.toPlayRequest() = MediaActionRequest(
        operation = MediaOperation.PLAY,
        origin = MediaOrigin.LOCAL_FILE,
        mediaId = id.toString(),
        sourceUri = mediaUri,
    )

    private fun mergeTracks(
        existing: List<LocalTrack>,
        incoming: List<LocalTrack>,
    ): List<LocalTrack> = (incoming + existing).distinctBy(LocalTrack::mediaUri)
}

data class LocalLibraryState(
    val tracks: List<LocalTrack> = emptyList(),
    val deviceTrackUris: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isImporting: Boolean = false,
    val error: String? = null,
    val statusMessage: String? = null,
)
