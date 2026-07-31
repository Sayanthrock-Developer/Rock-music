package com.rockmusic.app.player

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class FavouriteStore(private val preferences: SharedPreferences) {

    constructor(context: Context) : this(
        EncryptedSharedPreferences.create(
            context,
            "rock_music_favourites",
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
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
