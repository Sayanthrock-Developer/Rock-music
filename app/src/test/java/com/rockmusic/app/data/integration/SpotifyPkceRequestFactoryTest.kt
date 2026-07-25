package com.rockmusic.app.data.integration

import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyPkceRequestFactoryTest {
    @Test
    fun `creates an S256 request with bounded single use values`() {
        val factory = factory()
        val request = factory.create(
            scopes = setOf("playlist-read-private", "playlist-read-collaborative"),
            nowEpochMs = 1_000L,
        ).getOrThrow()

        assertTrue(request.codeVerifier.length in 43..128)
        assertTrue(request.state.length >= 32)
        assertEquals(601_000L, request.expiresAtEpochMs)
        assertEquals(
            Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256")
                    .digest(request.codeVerifier.toByteArray(StandardCharsets.US_ASCII)),
            ),
            request.codeChallenge,
        )

        val query = parseQuery(URI(request.authorizationUri).rawQuery)
        assertEquals("S256", query["code_challenge_method"])
        assertEquals(request.codeChallenge, query["code_challenge"])
        assertEquals(request.state, query["state"])
        assertEquals("rockmusic://oauth/spotify", query["redirect_uri"])
    }

    @Test
    fun `generates different verifier and state for every request`() {
        val factory = factory()
        val first = factory.create(setOf("playlist-read-private"), 1_000L).getOrThrow()
        val second = factory.create(setOf("playlist-read-private"), 1_000L).getOrThrow()

        assertNotEquals(first.codeVerifier, second.codeVerifier)
        assertNotEquals(first.state, second.state)
    }

    @Test
    fun `accepts only callback with matching target and state`() {
        val factory = factory()
        val request = factory.create(setOf("playlist-read-private"), 1_000L).getOrThrow()

        val result = factory.validateCallback(
            callbackUri = "rockmusic://oauth/spotify?code=abc123&state=${request.state}",
            pendingRequest = request,
            nowEpochMs = 2_000L,
        )

        val authorized = result as SpotifyAuthorizationCallbackResult.Authorized
        assertEquals("abc123", authorized.authorizationCode)
    }

    @Test
    fun `rejects callback state mismatch and expiry`() {
        val factory = factory()
        val request = factory.create(setOf("playlist-read-private"), 1_000L).getOrThrow()

        assertTrue(
            factory.validateCallback(
                "rockmusic://oauth/spotify?code=abc123&state=wrong",
                request,
                2_000L,
            ) is SpotifyAuthorizationCallbackResult.Rejected,
        )
        assertTrue(
            factory.validateCallback(
                "rockmusic://oauth/spotify?code=abc123&state=${request.state}",
                request,
                request.expiresAtEpochMs,
            ) is SpotifyAuthorizationCallbackResult.Rejected,
        )
    }

    @Test
    fun `rejects insecure redirect and unused scopes`() {
        val insecure = factory(redirectUri = "http://example.com/callback")
        assertTrue(insecure.create(setOf("playlist-read-private")).isFailure)

        val factory = factory()
        assertTrue(factory.create(setOf("user-modify-playback-state")).isFailure)
    }

    private fun factory(
        clientId: String = "0123456789abcdef0123456789abcdef",
        redirectUri: String = "rockmusic://oauth/spotify",
    ): SpotifyPkceRequestFactory = SpotifyPkceRequestFactory(
        configuration = FakeConfiguration(
            mapOf(
                ProviderConfigKey.SPOTIFY_CLIENT_ID to clientId,
                ProviderConfigKey.SPOTIFY_REDIRECT_URI to redirectUri,
            ),
        ),
        secureRandom = SecureRandom(),
    )

    private fun parseQuery(rawQuery: String): Map<String, String> = rawQuery.split('&')
        .associate { pair ->
            val (key, value) = pair.split('=', limit = 2)
            URLDecoder.decode(key, StandardCharsets.UTF_8.name()) to
                URLDecoder.decode(value, StandardCharsets.UTF_8.name())
        }

    private class FakeConfiguration(
        private val values: Map<ProviderConfigKey, String>,
    ) : ProviderConfigurationSource {
        override fun value(key: ProviderConfigKey): String = values[key].orEmpty()
    }
}
