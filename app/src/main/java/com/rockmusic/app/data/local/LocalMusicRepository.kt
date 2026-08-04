package com.rockmusic.app.data.local

import android.content.ContentUris
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import com.rockmusic.app.domain.media.PlayableAudio
import com.rockmusic.app.domain.model.LocalTrack
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.ArrayDeque
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

interface LocalMusicRepository {
    suspend fun scan(): List<LocalTrack>

    suspend fun folders(): List<LocalMusicFolder>

    suspend fun resolve(uri: Uri): LocalTrack

    suspend fun resolveAll(uris: List<Uri>): List<LocalTrack>

    suspend fun scanFolder(treeUri: Uri): List<LocalTrack>
}

class MediaStoreLocalMusicRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val folderExclusionStore: FolderExclusionStore,
) : LocalMusicRepository {

    override suspend fun scan(): List<LocalTrack> = withContext(Dispatchers.IO) {
        val excludedFolderIds = folderExclusionStore.excludedFolderIds.first()
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
            MediaStore.Audio.Media.RELATIVE_PATH,
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
                val relativePathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)

                while (cursor.moveToNext()) {
                    val mimeType = cursor.getString(mimeColumn)
                    val displayName = cursor.getString(displayNameColumn).orEmpty()
                    val isMusic = cursor.getInt(isMusicColumn) != 0
                    if (!isMusic && !PlayableAudio.isSupported(mimeType, displayName)) continue

                    val relativePath = cursor.getString(relativePathColumn)
                    if (LocalMusicFolderIdentity.id(relativePath) in excludedFolderIds) continue

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

    override suspend fun folders(): List<LocalMusicFolder> = withContext(Dispatchers.IO) {
        val projection = arrayOf(
            MediaStore.Audio.Media.RELATIVE_PATH,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.MIME_TYPE,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DISPLAY_NAME,
            MediaStore.Audio.Media.IS_MUSIC,
        )
        val selection = "${MediaStore.Audio.Media.DURATION} >= ?"
        val selectionArgs = arrayOf(MIN_TRACK_DURATION_MS.toString())
        val folders = linkedMapOf<String, FolderAccumulator>()

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.Audio.Media.RELATIVE_PATH} ASC",
        )?.use { cursor ->
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.RELATIVE_PATH)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
            val displayNameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
            val isMusicColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.IS_MUSIC)

            while (cursor.moveToNext()) {
                val mimeType = cursor.getString(mimeColumn)
                val displayName = cursor.getString(displayNameColumn).orEmpty()
                val isMusic = cursor.getInt(isMusicColumn) != 0
                if (!isMusic && !PlayableAudio.isSupported(mimeType, displayName)) continue

                val relativePath = cursor.getString(pathColumn)
                val folderId = LocalMusicFolderIdentity.id(relativePath)
                val accumulator = folders.getOrPut(folderId) {
                    FolderAccumulator(
                        id = folderId,
                        displayPath = LocalMusicFolderIdentity.displayPath(relativePath),
                        displayName = LocalMusicFolderIdentity.displayName(relativePath),
                    )
                }
                accumulator.songCount += 1
                accumulator.totalBytes += cursor.getLong(sizeColumn).coerceAtLeast(0L)
            }
        }

        folders.values
            .map(FolderAccumulator::toFolder)
            .sortedWith(
                compareBy<LocalMusicFolder, String>(String.CASE_INSENSITIVE_ORDER) { it.displayName }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.displayPath },
            )
    }

    override suspend fun resolve(uri: Uri): LocalTrack = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val hasPersistedReadAccess = resolver.persistedUriPermissions.any { permission ->
            permission.uri == uri && permission.isReadPermission
        }
        if (!hasPersistedReadAccess) {
            Log.w(
                TAG,
                "Persistent read access was not granted for the file; it may need to be selected again after restart.",
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

    override suspend fun resolveAll(uris: List<Uri>): List<LocalTrack> = withContext(Dispatchers.IO) {
        val uniqueUris = uris.distinctBy(Uri::toString)
        // Optimization: Concurrently resolve metadata for all provided URIs
        // to minimize sequential I/O bottlenecks.
        val tracks = uniqueUris.map { uri ->
            async {
                try {
                    resolve(uri)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    Log.w(TAG, "Skipping unreadable audio URI", error)
                    null
                }
            }
        }.awaitAll().filterNotNull()

        require(tracks.isNotEmpty()) {
            "No supported audio files could be opened."
        }
        tracks
    }

    override suspend fun scanFolder(treeUri: Uri): List<LocalTrack> = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val rootDocumentId = DocumentsContract.getTreeDocumentId(treeUri)
        val pendingFolders = ArrayDeque<String>()
        val visitedFolders = mutableSetOf<String>()
        val audioUris = mutableListOf<Uri>()
        pendingFolders.add(rootDocumentId)

        while (pendingFolders.isNotEmpty() && audioUris.size < MAX_FOLDER_TRACKS) {
            val parentDocumentId = pendingFolders.removeFirst()
            if (!visitedFolders.add(parentDocumentId)) continue

            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                parentDocumentId,
            )
            resolver.query(
                childrenUri,
                arrayOf(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                ),
                null,
                null,
                null,
            )?.use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                )
                val nameColumn = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                )
                val mimeColumn = cursor.getColumnIndexOrThrow(
                    DocumentsContract.Document.COLUMN_MIME_TYPE,
                )

                while (cursor.moveToNext() && audioUris.size < MAX_FOLDER_TRACKS) {
                    val documentId = cursor.getString(idColumn)
                    val displayName = cursor.getString(nameColumn).orEmpty()
                    val mimeType = cursor.getString(mimeColumn)

                    if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR) {
                        pendingFolders.add(documentId)
                    } else if (PlayableAudio.isSupported(mimeType, displayName)) {
                        audioUris += DocumentsContract.buildDocumentUriUsingTree(
                            treeUri,
                            documentId,
                        )
                    }
                }
            }
        }

        require(audioUris.isNotEmpty()) {
            "No supported audio files were found in the selected folder."
        }

        val tracks = try {
            resolveAll(audioUris)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            throw IllegalArgumentException(
                "The selected folder contains audio files, but none could be opened.",
                error,
            )
        }

        tracks.sortedWith(
            compareBy<LocalTrack, String>(String.CASE_INSENSITIVE_ORDER) { it.album }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title },
        )
    }

    private data class FolderAccumulator(
        val id: String,
        val displayPath: String,
        val displayName: String,
        var songCount: Int = 0,
        var totalBytes: Long = 0L,
    ) {
        fun toFolder() = LocalMusicFolder(
            id = id,
            displayPath = displayPath,
            displayName = displayName,
            songCount = songCount,
            totalBytes = totalBytes,
        )
    }

    private companion object {
        const val TAG = "LocalMusicRepository"
        const val MIN_TRACK_DURATION_MS = 1_000L
        const val MAX_FOLDER_TRACKS = 5_000
    }
}
