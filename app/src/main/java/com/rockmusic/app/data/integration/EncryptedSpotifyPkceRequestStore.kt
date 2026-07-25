package com.rockmusic.app.data.integration

import com.rockmusic.app.security.TokenVault
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class EncryptedSpotifyPkceRequestStore @Inject constructor(
    private val vault: TokenVault,
) : SpotifyPkceRequestStore {
    private val json = Json { ignoreUnknownKeys = false }

    override suspend fun save(request: SpotifyPkceRequest) = withContext(Dispatchers.IO) {
        vault.put(KEY_REQUEST, json.encodeToString(SpotifyPkceRequestEnvelope.from(request)))
    }

    override suspend fun consume(state: String): SpotifyPkceRequest? = withContext(Dispatchers.IO) {
        val request = read() ?: return@withContext null
        if (!constantTimeEquals(request.state, state)) return@withContext null
        vault.remove(KEY_REQUEST)
        request.takeIf { System.currentTimeMillis() < it.expiresAtEpochMs }
    }

    override suspend fun clearExpired(nowEpochMs: Long) = withContext(Dispatchers.IO) {
        val request = read() ?: return@withContext
        if (nowEpochMs >= request.expiresAtEpochMs) vault.remove(KEY_REQUEST)
    }

    suspend fun current(): SpotifyPkceRequest? = withContext(Dispatchers.IO) { read() }

    private fun read(): SpotifyPkceRequest? = vault.get(KEY_REQUEST)
        ?.let { payload ->
            runCatching { json.decodeFromString<SpotifyPkceRequestEnvelope>(payload).toRequest() }
                .getOrNull()
        }

    private fun constantTimeEquals(first: String, second: String): Boolean =
        MessageDigest.isEqual(
            first.toByteArray(StandardCharsets.UTF_8),
            second.toByteArray(StandardCharsets.UTF_8),
        )

    private companion object {
        const val KEY_REQUEST = "spotify.pkce.request"
    }
}

@Serializable
private data class SpotifyPkceRequestEnvelope(
    val authorizationUri: String,
    val codeVerifier: String,
    val codeChallenge: String,
    val state: String,
    val redirectUri: String,
    val scopes: List<String>,
    val createdAtEpochMs: Long,
    val expiresAtEpochMs: Long,
) {
    fun toRequest(): SpotifyPkceRequest = SpotifyPkceRequest(
        authorizationUri = authorizationUri,
        codeVerifier = codeVerifier,
        codeChallenge = codeChallenge,
        state = state,
        redirectUri = redirectUri,
        scopes = scopes.toSet(),
        createdAtEpochMs = createdAtEpochMs,
        expiresAtEpochMs = expiresAtEpochMs,
    )

    companion object {
        fun from(request: SpotifyPkceRequest): SpotifyPkceRequestEnvelope =
            SpotifyPkceRequestEnvelope(
                authorizationUri = request.authorizationUri,
                codeVerifier = request.codeVerifier,
                codeChallenge = request.codeChallenge,
                state = request.state,
                redirectUri = request.redirectUri,
                scopes = request.scopes.sorted(),
                createdAtEpochMs = request.createdAtEpochMs,
                expiresAtEpochMs = request.expiresAtEpochMs,
            )
    }
}
