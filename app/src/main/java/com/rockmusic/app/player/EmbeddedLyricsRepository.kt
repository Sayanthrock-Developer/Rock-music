package com.rockmusic.app.player

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.Reader
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Singleton
class EmbeddedLyricsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun read(mediaUri: String): Result<String?> = withContext(Dispatchers.IO) {
        runCatching {
            val uri = Uri.parse(mediaUri)
            when (uri.scheme?.lowercase()) {
                "file" -> readFileSidecar(uri)
                "content" -> readMediaStoreSidecar(uri)
                else -> null
            }
        }
    }

    private fun readFileSidecar(uri: Uri): String? {
        val audioFile = uri.path?.let(::File) ?: return null
        val baseName = audioFile.nameWithoutExtension
        val parent = audioFile.parentFile ?: return null
        return sidecarNames(baseName)
            .asSequence()
            .map { File(parent, it) }
            .firstOrNull(File::isFile)
            ?.reader(Charsets.UTF_8)
            ?.use(::readLimitedText)
    }

    private fun readMediaStoreSidecar(audioUri: Uri): String? {
        val metadata = runCatching {
            context.contentResolver.query(
                audioUri,
                arrayOf(
                    MediaStore.MediaColumns.DISPLAY_NAME,
                    MediaStore.MediaColumns.RELATIVE_PATH,
                ),
                null,
                null,
                null,
            )?.use { cursor ->
                if (!cursor.moveToFirst()) return@use null
                val displayName = cursor.getString(0) ?: return@use null
                val relativePath = cursor.getString(1) ?: return@use null
                displayName to relativePath
            }
        }.getOrNull() ?: return null

        val baseName = metadata.first.substringBeforeLast('.', metadata.first)
        val names = sidecarNames(baseName)
        val filesUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val selection =
            "${MediaStore.MediaColumns.RELATIVE_PATH}=? AND " +
                "${MediaStore.MediaColumns.DISPLAY_NAME}=?"

        return names.asSequence()
            .mapNotNull { name ->
                context.contentResolver.query(
                    filesUri,
                    arrayOf(MediaStore.MediaColumns._ID),
                    selection,
                    arrayOf(metadata.second, name),
                    null,
                )?.use { cursor ->
                    if (!cursor.moveToFirst()) return@use null
                    val sidecarUri = ContentUris.withAppendedId(filesUri, cursor.getLong(0))
                    context.contentResolver.openInputStream(sidecarUri)
                        ?.bufferedReader(Charsets.UTF_8)
                        ?.use(::readLimitedText)
                }
            }
            .firstOrNull()
    }

    private fun sidecarNames(baseName: String): List<String> = listOf(
        "$baseName.lrc",
        "$baseName.txt",
    )

    private fun readLimitedText(reader: Reader): String? {
        val output = StringBuilder()
        val buffer = CharArray(4_096)
        while (output.length < MAX_LYRICS_CHARS) {
            val count = reader.read(
                buffer,
                0,
                min(buffer.size, MAX_LYRICS_CHARS - output.length),
            )
            if (count < 0) break
            output.append(buffer, 0, count)
        }
        return output.toString().trim().takeIf(String::isNotBlank)
    }

    private companion object {
        const val MAX_LYRICS_CHARS = 500_000
    }
}
