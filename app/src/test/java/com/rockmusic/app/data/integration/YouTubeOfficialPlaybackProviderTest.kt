package com.rockmusic.app.data.integration

import com.rockmusic.app.domain.integration.ProviderCallResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeOfficialPlaybackProviderTest {
    private val provider = YouTubeOfficialPlaybackProvider()

    @Test
    fun `accepts official youtube music links`() {
        val result = provider.openTrack("https://music.youtube.com/watch?v=track123")

        assertTrue(result is ProviderCallResult.Success)
        assertEquals(
            "https://music.youtube.com/watch?v=track123",
            (result as ProviderCallResult.Success).value,
        )
    }

    @Test
    fun `rejects non official hosts`() {
        val result = provider.openTrack("https://example.com/watch?v=track123")

        assertTrue(result is ProviderCallResult.Failure)
        assertEquals(false, (result as ProviderCallResult.Failure).retryable)
    }

    @Test
    fun `builds an official youtube music search link`() {
        val result = provider.openSearch("Daft Punk One More Time")

        assertTrue(result is ProviderCallResult.Success)
        assertTrue(
            (result as ProviderCallResult.Success).value
                .startsWith("https://music.youtube.com/search?q="),
        )
    }
}
