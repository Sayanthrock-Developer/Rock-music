package com.rockmusic.app.data.local

import java.util.Locale

data class LocalMusicFolder(
    val id: String,
    val displayPath: String,
    val displayName: String,
    val songCount: Int,
    val totalBytes: Long,
)

object LocalMusicFolderIdentity {
    private const val ROOT_ID = "__root__"
    private const val ROOT_DISPLAY_NAME = "Internal storage"

    fun id(relativePath: String?): String {
        val normalized = normalizePath(relativePath)
        return if (normalized.isBlank()) ROOT_ID else normalized.lowercase(Locale.ROOT)
    }

    fun displayPath(relativePath: String?): String =
        normalizePath(relativePath).ifBlank { ROOT_DISPLAY_NAME }

    fun displayName(relativePath: String?): String {
        val normalized = normalizePath(relativePath)
        return if (normalized.isBlank()) {
            ROOT_DISPLAY_NAME
        } else {
            normalized.substringAfterLast('/')
        }
    }

    private fun normalizePath(relativePath: String?): String = relativePath
        .orEmpty()
        .replace('\\', '/')
        .split('/')
        .filter(String::isNotBlank)
        .joinToString("/")
}
