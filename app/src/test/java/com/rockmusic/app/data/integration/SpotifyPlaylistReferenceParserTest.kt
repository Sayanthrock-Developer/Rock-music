package com.rockmusic.app.data.integration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyPlaylistReferenceParserTest {
    @Test
    fun `accepts shared web link and removes tracking parameters`() {
        val result = SpotifyPlaylistReferenceParser.parse(
            "https://open.spotify.com/playlist/25Y5z4jvx8H5UHUFxSY95g?si=ckqn6D--T2iFW9QXSORsVg&utm_source=copy-link",
        )

        assertEquals("25Y5z4jvx8H5UHUFxSY95g", result.getOrThrow())
        assertEquals(
            "https://open.spotify.com/playlist/25Y5z4jvx8H5UHUFxSY95g",
            SpotifyPlaylistReferenceParser.canonicalWebUrl(result.getOrThrow()),
        )
    }

    @Test
    fun `accepts Spotify playlist URI`() {
        val result = SpotifyPlaylistReferenceParser.parse(
            "spotify:playlist:25Y5z4jvx8H5UHUFxSY95g",
        )

        assertEquals("25Y5z4jvx8H5UHUFxSY95g", result.getOrThrow())
    }

    @Test
    fun `rejects non playlist and non Spotify links`() {
        assertTrue(
            SpotifyPlaylistReferenceParser.parse(
                "https://open.spotify.com/track/25Y5z4jvx8H5UHUFxSY95g",
            ).isFailure,
        )
        assertTrue(
            SpotifyPlaylistReferenceParser.parse(
                "https://example.com/playlist/25Y5z4jvx8H5UHUFxSY95g",
            ).isFailure,
        )
        assertTrue(
            SpotifyPlaylistReferenceParser.parse(
                "http://open.spotify.com/playlist/25Y5z4jvx8H5UHUFxSY95g",
            ).isFailure,
        )
    }

    @Test
    fun `rejects malformed playlist ids`() {
        assertTrue(
            SpotifyPlaylistReferenceParser.parse(
                "spotify:playlist:not-a-valid-id",
            ).isFailure,
        )
    }
}
