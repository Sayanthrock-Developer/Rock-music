package com.rockmusic.app.data.integration

import com.rockmusic.app.domain.integration.OfficialRouteKind
import com.rockmusic.app.domain.integration.ProviderCallResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeOfficialPlaybackProviderTest {
    private val provider = YouTubeOfficialPlaybackProvider()

    @Test
    fun `accepts and canonicalizes official youtube music links`() {
        val result = provider.routeTrack("https://music.youtube.com/watch?v=track123")
        val route = (result as ProviderCallResult.Success).value

        assertEquals("https://music.youtube.com/watch?v=track123", route.webUri)
        assertEquals("vnd.youtube:track123", route.androidAppUri)
        assertEquals(OfficialRouteKind.VIDEO, route.kind)
        assertEquals("com.google.android.apps.youtube.music", route.preferredPackages.first())
    }

    @Test
    fun `accepts official playlist route`() {
        val result = provider.routeTrack(
            "https://www.youtube.com/playlist?list=PL1234567890",
        )
        val route = (result as ProviderCallResult.Success).value

        assertEquals(OfficialRouteKind.PLAYLIST, route.kind)
        assertEquals("PL1234567890", route.providerMediaId)
    }

    @Test
    fun `rejects non official hosts and deceptive subdomains`() {
        assertTrue(
            provider.openTrack("https://example.com/watch?v=track123") is ProviderCallResult.Failure,
        )
        assertTrue(
            provider.openTrack("https://youtube.com.example.com/watch?v=track123") is ProviderCallResult.Failure,
        )
    }

    @Test
    fun `rejects unsupported official paths and duplicate identifiers`() {
        assertTrue(
            provider.openTrack("https://www.youtube.com/channel/example") is ProviderCallResult.Failure,
        )
        assertTrue(
            provider.openTrack(
                "https://www.youtube.com/watch?v=track123&v=track456",
            ) is ProviderCallResult.Failure,
        )
    }

    @Test
    fun `builds a validated official youtube music search route`() {
        val result = provider.routeSearch("Daft Punk One More Time")
        val route = (result as ProviderCallResult.Success).value

        assertEquals(OfficialRouteKind.SEARCH, route.kind)
        assertTrue(route.webUri.startsWith("https://music.youtube.com/search?q="))
        assertEquals("com.google.android.apps.youtube.music", route.preferredPackages.first())
    }

    @Test
    fun `rejects blank and control character search queries`() {
        assertTrue(provider.openSearch("   ") is ProviderCallResult.Failure)
        assertTrue(provider.openSearch("bad\u0000query") is ProviderCallResult.Failure)
    }
}
