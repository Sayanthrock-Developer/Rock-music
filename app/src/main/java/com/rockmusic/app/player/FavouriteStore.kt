package com.rockmusic.app.player

import android.content.Context

class FavouriteStore(context: Context) {
    private val preferences = context.getSharedPreferences(
        "rock_music_favourites",
        Context.MODE_PRIVATE,
    )

    fun isFavourite(mediaId: String): Boolean =
        mediaId.isNotBlank() && preferences.getBoolean(mediaId, false)

    fun toggle(mediaId: String): Boolean {
        if (mediaId.isBlank()) return false
        val updated = !isFavourite(mediaId)
        preferences.edit().putBoolean(mediaId, updated).apply()
        return updated
    }
}
