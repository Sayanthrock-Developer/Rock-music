package com.rockmusic.app.data.local

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.rockmusic.app.domain.model.LocalTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface LocalMusicRepository {
    suspend fun scan(): List<LocalTrack>
}

class MediaStoreLocalMusicRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocalMusicRepository {

    override suspend fun scan(): List<LocalTrack> = withContext(Dispatchers.IO) {
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(MIN_TRACK_DURATION_MS.toString())

        buildList {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                "${MediaStore.Audio.Media.DATE_ADDED} DESC",
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
                val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val albumId = cursor.getLong(albumIdColumn)
                    add(
                        LocalTrack(
                            id = id,
                            title = cursor.getString(titleColumn).orEmpty().ifBlank { "Unknown title" },
                            artist = cursor.getString(artistColumn).orEmpty().ifBlank { "Unknown artist" },
                            album = cursor.getString(albumColumn).orEmpty().ifBlank { "Unknown album" },
                            durationMs = cursor.getLong(durationColumn),
                            mediaUri = ContentUris.withAppendedId(collection, id).toString(),
                            artworkUri = "content://media/external/audio/albumart/$albumId",
                            mimeType = cursor.getString(mimeColumn),
                            sizeBytes = cursor.getLong(sizeColumn),
                        ),
                    )
                }
            }
        }
    }

    private companion object {
        const val MIN_TRACK_DURATION_MS = 10_000L
    }
}
