package com.rockmusic.app.data.integration

import com.rockmusic.app.security.TokenVault
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/** A safe, display-only projection of a Spotify playlist. Playback remains in Spotify. */
data class SpotifyPlaylistPreview(
    val id: String,
    val name: String,
    val ownerName: String,
    val description: String,
    val imageUrl: String?,
    val externalUrl: String,
    val totalTracks: Int,
    val tracks: List<SpotifyPlaylistTrackPreview>,
)

data class SpotifyPlaylistTrackPreview(
    val id: String,
    val name: String,
    val artists: String,
    val albumName: String,
    val imageUrl: String?,
    val externalUrl: String,
    val durationMs: Long,
    val explicit: Boolean,
)

object SpotifyPlaylistReferenceParser {
    private val playlistIdPattern = Regex("^[A-Za-z0-9]{22}$")

    fun parse(reference: String): Result<String> = runCatching {
        val value = reference.trim()
        require(value.isNotBlank()) { "Enter a Spotify playlist link or URI." }

        val playlistId = when {
            value.startsWith("spotify:", ignoreCase = true) -> {
                val parts = value.split(':')
                require(
                    parts.size == 3 &&
                        parts[0].equals("spotify", ignoreCase = true) &&
                        parts[1].equals("playlist", ignoreCase = true),
                ) { "Enter an exact spotify:playlist: URI." }
                parts[2]
            }

            else -> {
                val uri = URI(value)
                require(uri.scheme.equals("https", ignoreCase = true)) {
                    "Spotify playlist links must use HTTPS."
                }
                require(uri.host.equals("open.spotify.com", ignoreCase = true)) {
                    "Only open.spotify.com playlist links are supported."
                }
                require(
                    uri.port == -1 &&
                        uri.userInfo == null &&
                        uri.fragment == null,
                ) { "The Spotify playlist link is malformed." }
                val segments = uri.path.orEmpty().split('/').filter(String::isNotBlank)
                require(segments.size == 2 && segments[0].equals("playlist", ignoreCase = true)) {
                    "Enter a Spotify playlist link, not an album, artist, or track link."
                }
                segments[1]
            }
        }

        require(playlistIdPattern.matches(playlistId)) {
            "The Spotify playlist ID is invalid."
        }
        playlistId
    }

    fun canonicalWebUrl(playlistId: String): String {
        require(playlistIdPattern.matches(playlistId)) { "The Spotify playlist ID is invalid." }
        return "https://open.spotify.com/playlist/$playlistId"
    }
}

