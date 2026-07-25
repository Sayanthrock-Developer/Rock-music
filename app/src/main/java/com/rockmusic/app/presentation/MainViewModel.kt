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
        if (_libraryState.value.isLoading) return
        viewModelScope.launch {
            _libraryState.value = _libraryState.value.copy(
                isLoading = true,
                error = null,
                statusMessage = "Scanning songs on this phone…",
            )
            _libraryState.value = runCatching { localMusicRepository.scan() }
                .fold(
                    onSuccess = { scanned ->
                        val merged = mergeTracks(_libraryState.value.tracks, scanned)
                        LocalLibraryState(
                            tracks = merged,
                            statusMessage = "Found ${scanned.size} songs on this phone.",
                        )
                    },
                    onFailure = {
                        _libraryState.value.copy(
                            isLoading = false,
                            error = it.message ?: "Unable to scan local music",
                            statusMessage = null,
                        )
                    },
                )
        }
    }

    fun addAllSongs(autoPlay: Boolean = true) {
        if (_libraryState.value.isLoading || _libraryState.value.isImporting) return
        viewModelScope.launch {
            _libraryState.value = _libraryState.value.copy(
                isLoading = true,
                error = null,
                statusMessage = "Scanning and adding all songs…",
            )
            runCatching { localMusicRepository.scan() }
                .onSuccess { scanned ->
                    _libraryState.value = _libraryState.value.copy(
                        tracks = mergeTracks(_libraryState.value.tracks, scanned),
                        isLoading = false,
                        error = null,
                        statusMessage = "Added ${scanned.size} songs.",
                    )
                    if (autoPlay) playAll(scanned)
                }
                .onFailure {
                    _libraryState.value = _libraryState.value.copy(
                        isLoading = false,
                        error = it.message ?: "Unable to add songs from this phone",
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
        val approvedTracks = tracks.distinctBy(LocalTrack::mediaUri).filter { track ->
            val decision = mediaActionPolicy.decide(track.toPlayRequest())
            _lastActionDecision.value = decision
            decision == MediaActionDecision.ExecuteInApp
        }

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
    val isLoading: Boolean = false,
    val isImporting: Boolean = false,
    val error: String? = null,
    val statusMessage: String? = null,
)
