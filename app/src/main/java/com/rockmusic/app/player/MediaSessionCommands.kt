package com.rockmusic.app.player

import android.os.Bundle
import androidx.media3.session.SessionCommand

object MediaSessionCommands {
    const val ACTION_TOGGLE_FAVOURITE = "com.rockmusic.app.action.TOGGLE_FAVOURITE"
    const val ACTION_OPEN_LYRICS = "com.rockmusic.app.action.OPEN_LYRICS"
    const val EXTRA_OPEN_PLAYER_SURFACE = "com.rockmusic.app.extra.OPEN_PLAYER_SURFACE"
    const val SURFACE_LYRICS = "lyrics"
    const val METADATA_IS_FAVOURITE = "com.rockmusic.app.metadata.IS_FAVOURITE"

    val toggleFavourite = SessionCommand(ACTION_TOGGLE_FAVOURITE, Bundle.EMPTY)
    val openLyrics = SessionCommand(ACTION_OPEN_LYRICS, Bundle.EMPTY)
}
