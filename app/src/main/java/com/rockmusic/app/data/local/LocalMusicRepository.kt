package com.rockmusic.app.data.local

import android.content.ContentUris
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import com.rockmusic.app.domain.media.PlayableAudio
import com.rockmusic.app.domain.model.LocalTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.abs

interface LocalMusicRepository {
    suspend fun scan(): List<LocalTrack>

    suspend fun resolve(uri: Uri): LocalTrack
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
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.IS_MUSIC,
        )
        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
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
                val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
                val isMusicColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC)

                while (cursor.moveToNext()) {
                    val mimeType = cursor.getString(mimeColumn)
                    val displayName = cursor.getString(displayNameColumn).orEmpty()
                    val isMusic = cursor.getInt(isMusicColumn) != 0
                    if (!isMusic && !PlayableAudio.isSupported(mimeType, displayName)) continue

                    val id = cursor.getLong(idColumn)
                    val albumId = cursor.getLong(albumIdColumn)
                    add(
                        LocalTrack(
                            id = id,
                            title = cursor.getString(titleColumn)
                                .orEmpty()
                                .ifBlank { displayName.substringBeforeLast('.').ifBlank { "Unknown title" } },
                            artist = cursor.getString(artistColumn).orEmpty().ifBlank { "Unknown artist" },
                            album = cursor.getString(albumColumn).orEmpty().ifBlank { "Downloads" },
                            durationMs = cursor.getLong(durationColumn),
                            mediaUri = ContentUris.withAppendedId(collection, id).toString(),
                            artworkUri = albumId.takeIf { it > 0 }
                                ?.let { "content://media/external/audio/albumart/$it" },
                            mimeType = mimeType,
                            sizeBytes = cursor.getLong(sizeColumn),
                        ),
                    )
                }
            }
        }
    }

    override suspend fun resolve(uri: Uri): LocalTrack = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val hasPersistedReadAccess = resolver.persistedUriPermissions.any { permission ->
            permission.uri == uri && permission.isReadPermission
        }
        if (!hasPersistedReadAccess) {
            Log.w(
                TAG,
                "Persistent read access was not granted for $uri; the file may need to be selected again after restart.",
            )
        }

        resolver.openAssetFileDescriptor(uri, "r")?.use { descriptor ->
            val length = descriptor.length
            require(length == AssetFileDescriptor.UNKNOWN_LENGTH || length > 0L) {
                "The selected audio file is empty."
            }
        } ?: error("The selected audio file cannot be opened.")

        var displayName = "Downloaded audio"
        var sizeBytes = 0L
        resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) displayName = cursor.getString(nameIndex).orEmpty().ifBlank { displayName }
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) sizeBytes = cursor.getLong(sizeIndex)
            }
        }

        val mimeType = resolver.getType(uri)
        require(PlayableAudio.isSupported(mimeType, displayName)) {
            "The selected file is not a supported audio file."
        }

        val metadata = MediaMetadataRetriever()
        try {
            metadata.setDataSource(context, uri)
            val title = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                .orEmpty()
                .ifBlank { displayName.substringBeforeLast('.').ifBlank { "Downloaded audio" } }
            val artist = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                .orEmpty()
                .ifBlank { "Unknown artist" }
            val album = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                .orEmpty()
                .ifBlank { "Downloads" }
            val durationMs = metadata.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull()
                ?: 0L
            val stableId = uri.toString().hashCode().toLong().let { hash ->
                if (hash == 0L) -1L else -abs(hash)
            }

            LocalTrack(
                id = stableId,
                title = title,
                artist = artist,
                album = album,
                durationMs = durationMs,
                mediaUri = uri.toString(),
                artworkUri = null,
                mimeType = mimeType,
                sizeBytes = sizeBytes,
            )
        } finally {
            metadata.release()
        }
    }

    private companion object {
        const val TAG = "LocalMusicRepository"
        const val MIN_TRACK_DURATION_MS = 1_000L
    }
}