@Singleton
class SpotifyPlaylistClient @Inject constructor(
    private val configuration: BuildConfigProviderConfigurationSource,
    private val vault: TokenVault,
) {
    private val client = OkHttpClient.Builder()
        .followRedirects(false)
        .followSslRedirects(false)
        .build()
    private val json = Json { ignoreUnknownKeys = true }
    private val tokenMutex = Mutex()

    suspend fun load(reference: String): Result<SpotifyPlaylistPreview> = withContext(Dispatchers.IO) {
        try {
            val playlistId = SpotifyPlaylistReferenceParser.parse(reference).getOrThrow()
            val firstToken = validAccessToken().getOrThrow()
            val firstResponse = executePlaylistRequest(playlistId, firstToken)
            if (firstResponse.code != HTTP_UNAUTHORIZED) {
                return@withContext firstResponse.toResult(playlistId)
            }

            firstResponse.close()
            val refreshedToken = refreshAccessToken(force = true).getOrThrow()
            executePlaylistRequest(playlistId, refreshedToken).toResult(playlistId)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            Result.failure(error)
        }
    }

    private suspend fun validAccessToken(nowEpochMs: Long = System.currentTimeMillis()): Result<String> =
        tokenMutex.withLock {
            resultPreservingCancellation {
                val token = readToken()
                    ?: error("Spotify is not authorised. Complete Spotify sign-in first.")
                if (nowEpochMs + EXPIRY_SAFETY_WINDOW_MS < token.expiresAtEpochMs) {
                    token.accessToken
                } else {
                    refreshTokenLocked(token).accessToken
                }
            }
        }

    private suspend fun refreshAccessToken(force: Boolean): Result<String> = tokenMutex.withLock {
        resultPreservingCancellation {
            val token = readToken()
                ?: error("Spotify is not authorised. Complete Spotify sign-in first.")
            if (!force && System.currentTimeMillis() + EXPIRY_SAFETY_WINDOW_MS < token.expiresAtEpochMs) {
                token.accessToken
            } else {
                refreshTokenLocked(token).accessToken
            }
        }
    }

    private fun refreshTokenLocked(current: SpotifyStoredToken): SpotifyStoredToken {
        val refreshToken = current.refreshToken
            ?.takeIf(String::isNotBlank)
            ?: error("Spotify authorisation expired. Re-authorise Spotify to continue.")
        val clientId = configuration.value(ProviderConfigKey.SPOTIFY_CLIENT_ID)
        require(clientId.isNotBlank()) { "Spotify client ID is not configured." }

        val request = Request.Builder()
            .url(TOKEN_ENDPOINT)
            .post(
                FormBody.Builder()
                    .add("client_id", clientId)
                    .add("grant_type", "refresh_token")
                    .add("refresh_token", refreshToken)
                    .build(),
            )
            .header("Accept", "application/json")
            .build()

        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                error("Spotify token refresh failed with HTTP ${response.code}. Re-authorise Spotify.")
            }
            val refreshed = json.decodeFromString<SpotifyRefreshTokenResponse>(body)
            require(refreshed.tokenType.equals("Bearer", ignoreCase = true)) {
                "Spotify returned an unsupported token type."
            }
            require(refreshed.accessToken.isNotBlank() && refreshed.expiresInSeconds > 0L) {
                "Spotify returned an invalid refresh response."
            }
            return SpotifyStoredToken(
                accessToken = refreshed.accessToken,
                refreshToken = refreshed.refreshToken?.takeIf(String::isNotBlank) ?: refreshToken,
                scope = refreshed.scope ?: current.scope,
                expiresAtEpochMs = System.currentTimeMillis() + refreshed.expiresInSeconds * 1_000L,
            ).also(::writeToken)
        }
    }

    private fun executePlaylistRequest(playlistId: String, accessToken: String): Response {
        val request = Request.Builder()
            .url("$API_BASE_URL/playlists/$playlistId")
            .get()
            .header("Authorization", "Bearer $accessToken")
            .header("Accept", "application/json")
            .build()
        return client.newCall(request).execute()
    }

    private fun Response.toResult(playlistId: String): Result<SpotifyPlaylistPreview> = use { response ->
        val body = response.body?.string().orEmpty()
        when {
            response.isSuccessful -> runCatching {
                json.decodeFromString<SpotifyPlaylistResponse>(body).toPreview()
            }

            response.code == HTTP_NOT_FOUND -> Result.failure(
                IllegalArgumentException("Spotify could not find playlist $playlistId or it is not visible to this account."),
            )

            response.code == HTTP_FORBIDDEN -> Result.failure(
                IllegalStateException("This Spotify account is not permitted to read that playlist."),
            )

            response.code == HTTP_UNAUTHORIZED -> Result.failure(
                IllegalStateException("Spotify authorisation expired. Re-authorise Spotify to continue."),
            )

            response.code == HTTP_RATE_LIMITED -> Result.failure(
                IllegalStateException("Spotify rate-limited this request. Try again later."),
            )

            else -> Result.failure(
                IllegalStateException("Spotify playlist request failed with HTTP ${response.code}."),
            )
        }
    }

    private fun readToken(): SpotifyStoredToken? = vault.get(KEY_TOKEN)
        ?.let { payload -> runCatching { json.decodeFromString<SpotifyStoredToken>(payload) }.getOrNull() }

    private fun writeToken(token: SpotifyStoredToken) {
        vault.put(KEY_TOKEN, json.encodeToString(token))
    }

    private fun SpotifyPlaylistResponse.toPreview(): SpotifyPlaylistPreview = SpotifyPlaylistPreview(
        id = id,
        name = name,
        ownerName = owner.displayName.orEmpty().ifBlank { owner.id },
        description = description.orEmpty(),
        imageUrl = images.maxByOrNull { it.width ?: 0 }?.url,
        externalUrl = externalUrls.spotify.ifBlank {
            SpotifyPlaylistReferenceParser.canonicalWebUrl(id)
        },
        totalTracks = tracks.total,
        tracks = tracks.items.mapNotNull { item ->
            val track = item.track ?: return@mapNotNull null
            val trackId = track.id?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val album = track.album
            if (track.type != "track" || album == null) return@mapNotNull null
            SpotifyPlaylistTrackPreview(
                id = trackId,
                name = track.name,
                artists = track.artists.joinToString { artist -> artist.name }
                    .ifBlank { "Unknown artist" },
                albumName = album.name,
                imageUrl = album.images.maxByOrNull { image -> image.width ?: 0 }?.url,
                externalUrl = track.externalUrls.spotify.ifBlank {
                    "https://open.spotify.com/track/$trackId"
                },
                durationMs = track.durationMs,
                explicit = track.explicit,
            )
        },
    )

    private inline fun <T> resultPreservingCancellation(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        Result.failure(error)
    }

    private companion object {
        const val API_BASE_URL = "https://api.spotify.com/v1"
        const val TOKEN_ENDPOINT = "https://accounts.spotify.com/api/token"
        const val KEY_TOKEN = "spotify.oauth.token"
        const val EXPIRY_SAFETY_WINDOW_MS = 60_000L
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
        const val HTTP_NOT_FOUND = 404
        const val HTTP_RATE_LIMITED = 429
    }
}

