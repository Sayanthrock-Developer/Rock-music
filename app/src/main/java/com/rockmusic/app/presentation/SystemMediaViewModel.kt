package com.rockmusic.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rockmusic.app.player.EmbeddedLyricsRepository
import com.rockmusic.app.player.PlayerConnection
import com.rockmusic.app.player.SystemAudioRoute
import com.rockmusic.app.player.SystemAudioRouteController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SystemMediaViewModel @Inject constructor(
    private val playerConnection: PlayerConnection,
    private val lyricsRepository: EmbeddedLyricsRepository,
    private val routeController: SystemAudioRouteController,
) : ViewModel() {
    private val _lyricsState = MutableStateFlow(LyricsUiState())
    val lyricsState: StateFlow<LyricsUiState> = _lyricsState.asStateFlow()

    private val _routeState = MutableStateFlow(AudioRouteUiState())
    val routeState: StateFlow<AudioRouteUiState> = _routeState.asStateFlow()

    fun toggleFavourite() = playerConnection.toggleFavourite()

    fun setVolume(value: Float) = playerConnection.setVolume(value)

    fun playQueueIndex(index: Int) = playerConnection.playQueueIndex(index)

    fun loadLyrics(mediaUri: String?) {
        if (mediaUri.isNullOrBlank()) {
            _lyricsState.value = LyricsUiState(error = "No local audio file is selected.")
            return
        }
        if (_lyricsState.value.isLoading) return

        viewModelScope.launch {
            _lyricsState.value = LyricsUiState(isLoading = true)
            lyricsRepository.read(mediaUri)
                .onSuccess { lyrics ->
                    _lyricsState.value = LyricsUiState(
                        lyrics = lyrics,
                        message = if (lyrics == null) {
                            "No embedded lyrics were found in this local audio file."
                        } else {
                            null
                        },
                    )
                }
                .onFailure { error ->
                    _lyricsState.value = LyricsUiState(
                        error = error.message ?: "Unable to read embedded lyrics.",
                    )
                }
        }
    }

    fun clearLyrics() {
        _lyricsState.value = LyricsUiState()
    }

    fun refreshAudioRoutes() {
        _routeState.value = runCatching { routeController.routes() }
            .fold(
                onSuccess = { routes -> AudioRouteUiState(routes = routes) },
                onFailure = { error ->
                    AudioRouteUiState(error = error.message ?: "Unable to read audio outputs.")
                },
            )
    }

    fun selectAudioRoute(index: Int) {
        routeController.select(index)
            .onSuccess {
                _routeState.value = AudioRouteUiState(
                    routes = routeController.routes(),
                    message = "Audio output changed.",
                )
            }
            .onFailure { error ->
                _routeState.value = _routeState.value.copy(
                    error = error.message ?: "Unable to change the audio output.",
                    message = null,
                )
            }
    }
}

data class LyricsUiState(
    val isLoading: Boolean = false,
    val lyrics: String? = null,
    val message: String? = null,
    val error: String? = null,
)

data class AudioRouteUiState(
    val routes: List<SystemAudioRoute> = emptyList(),
    val message: String? = null,
    val error: String? = null,
)
