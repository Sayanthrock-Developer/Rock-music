package com.rockmusic.app.data.integration

import com.rockmusic.app.security.TokenVault
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EncryptedSpotifyPkceRequestStore @Inject constructor(
    private val vault: TokenVault,
) : SpotifyPkceRequestStore {
    override suspend fun save(request: SpotifyPkceRequest) {
        clear()
        vault.put(KEY_AUTHORIZATION_URI, request.authorizationUri)
        vault.put(KEY_CODE_VERIFIER, request.codeVerifier)
        vault.put(KEY_CODE_CHALLENGE, request.codeChallenge)
        vault.put(KEY_STATE, request.state)
        vault.put(KEY_REDIRECT_URI, request.redirectUri)
        vault.put(KEY_SCOPES, request.scopes.sorted().joinToString("\n"))
        vault.put(KEY_CREATED_AT, request.createdAtEpochMs.toString())
        vault.put(KEY_EXPIRES_AT, request.expiresAtEpochMs.toString())
    }

    override suspend fun consume(state: String): SpotifyPkceRequest? {
        val savedState = vault.get(KEY_STATE) ?: return null
        if (!constantTimeEquals(savedState, state)) return null
        val request = read() ?: run {
            clear()
            return null
        }
        clear()
        return request
    }

    override suspend fun clearExpired(nowEpochMs: Long) {
        val expiresAt = vault.get(KEY_EXPIRES_AT)?.toLongOrNull() ?: return
        if (nowEpochMs >= expiresAt) clear()
    }

    suspend fun current(): SpotifyPkceRequest? = read()

    private fun read(): SpotifyPkceRequest? {
        val authorizationUri = vault.get(KEY_AUTHORIZATION_URI) ?: return null
        val codeVerifier = vault.get(KEY_CODE_VERIFIER) ?: return null
        val codeChallenge = vault.get(KEY_CODE_CHALLENGE) ?: return null
        val state = vault.get(KEY_STATE) ?: return null
        val redirectUri = vault.get(KEY_REDIRECT_URI) ?: return null
        val createdAt = vault.get(KEY_CREATED_AT)?.toLongOrNull() ?: return null
        val expiresAt = vault.get(KEY_EXPIRES_AT)?.toLongOrNull() ?: return null
        val scopes = vault.get(KEY_SCOPES)
            .orEmpty()
            .lineSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .toSet()
        return SpotifyPkceRequest(
            authorizationUri = authorizationUri,
            codeVerifier = codeVerifier,
            codeChallenge = codeChallenge,
            state = state,
            redirectUri = redirectUri,
            scopes = scopes,
            createdAtEpochMs = createdAt,
            expiresAtEpochMs = expiresAt,
        )
    }

    private fun clear() {
        listOf(
            KEY_AUTHORIZATION_URI,
            KEY_CODE_VERIFIER,
            KEY_CODE_CHALLENGE,
            KEY_STATE,
            KEY_REDIRECT_URI,
            KEY_SCOPES,
            KEY_CREATED_AT,
            KEY_EXPIRES_AT,
        ).forEach(vault::remove)
    }

    private fun constantTimeEquals(first: String, second: String): Boolean =
        MessageDigest.isEqual(
            first.toByteArray(StandardCharsets.UTF_8),
            second.toByteArray(StandardCharsets.UTF_8),
        )

    private companion object {
        const val KEY_AUTHORIZATION_URI = "spotify.pkce.authorization_uri"
        const val KEY_CODE_VERIFIER = "spotify.pkce.code_verifier"
        const val KEY_CODE_CHALLENGE = "spotify.pkce.code_challenge"
        const val KEY_STATE = "spotify.pkce.state"
        const val KEY_REDIRECT_URI = "spotify.pkce.redirect_uri"
        const val KEY_SCOPES = "spotify.pkce.scopes"
        const val KEY_CREATED_AT = "spotify.pkce.created_at"
        const val KEY_EXPIRES_AT = "spotify.pkce.expires_at"
    }
}
