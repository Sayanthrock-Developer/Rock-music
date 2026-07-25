package com.rockmusic.app.player

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
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
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, Uri.parse(mediaUri))
                retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_LYRIC)
                    ?.trim()
                    ?.takeIf(String::isNotBlank)
            }
        }
    }
}
