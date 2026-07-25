package com.rockmusic.app.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.rockmusic.app.domain.model.LocalTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlayerConnection @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controller: MediaController? = null
    private var pendingTrack: LocalTrack? = null

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish(player)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                _state.value = _state.value.copy(isPreparing = false, errorMessage = null)
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.value = _state.value.copy(
                isPreparing = false,
                errorMessage = error.localizedMessage
                    ?: "This audio file could not be played (code ${error.errorCode}).",
            )
        }
    }

    init {
        val token = SessionToken(context, ComponentName(context, MusicService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                runCatching { future.get() }
                    .onSuccess { mediaController ->
                        controller = mediaController
                        mediaController.addListener(listener)
                        publish(mediaController)
                        startPositionUpdates()
                        pendingTrack?.let { track ->
                            pendingTrack = null
                            startPlayback(mediaController, track)
                        }
                    }
                    .onFailure { error ->
                        _state.value = _state.value.copy(
                            isPreparing = false,
                            errorMessage = error.message ?: "Unable to start the audio player.",
                        )
                    }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    fun play(track: LocalTrack) {
        val mediaController = controller
        if (mediaController == null) {
            pendingTrack = track
            _state.value = _state.value.copy(
                title = track.title,
                artist = track.artist,
                album = track.album,
                artworkUri = track.artworkUri,
                isPreparing = true,
                errorMessage = null,
            )
            return
        }
        startPlayback(mediaController, track)
    }

    fun togglePlayPause() {
        controller?.let { mediaController ->
            if (mediaController.isPlaying) {
                mediaController.pause()
            } else {
                mediaController.play()
            }
        }
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs.coerceAtLeast(0L))
    }

    fun skipNext() {
        controller?.seekToNextMediaItem()
    }

    fun skipPrevious() {
        controller?.seekToPreviousMediaItem()
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    private fun startPlayback(mediaController: MediaController, track: LocalTrack) {
        val builder = MediaItem.Builder()
            .setUri(Uri.parse(track.mediaUri))
            .setMediaId("local:${track.id}")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .setArtworkUri(track.artworkUri?.let(Uri::parse))
                    .build(),
            )
        track.mimeType
            ?.takeIf(String::isNotBlank)
            ?.let(builder::setMimeType)

        _state.value = _state.value.copy(
            title = track.title,
            artist = track.artist,
            album = track.album,
            artworkUri = track.artworkUri,
            isPreparing = true,
            errorMessage = null,
        )
        mediaController.setMediaItem(builder.build())
        mediaController.prepare()
        mediaController.play()
    }

    private fun startPositionUpdates() {
        scope.launch {
            while (isActive) {
                controller?.let(::publish)
                delay(500)
            }
        }
    }

    private fun publish(player: Player) {
        val currentState = _state.value
        val metadata = player.currentMediaItem?.mediaMetadata
        _state.value = currentState.copy(
            title = metadata?.title?.toString() ?: currentState.title,
            artist = metadata?.artist?.toString() ?: currentState.artist,
            album = metadata?.albumTitle?.toString() ?: currentState.album,
            artworkUri = metadata?.artworkUri?.toString() ?: currentState.artworkUri,
            isPlaying = player.isPlaying,
            isPreparing = player.playbackState == Player.STATE_BUFFERING,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.takeIf { it > 0 } ?: 0L,
            hasNext = player.hasNextMediaItem(),
            hasPrevious = player.hasPreviousMediaItem(),
            errorMessage = currentState.errorMessage,
        )
    }
}

data class PlayerUiState(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val artworkUri: String? = null,
    val isPlaying: Boolean = false,
    val isPreparing: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val errorMessage: String? = null,
) {
    val hasMedia: Boolean get() = title != null
}
