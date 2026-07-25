package com.rockmusic.app.data.integration

import com.rockmusic.app.domain.integration.OfficialProviderRoute
import com.rockmusic.app.domain.integration.OfficialRouteKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfficialProviderRouteLauncherTest {
    @Test
    fun `plans official packages before the web fallback`() {
        val route = OfficialProviderRoute(
            webUri = "https://music.youtube.com/watch?v=abc123",
            androidAppUri = "vnd.youtube:abc123",
            preferredPackages = listOf(
                "com.google.android.apps.youtube.music",
                "com.google.android.youtube",
                "com.google.android.youtube",
            ),
            kind = OfficialRouteKind.VIDEO,
            providerMediaId = "abc123",
        )

        assertEquals(
            listOf(
                OfficialRouteLaunchTarget(
                    uri = "vnd.youtube:abc123",
                    packageName = "com.google.android.apps.youtube.music",
                ),
                OfficialRouteLaunchTarget(
                    uri = "vnd.youtube:abc123",
                    packageName = "com.google.android.youtube",
                ),
                OfficialRouteLaunchTarget(
                    uri = "https://music.youtube.com/watch?v=abc123",
                    packageName = null,
                ),
            ),
            OfficialRouteLaunchPlanner.targets(route),
        )
    }

    @Test
    fun `uses the validated web route for package and browser search targets`() {
        val route = OfficialProviderRoute(
            webUri = "https://music.youtube.com/search?q=rock",
            androidAppUri = null,
            preferredPackages = listOf("com.google.android.apps.youtube.music"),
            kind = OfficialRouteKind.SEARCH,
        )

        val targets = OfficialRouteLaunchPlanner.targets(route)

        assertEquals("https://music.youtube.com/search?q=rock", targets.first().uri)
        assertEquals("com.google.android.apps.youtube.music", targets.first().packageName)
        assertEquals(null, targets.last().packageName)
    }

    @Test
    fun `rejects non HTTPS web fallbacks`() {
        val route = OfficialProviderRoute(
            webUri = "http://music.youtube.com/search?q=rock",
            androidAppUri = null,
            preferredPackages = emptyList(),
            kind = OfficialRouteKind.SEARCH,
        )

        assertTrue(runCatching { OfficialRouteLaunchPlanner.targets(route) }.isFailure)
    }
}
