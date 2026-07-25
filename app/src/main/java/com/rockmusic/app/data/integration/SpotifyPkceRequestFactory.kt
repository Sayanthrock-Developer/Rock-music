package com.rockmusic.app.data.integration

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.inject.Inject
import javax.inject.Singleton

data class SpotifyPkceRequest(
    val authorizationUri: String,
    val codeVerifier: String,
    val state: String,
)

@Singleton
class SpotifyPkceRequestFactory @Inject constructor(
    private val configuration: BuildConfigProviderConfigurationSource,
) {
    private val secureRandom = SecureRandom()

    fun create(scopes: Set<String>): Result<SpotifyPkceRequest> = runCatching {
        val clientId = configuration.value(ProviderConfigKey.SPOTIFY_CLIENT_ID)
        val redirectUri = configuration.value(ProviderConfigKey.SPOTIFY_REDIRECT_URI)
        require(clientId.isNotBlank()) { "ROCK_SPOTIFY_CLIENT_ID is not configured" }
        require(redirectUri.isNotBlank()) { "ROCK_SPOTIFY_REDIRECT_URI is not configured" }

        val verifier = randomUrlSafeValue(64)
        val challenge = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                MessageDigest.getInstance("SHA-256")
                    .digest(verifier.toByteArray(StandardCharsets.US_ASCII)),
            )
        val state = randomUrlSafeValue(32)
        val scope = scopes.sorted().joinToString(" ")

        val query = linkedMapOf(
            "client_id" to clientId,
            "response_type" to "code",
            "redirect_uri" to redirectUri,
            "code_challenge_method" to "S256",
            "code_challenge" to challenge,
            "state" to state,
            "scope" to scope,
        ).entries.joinToString("&") { (key, value) ->
            "${key.encode()}=${value.encode()}"
        }

        SpotifyPkceRequest(
            authorizationUri = "https://accounts.spotify.com/authorize?$query",
            codeVerifier = verifier,
            state = state,
        )
    }

    private fun randomUrlSafeValue(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun String.encode(): String =
        URLEncoder.encode(this, StandardCharsets.UTF_8.name())
}
