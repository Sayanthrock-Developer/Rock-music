package com.rockmusic.app.player

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class EmbeddedLyricsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun read(mediaUri: String): Result<String?> = withContext(Dispatchers.IO) {
        runCatching {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return@runCatching null
            readApi30(mediaUri)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun readApi30(mediaUri: String): String? = MediaMetadataRetriever().use { retriever ->
        retriever.setDataSource(context, Uri.parse(mediaUri))
        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LYRIC)
            ?.trim()
            ?.takeIf(String::isNotBlank)
    }
}
