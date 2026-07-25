package com.rockmusic.app.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemMediaStateTest {
    @Test
    fun `system media custom action constants are stable and distinct`() {
        assertEquals(
            "com.rockmusic.app.action.TOGGLE_FAVOURITE",
            MediaSessionCommands.ACTION_TOGGLE_FAVOURITE,
        )
        assertEquals(
            "com.rockmusic.app.action.OPEN_LYRICS",
            MediaSessionCommands.ACTION_OPEN_LYRICS,
        )
        assertNotEquals(
            MediaSessionCommands.ACTION_TOGGLE_FAVOURITE,
            MediaSessionCommands.ACTION_OPEN_LYRICS,
        )
    }

    @Test
    fun `player state exposes favourite volume and queue`() {
        val state = PlayerUiState(
            title = "Track",
            isFavourite = true,
            volume = 0.45f,
            queue = listOf(
                PlayerQueueItem(
                    mediaId = "local:1",
                    title = "Track",
                    artist = "Artist",
                    artworkUri = null,
                ),
            ),
            currentQueueIndex = 0,
        )

        assertTrue(state.hasMedia)
        assertTrue(state.isFavourite)
        assertEquals(0.45f, state.volume)
        assertEquals("local:1", state.queue.single().mediaId)
    }

    @Test
    fun `empty player state has no media`() {
        assertFalse(PlayerUiState().hasMedia)
    }
}
