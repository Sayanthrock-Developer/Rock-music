package com.rockmusic.app.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerUiStateTest {

    @Test
    fun `hasMedia returns false when title is null`() {
        val state = PlayerUiState(title = null)
        assertFalse(state.hasMedia)
    }

    @Test
    fun `hasMedia returns true when title is not null`() {
        val state = PlayerUiState(title = "Some Title")
        assertTrue(state.hasMedia)
    }
}
