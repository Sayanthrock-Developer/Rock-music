package com.rockmusic.app.presentation

import com.rockmusic.app.domain.model.LocalTrack

enum class UnifiedHomeSource(val label: String) {
    ALL("All"),
    SONGS("Songs"),
    YOUTUBE("YouTube Music"),
}

object UnifiedHomeLibrary {
    fun localTracks(
        tracks: List<LocalTrack>,
        query: String,
        source: UnifiedHomeSource,
    ): List<LocalTrack> {
        if (source == UnifiedHomeSource.YOUTUBE) return emptyList()
        val cleanedQuery = query.trim()
        return tracks
            .distinctBy(LocalTrack::mediaUri)
            .filter { track ->
                cleanedQuery.isBlank() ||
                    track.title.contains(cleanedQuery, ignoreCase = true) ||
                    track.artist.contains(cleanedQuery, ignoreCase = true) ||
                    track.album.contains(cleanedQuery, ignoreCase = true)
            }
    }

    fun featuredTracks(tracks: List<LocalTrack>, limit: Int = 5): List<LocalTrack> = tracks
        // Optimization: Use asSequence() for lazy evaluation to prevent distinctBy from processing the entire list
        .asSequence()
        .distinctBy(LocalTrack::mediaUri)
        .take(limit.coerceAtLeast(0))
        .toList()

    fun speedDialRows(
        tracks: List<LocalTrack>,
        columns: Int = 3,
    ): List<List<LocalTrack>> = tracks
        .distinctBy(LocalTrack::mediaUri)
        .chunked(columns.coerceAtLeast(1))
}
