package com.rockmusic.app.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelsTest {
    @Test
    fun lyricLine_keepsWordTimingForWordByWordHighlighting() {
        val line = LyricLine(
            startMs = 1_000,
            endMs = 3_000,
            text = "Rock Music",
            words = listOf(
                LyricWord("Rock", 1_000, 1_800),
                LyricWord("Music", 1_900, 3_000),
            ),
        )

        assertEquals(2, line.words.size)
        assertTrue(line.words.zipWithNext().all { (first, second) -> first.endMs <= second.startMs })
    }

    @Test
    fun localTrack_retainsProviderIndependentMediaUri() {
        val track = LocalTrack(7, "Title", "Artist", "Album", 120_000, "content://media/7", null, "audio/flac", 1024)
        assertEquals("content://media/7", track.mediaUri)
    }
}
