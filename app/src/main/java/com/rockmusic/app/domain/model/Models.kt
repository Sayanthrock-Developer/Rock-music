package com.rockmusic.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LocalTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val mediaUri: String,
    val artworkUri: String?,
    val mimeType: String?,
    val sizeBytes: Long,
)

@Serializable
data class LyricWord(
    val text: String,
    val startMs: Long,
    val endMs: Long,
)

@Serializable
data class LyricLine(
    val startMs: Long,
    val endMs: Long?,
    val text: String,
    val translation: String? = null,
    val words: List<LyricWord> = emptyList(),
)

enum class MediaSourceType {
    LOCAL,
    PODCAST,
    USER_CLOUD,
    LICENSED,
}
