package com.rockmusic.app.presentation

import com.rockmusic.app.domain.model.LocalTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnifiedHomeLibraryTest {
    private val tracks = listOf(
        track(1, "Trickin'", "DaBaby", "Imported", "content://one"),
        track(2, "Jump Around", "Vanilla Ice", "Downloads", "content://two"),
        track(3, "Like You", "Naeil", "Speed Dial", "content://three"),
        track(4, "Trickin' duplicate", "DaBaby", "Imported", "content://one"),
    )

    @Test
    fun `all imported tracks stay on Home without duplicates`() {
        val visible = UnifiedHomeLibrary.localTracks(
            tracks = tracks,
            query = "",
            source = UnifiedHomeSource.ALL,
        )

        assertEquals(listOf("Trickin'", "Jump Around", "Like You"), visible.map(LocalTrack::title))
    }

    @Test
    fun `search matches title artist and album`() {
        assertEquals(
            listOf("Trickin'"),
            UnifiedHomeLibrary.localTracks(tracks, "dababy", UnifiedHomeSource.SONGS)
                .map(LocalTrack::title),
        )
        assertEquals(
            listOf("Jump Around"),
            UnifiedHomeLibrary.localTracks(tracks, "downloads", UnifiedHomeSource.ALL)
                .map(LocalTrack::title),
        )
    }

    @Test
    fun `YouTube source hides local results but keeps the local library unchanged`() {
        assertTrue(
            UnifiedHomeLibrary.localTracks(tracks, "", UnifiedHomeSource.YOUTUBE).isEmpty(),
        )
        assertEquals(3, UnifiedHomeLibrary.featuredTracks(tracks, limit = 3).size)
        assertEquals(2, UnifiedHomeLibrary.speedDialRows(tracks, columns = 2).size)
    }

    private fun track(
        id: Long,
        title: String,
        artist: String,
        album: String,
        mediaUri: String,
    ) = LocalTrack(
        id = id,
        title = title,
        artist = artist,
        album = album,
        durationMs = 120_000L,
        mediaUri = mediaUri,
        artworkUri = null,
        mimeType = "audio/mpeg",
        sizeBytes = 1_000L,
    )
}
