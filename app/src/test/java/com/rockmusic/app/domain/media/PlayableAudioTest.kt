package com.rockmusic.app.domain.media

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayableAudioTest {
    @Test
    fun `accepts audio mime types`() {
        assertTrue(PlayableAudio.isSupported("audio/mpeg", "download.bin"))
    }

    @Test
    fun `accepts supported extensions when provider mime type is generic`() {
        assertTrue(PlayableAudio.isSupported("application/octet-stream", "My Song.FLAC"))
        assertTrue(PlayableAudio.isSupported(null, "voice-note.opus"))
    }

    @Test
    fun `rejects non audio files`() {
        assertFalse(PlayableAudio.isSupported("application/pdf", "document.pdf"))
        assertFalse(PlayableAudio.isSupported(null, "archive.zip"))
    }
}
