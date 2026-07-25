package com.rockmusic.app.player

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ConnectionResult
import androidx.media3.session.MediaSession.ConnectionResult.AcceptedResultBuilder
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.rockmusic.app.MainActivity

@OptIn(UnstableApi::class)
class MusicService : MediaSessionService() {
    private lateinit var player: ExoPlayer
    private lateinit var favouriteStore: FavouriteStore
    private var mediaSession: MediaSession? = null

    private val playerListener = object : Player.Listener {
        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            syncFavouriteMetadata(mediaItem)
        }
    }

    override fun onCreate() {
        super.onCreate()
        favouriteStore = FavouriteStore(this)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()
            .also { it.addListener(playerListener) }

        val sessionActivity = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivity)
            .setCallback(SessionCallback())
            .setMediaButtonPreferences(mediaButtons(isFavourite = false))
            .build()
    }

    override fun onGetSession(
        controllerInfo: MediaSession.ControllerInfo,
    ): MediaSession? = mediaSession

    override fun onDestroy() {
        player.removeListener(playerListener)
        mediaSession?.release()
        mediaSession = null
        player.release()
        super.onDestroy()
    }

    private inner class SessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ConnectionResult {
            val sessionCommands = if (controller.isTrusted) {
                ConnectionResult.DEFAULT_SESSION_COMMANDS.buildUpon()
                    .add(MediaSessionCommands.toggleFavourite)
                    .add(MediaSessionCommands.openLyrics)
                    .build()
            } else {
                SessionCommands.EMPTY
            }
            return AcceptedResultBuilder(session)
                .setAvailableSessionCommands(sessionCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle,
        ): ListenableFuture<SessionResult> {
            if (!controller.isTrusted) {
                return Futures.immediateFuture(
                    SessionResult(SessionResult.RESULT_ERROR_PERMISSION_DENIED),
                )
            }
            return when (customCommand.customAction) {
                MediaSessionCommands.ACTION_TOGGLE_FAVOURITE -> {
                    val current = player.currentMediaItem
                    if (current == null) {
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_BAD_VALUE))
                    } else {
                        favouriteStore.toggle(current.mediaId)
                        syncFavouriteMetadata(current)
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }
                }

                MediaSessionCommands.ACTION_OPEN_LYRICS -> {
                    startActivity(
                        Intent(this@MusicService, MainActivity::class.java).apply {
                            addFlags(
                                Intent.FLAG_ACTIVITY_NEW_TASK or
                                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
                            )
                            putExtra(
                                MediaSessionCommands.EXTRA_OPEN_PLAYER_SURFACE,
                                MediaSessionCommands.SURFACE_LYRICS,
                            )
                        },
                    )
                    Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }

                else -> super.onCustomCommand(session, controller, customCommand, args)
            }
        }
    }

    private fun syncFavouriteMetadata(mediaItem: MediaItem?) {
        val item = mediaItem ?: return
        val index = player.currentMediaItemIndex
        if (index !in 0 until player.mediaItemCount) return

        val isFavourite = favouriteStore.isFavourite(item.mediaId)
        val currentValue = item.mediaMetadata.extras
            ?.getBoolean(MediaSessionCommands.METADATA_IS_FAVOURITE, false)
            ?: false

        if (currentValue != isFavourite) {
            val extras = Bundle(item.mediaMetadata.extras ?: Bundle.EMPTY).apply {
                putBoolean(MediaSessionCommands.METADATA_IS_FAVOURITE, isFavourite)
            }
            val updated = item.buildUpon()
                .setMediaMetadata(
                    item.mediaMetadata.buildUpon()
                        .setExtras(extras)
                        .build(),
                )
                .build()
            player.replaceMediaItem(index, updated)
        }

        mediaSession?.setMediaButtonPreferences(mediaButtons(isFavourite))
    }

    private fun mediaButtons(isFavourite: Boolean): List<CommandButton> = listOf(
        CommandButton.Builder(
            if (isFavourite) CommandButton.ICON_HEART_FILLED else CommandButton.ICON_HEART_UNFILLED,
        )
            .setDisplayName(if (isFavourite) "Remove from favourites" else "Add to favourites")
            .setSessionCommand(MediaSessionCommands.toggleFavourite)
            .build(),
        CommandButton.Builder(CommandButton.ICON_SUBTITLES)
            .setDisplayName("Open lyrics")
            .setSessionCommand(MediaSessionCommands.openLyrics)
            .build(),
    )
}
