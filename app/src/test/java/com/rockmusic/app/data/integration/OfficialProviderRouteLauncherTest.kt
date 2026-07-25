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
    fun `rejects non HTTPS and non YouTube web fallbacks`() {
        val insecure = OfficialProviderRoute(
            webUri = "http://music.youtube.com/search?q=rock",
            androidAppUri = null,
            preferredPackages = emptyList(),
            kind = OfficialRouteKind.SEARCH,
        )
        val hostile = insecure.copy(webUri = "https://attacker.example/search?q=rock")

        assertTrue(runCatching { OfficialRouteLaunchPlanner.targets(insecure) }.isFailure)
        assertTrue(runCatching { OfficialRouteLaunchPlanner.targets(hostile) }.isFailure)
    }

    @Test
    fun `rejects unsupported Android packages and malformed app ids`() {
        val unsupportedPackage = OfficialProviderRoute(
            webUri = "https://www.youtube.com/watch?v=abc123",
            androidAppUri = "vnd.youtube:abc123",
            preferredPackages = listOf("com.attacker.player"),
            kind = OfficialRouteKind.VIDEO,
        )
        val malformedAppUri = unsupportedPackage.copy(
            preferredPackages = listOf("com.google.android.youtube"),
            androidAppUri = "vnd.youtube:not valid",
        )

        assertTrue(runCatching { OfficialRouteLaunchPlanner.targets(unsupportedPackage) }.isFailure)
        assertTrue(runCatching { OfficialRouteLaunchPlanner.targets(malformedAppUri) }.isFailure)
    }
}
