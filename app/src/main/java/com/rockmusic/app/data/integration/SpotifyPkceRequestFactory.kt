package com.rockmusic.app.data.integration

import java.net.URI
import java.net.URLDecoder
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
    val codeChallenge: String,
    val state: String,
    val redirectUri: String,
    val scopes: Set<String>,
    val createdAtEpochMs: Long,
    val expiresAtEpochMs: Long,
)

sealed interface SpotifyAuthorizationCallbackResult {
    data class Authorized(
        val authorizationCode: String,
        val request: SpotifyPkceRequest,
    ) : SpotifyAuthorizationCallbackResult

    data class ProviderError(
        val error: String,
        val description: String?,
    ) : SpotifyAuthorizationCallbackResult

    data class Rejected(val reason: String) : SpotifyAuthorizationCallbackResult
}

interface SpotifyPkceRequestStore {
    suspend fun save(request: SpotifyPkceRequest)
    suspend fun consume(state: String): SpotifyPkceRequest?
    suspend fun clearExpired(nowEpochMs: Long)
}

@Singleton
class SpotifyPkceRequestFactory internal constructor(
    private val configuration: ProviderConfigurationSource,
    private val secureRandom: SecureRandom,
) {
    @Inject
    constructor(configuration: BuildConfigProviderConfigurationSource) : this(
        configuration = configuration,
        secureRandom = SecureRandom(),
    )

    fun create(
        scopes: Set<String>,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): Result<SpotifyPkceRequest> = runCatching {
        val clientId = configuration.value(ProviderConfigKey.SPOTIFY_CLIENT_ID)
        val redirectUri = configuration.value(ProviderConfigKey.SPOTIFY_REDIRECT_URI)
        validateClientId(clientId)
        validateRedirectUri(redirectUri)

        val normalizedScopes = scopes.map(String::trim).filter(String::isNotBlank).toSortedSet()
        require(normalizedScopes.isNotEmpty()) { "At least one Spotify scope is required" }
        require(normalizedScopes.size <= MAX_SCOPE_COUNT) { "Too many Spotify scopes were requested" }
        require(normalizedScopes.all(SPOTIFY_SCOPE_PATTERN::matches)) {
            "A Spotify scope contains unsupported characters"
        }
        require(normalizedScopes.all(SUPPORTED_SCOPES::contains)) {
            "The request contains a Spotify scope that Rock Music does not use"
        }

        val verifier = randomUrlSafeValue(64)
        require(verifier.length in 43..128 && verifier.all(PKCE_CHARACTER_SET::contains)) {
            "Unable to generate a valid PKCE verifier"
        }
        val challenge = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                MessageDigest.getInstance("SHA-256")
                    .digest(verifier.toByteArray(StandardCharsets.US_ASCII)),
            )
        val state = randomUrlSafeValue(32)
        val scope = normalizedScopes.joinToString(" ")

        val query = linkedMapOf(
            "client_id" to clientId,
            "response_type" to "code",
            "redirect_uri" to redirectUri,
            "code_challenge_method" to "S256",
            "code_challenge" to challenge,
            "state" to state,
            "scope" to scope,
            "show_dialog" to "false",
        ).entries.joinToString("&") { (key, value) ->
            "${key.encode()}=${value.encode()}"
        }

        SpotifyPkceRequest(
            authorizationUri = "$SPOTIFY_AUTHORIZE_ENDPOINT?$query",
            codeVerifier = verifier,
            codeChallenge = challenge,
            state = state,
            redirectUri = redirectUri,
            scopes = normalizedScopes,
            createdAtEpochMs = nowEpochMs,
            expiresAtEpochMs = nowEpochMs + REQUEST_LIFETIME_MS,
        )
    }

    fun validateCallback(
        callbackUri: String,
        pendingRequest: SpotifyPkceRequest,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): SpotifyAuthorizationCallbackResult {
        if (nowEpochMs >= pendingRequest.expiresAtEpochMs) {
            return SpotifyAuthorizationCallbackResult.Rejected("The Spotify authorization request expired")
        }

        val callback = runCatching { URI(callbackUri.trim()) }.getOrNull()
            ?: return SpotifyAuthorizationCallbackResult.Rejected("The Spotify callback URI is invalid")
        val redirect = runCatching { URI(pendingRequest.redirectUri) }.getOrNull()
            ?: return SpotifyAuthorizationCallbackResult.Rejected("The configured Spotify redirect URI is invalid")
        if (!sameRedirectTarget(callback, redirect)) {
            return SpotifyAuthorizationCallbackResult.Rejected("The Spotify callback target does not match the configured redirect URI")
        }

        val parameters = runCatching { parseQuery(callback.rawQuery) }.getOrElse {
            return SpotifyAuthorizationCallbackResult.Rejected("The Spotify callback query is invalid")
        }
        if (parameters.values.any { it.size != 1 }) {
            return SpotifyAuthorizationCallbackResult.Rejected("The Spotify callback contains duplicate parameters")
        }
        val returnedState = parameters["state"]?.singleOrNull()
            ?: return SpotifyAuthorizationCallbackResult.Rejected("The Spotify callback is missing state")
        if (!constantTimeEquals(returnedState, pendingRequest.state)) {
            return SpotifyAuthorizationCallbackResult.Rejected("The Spotify callback state did not match")
        }

        val providerError = parameters["error"]?.singleOrNull()
        if (!providerError.isNullOrBlank()) {
            return SpotifyAuthorizationCallbackResult.ProviderError(
                error = providerError,
                description = parameters["error_description"]?.singleOrNull(),
            )
        }

        val code = parameters["code"]?.singleOrNull()?.trim().orEmpty()
        if (code.isBlank()) {
            return SpotifyAuthorizationCallbackResult.Rejected("The Spotify callback is missing an authorization code")
        }
        return SpotifyAuthorizationCallbackResult.Authorized(
            authorizationCode = code,
            request = pendingRequest,
        )
    }

    private fun validateClientId(clientId: String) {
        require(clientId.isNotBlank()) { "ROCK_SPOTIFY_CLIENT_ID is not configured" }
        require(clientId.length in 8..128 && clientId.all(CLIENT_ID_CHARACTER_SET::contains)) {
            "ROCK_SPOTIFY_CLIENT_ID is malformed"
        }
    }

    private fun validateRedirectUri(value: String) {
        require(value.isNotBlank()) { "ROCK_SPOTIFY_REDIRECT_URI is not configured" }
        val uri = runCatching { URI(value) }.getOrNull()
        require(uri != null && uri.isAbsolute) { "ROCK_SPOTIFY_REDIRECT_URI is invalid" }
        require(!uri.scheme.equals("http", ignoreCase = true)) {
            "ROCK_SPOTIFY_REDIRECT_URI must use HTTPS or a private app scheme"
        }
        require(uri.userInfo == null && uri.fragment == null && uri.query == null) {
            "ROCK_SPOTIFY_REDIRECT_URI must not contain credentials, query parameters, or a fragment"
        }
        require(uri.scheme.equals("https", ignoreCase = true) || !uri.authority.isNullOrBlank()) {
            "ROCK_SPOTIFY_REDIRECT_URI must identify a callback target"
        }
    }

    private fun randomUrlSafeValue(byteCount: Int): String {
        val bytes = ByteArray(byteCount)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun sameRedirectTarget(callback: URI, redirect: URI): Boolean =
        callback.scheme.equals(redirect.scheme, ignoreCase = true) &&
            callback.rawAuthority.orEmpty().equals(redirect.rawAuthority.orEmpty(), ignoreCase = true) &&
            callback.rawPath.orEmpty() == redirect.rawPath.orEmpty() &&
            callback.userInfo == null &&
            callback.fragment == null

    private fun parseQuery(rawQuery: String?): Map<String, List<String>> {
        if (rawQuery.isNullOrBlank()) return emptyMap()
        return rawQuery.split('&')
            .filter(String::isNotBlank)
            .map { pair ->
                val separator = pair.indexOf('=')
                val key = if (separator >= 0) pair.substring(0, separator) else pair
                val value = if (separator >= 0) pair.substring(separator + 1) else ""
                key.decode() to value.decode()
            }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
    }

    private fun constantTimeEquals(first: String, second: String): Boolean =
        MessageDigest.isEqual(
            first.toByteArray(StandardCharsets.UTF_8),
            second.toByteArray(StandardCharsets.UTF_8),
        )

    private fun String.encode(): String =
        URLEncoder.encode(this, StandardCharsets.UTF_8.name())

    private fun String.decode(): String =
        URLDecoder.decode(this, StandardCharsets.UTF_8.name())

    private companion object {
        const val SPOTIFY_AUTHORIZE_ENDPOINT = "https://accounts.spotify.com/authorize"
        const val REQUEST_LIFETIME_MS = 10 * 60 * 1_000L
        const val MAX_SCOPE_COUNT = 16
        val CLIENT_ID_CHARACTER_SET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_-".toSet()
        val PKCE_CHARACTER_SET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._~".toSet()
        val SPOTIFY_SCOPE_PATTERN = Regex("^[a-z0-9-]+$")
        val SUPPORTED_SCOPES = setOf(
            "playlist-read-collaborative",
            "playlist-read-private",
            "user-read-email",
            "user-read-private",
        )
    }
}