@Serializable
private data class SpotifyStoredToken(
    val accessToken: String,
    val refreshToken: String? = null,
    val scope: String,
    val expiresAtEpochMs: Long,
)

@Serializable
private data class SpotifyRefreshTokenResponse(
    @SerialName("access_token") val accessToken: String,
    @SerialName("token_type") val tokenType: String,
    @SerialName("expires_in") val expiresInSeconds: Long,
    @SerialName("refresh_token") val refreshToken: String? = null,
    val scope: String? = null,
)

@Serializable
private data class SpotifyPlaylistResponse(
    val id: String,
    val name: String,
    val description: String? = null,
    val images: List<SpotifyImage> = emptyList(),
    val owner: SpotifyOwner,
    @SerialName("external_urls") val externalUrls: SpotifyExternalUrls = SpotifyExternalUrls(),
    val tracks: SpotifyPlaylistTracks,
)

@Serializable
private data class SpotifyOwner(
    val id: String,
    @SerialName("display_name") val displayName: String? = null,
)

@Serializable
private data class SpotifyExternalUrls(
    val spotify: String = "",
)

@Serializable
private data class SpotifyImage(
    val url: String,
    val width: Int? = null,
    val height: Int? = null,
)

@Serializable
private data class SpotifyPlaylistTracks(
    val total: Int = 0,
    val items: List<SpotifyPlaylistItem> = emptyList(),
)

@Serializable
private data class SpotifyPlaylistItem(
    val track: SpotifyTrack? = null,
)

@Serializable
private data class SpotifyTrack(
    val id: String? = null,
    val name: String = "",
    val type: String = "track",
    val artists: List<SpotifyArtist> = emptyList(),
    val album: SpotifyAlbum? = null,
    @SerialName("external_urls") val externalUrls: SpotifyExternalUrls = SpotifyExternalUrls(),
    @SerialName("duration_ms") val durationMs: Long = 0L,
    val explicit: Boolean = false,
)

@Serializable
private data class SpotifyArtist(
    val name: String,
)

@Serializable
private data class SpotifyAlbum(
    val name: String = "",
    val images: List<SpotifyImage> = emptyList(),
)
