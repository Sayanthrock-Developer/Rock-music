package com.rockmusic.app.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.rockmusic.app.domain.model.LocalTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Singleton
class PlayerConnection @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val favouriteStore = FavouriteStore(context)
    private var controller: MediaController? = null
    private var pendingQueue: PendingQueue? = null
    private var pendingVolume: Float? = null

    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val listener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            publish(
                player,
                includeQueue = events.contains(Player.EVENT_TIMELINE_CHANGED) ||
                    events.contains(Player.EVENT_MEDIA_ITEM_TRANSITION),
            )
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
                        pendingVolume?.let { requestedVolume ->
                            mediaController.volume = requestedVolume
                            pendingVolume = null
                        }
                        val queue = pendingQueue
                        if (queue == null) {
                            publish(mediaController, includeQueue = true)
                        } else {
                            pendingQueue = null
                            startPlayback(mediaController, queue.tracks, queue.startIndex)
                        }
                        startPositionUpdates()
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
        playQueue(listOf(track))
    }

    fun playQueue(tracks: List<LocalTrack>, startIndex: Int = 0) {
        val playableTracks = tracks.distinctBy(LocalTrack::mediaUri)
        if (playableTracks.isEmpty()) return

        val safeStartIndex = startIndex.coerceIn(playableTracks.indices)
        val mediaController = controller
        if (mediaController == null) {
            pendingQueue = PendingQueue(playableTracks, safeStartIndex)
            publishPreparingTrack(playableTracks[safeStartIndex])
            return
        }
        startPlayback(mediaController, playableTracks, safeStartIndex)
    }

    fun togglePlayPause() {
        controller?.let { mediaController ->
            if (mediaController.isPlaying) mediaController.pause() else mediaController.play()
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

    fun playQueueIndex(index: Int) {
        controller?.let { mediaController ->
            if (index !in 0 until mediaController.mediaItemCount) return
            mediaController.seekToDefaultPosition(index)
            mediaController.play()
        }
    }

    fun setVolume(volume: Float) {
        val safeVolume = volume.coerceIn(0f, 1f)
        _state.value = _state.value.copy(volume = safeVolume)
        val mediaController = controller
        if (mediaController == null) {
            pendingVolume = safeVolume
        } else {
            mediaController.volume = safeVolume
        }
    }

    fun toggleFavourite() {
        controller?.sendCustomCommand(MediaSessionCommands.toggleFavourite, Bundle.EMPTY)
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    private fun startPlayback(
        mediaController: MediaController,
        tracks: List<LocalTrack>,
        startIndex: Int,
    ) {
        val items = tracks.map(::toMediaItem)
        publishPreparingTrack(tracks[startIndex])
        mediaController.setMediaItems(items, startIndex, C.TIME_UNSET)
        mediaController.prepare()
        mediaController.play()
    }

    private fun toMediaItem(track: LocalTrack): MediaItem {
        val mediaId = "local:${track.id}"
        val extras = Bundle().apply {
            putBoolean(
                MediaSessionCommands.METADATA_IS_FAVOURITE,
                favouriteStore.isFavourite(mediaId),
            )
        }
        val builder = MediaItem.Builder()
            .setUri(Uri.parse(track.mediaUri))
            .setMediaId(mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .setArtworkUri(track.artworkUri?.let(Uri::parse))
                    .setExtras(extras)
                    .build(),
            )
        track.mimeType
            ?.takeIf(String::isNotBlank)
            ?.let(builder::setMimeType)
        return builder.build()
    }

    private fun publishPreparingTrack(track: LocalTrack) {
        _state.value = _state.value.copy(
            title = track.title,
            artist = track.artist,
            album = track.album,
            mediaUri = track.mediaUri,
            artworkUri = track.artworkUri,
            isPreparing = true,
            errorMessage = null,
        )
    }

    private fun startPositionUpdates() {
        scope.launch {
            while (isActive) {
                controller?.let { publish(it, includeQueue = false) }
                delay(500)
            }
        }
    }

    private fun publish(player: Player, includeQueue: Boolean) {
        val currentState = _state.value
        val mediaItem = player.currentMediaItem
        if (mediaItem == null) {
            _state.value = PlayerUiState(
                volume = player.volume.coerceIn(0f, 1f),
                queue = if (includeQueue) queueSnapshot(player) else emptyList(),
                errorMessage = currentState.errorMessage,
            )
            return
        }

        val metadata = mediaItem.mediaMetadata
        _state.value = currentState.copy(
            title = metadata.title?.toString(),
            artist = metadata.artist?.toString(),
            album = metadata.albumTitle?.toString(),
            mediaUri = mediaItem.localConfiguration?.uri?.toString(),
            artworkUri = metadata.artworkUri?.toString(),
            isFavourite = metadata.extras
                ?.getBoolean(MediaSessionCommands.METADATA_IS_FAVOURITE, false)
                ?: false,
            isPlaying = player.isPlaying,
            isPreparing = player.playbackState == Player.STATE_BUFFERING,
            positionMs = player.currentPosition.coerceAtLeast(0L),
            durationMs = player.duration.takeIf { it > 0 } ?: 0L,
            hasNext = player.hasNextMediaItem(),
            hasPrevious = player.hasPreviousMediaItem(),
            volume = player.volume.coerceIn(0f, 1f),
            currentQueueIndex = player.currentMediaItemIndex.coerceAtLeast(0),
            queue = if (includeQueue) queueSnapshot(player) else currentState.queue,
            errorMessage = currentState.errorMessage,
        )
    }

    private fun queueSnapshot(player: Player): List<PlayerQueueItem> {
        val count = player.mediaItemCount
        if (count == 0) return emptyList()
        // ⚡ Bolt: Replaced manual loop and ArrayList with functional List constructor
        // for minor performance improvement and cleaner code.
        return List(count) { index ->
            val item = player.getMediaItemAt(index)
            val metadata = item.mediaMetadata
            PlayerQueueItem(
                mediaId = item.mediaId,
                title = metadata.title?.toString().orEmpty().ifBlank { "Unknown title" },
                artist = metadata.artist?.toString().orEmpty().ifBlank { "Unknown artist" },
                artworkUri = metadata.artworkUri?.toString(),
            )
        }
    }

    private data class PendingQueue(
        val tracks: List<LocalTrack>,
        val startIndex: Int,
    )
}

data class PlayerQueueItem(
    val mediaId: String,
    val title: String,
    val artist: String,
    val artworkUri: String?,
)

data class PlayerUiState(
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val mediaUri: String? = null,
    val artworkUri: String? = null,
    val isFavourite: Boolean = false,
    val isPlaying: Boolean = false,
    val isPreparing: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
    val volume: Float = 1f,
    val queue: List<PlayerQueueItem> = emptyList(),
    val currentQueueIndex: Int = 0,
    val errorMessage: String? = null,
) {
    val hasMedia: Boolean get() = title != null
}
