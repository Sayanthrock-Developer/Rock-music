package com.rockmusic.app.player

import android.content.ComponentName
import android.content.Context
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
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

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish(player)
        }
    }

    init {
        val token = SessionToken(context, ComponentName(context, MusicService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener(
            {
                runCatching { future.get() }.onSuccess { mediaController ->
                    controller = mediaController
                    mediaController.addListener(listener)
                    publish(mediaController)
                    startPositionUpdates()
                }
            },
            ContextCompat.getMainExecutor(context),
        )
    }

    fun play(track: LocalTrack) {
        val item = MediaItem.Builder()
            .setUri(track.mediaUri)
            .setMediaId("local:${track.id}")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .setArtworkUri(track.artworkUri?.let(android.net.Uri::parse))
                    .build(),
            )
            .build()
        controller?.run {
            setMediaItem(item)
            prepare()
            play()
        }
    }

    fun togglePlayPause() {
        controller?.let { if (it.isPlaying) it.pause() else it.play() }
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

    private fun startPositionUpdates() {
        scope.launch {
            while (isActive) {
                controller?.let(::publish)
                delay(500)
            }
        }
    }

    private fun publish(player: Player) {
        val metadata = player.currentMediaItem?.mediaMetadata
        _state.value = PlayerUiState(
            title = metadata?.title?.toString(),
            artist = metadata?.artist?.toString(),
            album = metadata?.albumTitle?.toString(),
            artworkUri = metadata?.artworkUri?.toString(),
            isPlaying = player.isPlaying,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.takeIf { it > 0 } ?: 0L,
            hasNext = player.hasNextMediaItem(),
            hasPrevious = player.hasPreviousMediaItem(),
        )
    }
}

data class PlayerUiState(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val artworkUri: String? = null,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
) {
    val hasMedia: Boolean get() = title != null
}
