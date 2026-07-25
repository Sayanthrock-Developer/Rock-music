package com.rockmusic.app.data.integration

import android.net.Uri
import com.rockmusic.app.domain.integration.IntegrationId
import com.rockmusic.app.security.TokenVault
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

@Singleton
class SpotifyAuthorizationCallbackHandler @Inject constructor(
    private val requestFactory: SpotifyPkceRequestFactory,
    private val requestStore: EncryptedSpotifyPkceRequestStore,
    private val tokenClient: SpotifyPkceTokenClient,
    private val authorizationStore: IntegrationAuthorizationStore,
) {
    fun canHandle(uri: Uri?): Boolean =
        uri?.scheme.equals(CALLBACK_SCHEME, ignoreCase = true) &&
            uri?.host.equals(CALLBACK_HOST, ignoreCase = true) &&
            uri?.path == CALLBACK_PATH

    suspend fun handle(callbackUri: String): Result<String> = try {
        val pending = requestStore.current()
            ?: error("No pending Spotify authorization request was found.")
        val message = when (
            val callback = requestFactory.validateCallback(
                callbackUri = callbackUri,
                pendingRequest = pending,
            )
        ) {
            is SpotifyAuthorizationCallbackResult.Authorized -> {
                val consumed = requestStore.consume(callback.request.state)
                    ?: error("The Spotify authorization request expired, was consumed, or was replaced.")
                tokenClient.exchange(
                    code = callback.authorizationCode,
                    request = consumed,
                ).getOrThrow()
                authorizationStore.markAuthorized(IntegrationId.SPOTIFY)
                "Spotify authorization completed securely."
            }

            is SpotifyAuthorizationCallbackResult.ProviderError -> {
                requestStore.consume(pending.state)
                error(
                    callback.description?.takeIf(String::isNotBlank)
                        ?: "Spotify authorization was denied: ${callback.error}",
                )
            }

            is SpotifyAuthorizationCallbackResult.Rejected -> error(callback.reason)
        }
        Result.success(message)
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        Result.failure(error)
    }

    private companion object {
        const val CALLBACK_SCHEME = "rockmusic"
        const val CALLBACK_HOST = "oauth"
        const val CALLBACK_PATH = "/spotify"
    }
}

@Singleton
class SpotifyPkceTokenClient @Inject constructor(
    private val configuration: BuildConfigProviderConfigurationSource,
    private val vault: TokenVault,
) {
    private val client = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .build()
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun exchange(
        code: String,
        request: SpotifyPkceRequest,
        nowEpochMs: Long = System.currentTimeMillis(),
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val clientId = configuration.value(ProviderConfigKey.SPOTIFY_CLIENT_ID)
            require(clientId.isNotBlank()) { "Spotify client ID is not configured." }
            require(code.isNotBlank()) { "Spotify authorization code is empty." }

            val body = FormBody.Builder()
                .add("client_id", clientId)
                .add("grant_type", "authorization_code")
                .add("code", code)
                .add("redirect_uri", request.redirectUri)
                .add("code_verifier", request.codeVerifier)
                .build()
            val httpRequest = Request.Builder()
                .url(TOKEN_ENDPOINT)
                .post(body)
                .header("Accept", "application/json")
                .build()

            client.newCall(httpRequest).execute().use { response ->
                val responseBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    error("Spotify token exchange failed with HTTP ${response.code}.")
                }
                val token = json.decodeFromString<SpotifyTokenResponse>(responseBody)
                require(token.tokenType.equals("Bearer", ignoreCase = true)) {
                    "Spotify returned an unsupported token type."
                }
                require(token.accessToken.isNotBlank() && token.expiresInSeconds > 0L) {
                    "Spotify returned an invalid token response."
                }
                vault.put(
                    KEY_TOKEN,
                    json.encodeToString(
                        SpotifyTokenEnvelope(
                            accessToken = token.accessToken,
                            refreshToken = token.refreshToken,
                            scope = token.scope.orEmpty(),
                            expiresAtEpochMs = nowEpochMs + token.expiresInSeconds * 1_000L,
                        ),
                    ),
                )
            }
            Result.success(Unit)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    private companion object {
        const val TOKEN_ENDPOINT = "https://accounts.spotify.com/api/token"
        const val KEY_TOKEN = "spotify.oauth.token"
    }
}

@Serializable
private data class SpotifyTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresInSeconds: Long,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val scope: String? = null,
)

@Serializable
private data class SpotifyTokenEnvelope(
    val accessToken: String,
    val refreshToken: String? = null,
    val scope: String,
    val expiresAtEpochMs: Long,
)
