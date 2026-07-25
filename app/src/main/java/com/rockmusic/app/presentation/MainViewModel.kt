package com.rockmusic.app.presentation

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

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
            _libraryState.value = _libraryState.value.copy(isLoading = true, error = null)
            _libraryState.value = runCatching { localMusicRepository.scan() }
                .fold(
                    onSuccess = { LocalLibraryState(tracks = it) },
                    onFailure = { LocalLibraryState(error = it.message ?: "Unable to scan local music") },
                )
        }
    }

    fun play(track: LocalTrack) {
        val decision = mediaActionPolicy.decide(
            MediaActionRequest(
                operation = MediaOperation.PLAY,
                origin = MediaOrigin.LOCAL_FILE,
                mediaId = track.id.toString(),
                sourceUri = track.mediaUri,
            ),
        )
        _lastActionDecision.value = decision

        if (decision == MediaActionDecision.ExecuteInApp) {
            playerConnection.play(track)
        }
    }

    fun togglePlayPause() = playerConnection.togglePlayPause()
    fun seekTo(positionMs: Long) = playerConnection.seekTo(positionMs)
    fun skipNext() = playerConnection.skipNext()
    fun skipPrevious() = playerConnection.skipPrevious()
}

data class LocalLibraryState(
    val tracks: List<LocalTrack> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)
