package com.rockmusic.app.domain.media

object PlayableAudio {
    private val supportedExtensions = setOf(
        ".mp3",
        ".m4a",
        ".aac",
        ".flac",
        ".ogg",
        ".opus",
        ".wav",
        ".amr",
        ".3gp",
    )

    fun isSupported(mimeType: String?, displayName: String): Boolean =
        mimeType?.startsWith("audio/", ignoreCase = true) == true ||
            supportedExtensions.any { displayName.endsWith(it, ignoreCase = true) }
}
