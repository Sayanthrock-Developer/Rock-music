package com.rockmusic.app.data.integration

import com.rockmusic.app.domain.integration.IntegrationAvailability
import com.rockmusic.app.domain.integration.IntegrationId
import com.rockmusic.app.domain.integration.OfficialPlaybackProvider
import com.rockmusic.app.domain.integration.OfficialProviderRoute
import com.rockmusic.app.domain.integration.OfficialRouteKind
import com.rockmusic.app.domain.integration.ProviderCallResult
import com.rockmusic.app.domain.integration.ProviderCapabilities
import com.rockmusic.app.domain.integration.mapValue
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeOfficialPlaybackProvider @Inject constructor() : OfficialPlaybackProvider {
    override val id: IntegrationId = IntegrationId.OFFICIAL_YOUTUBE

    override suspend fun availability(): IntegrationAvailability = IntegrationAvailability.Available

    override suspend fun capabilities(): ProviderCapabilities =
        ProviderCapabilities(canOpenOfficialPlayback = true, canSearch = true)

    override fun openTrack(trackUri: String): ProviderCallResult<String> =
        routeTrack(trackUri).mapValue(OfficialProviderRoute::webUri)

    override fun openSearch(query: String): ProviderCallResult<String> =
        routeSearch(query).mapValue(OfficialProviderRoute::webUri)

    override fun routeTrack(trackUri: String): ProviderCallResult<OfficialProviderRoute> {
        val uri = runCatching { URI(trackUri.trim()) }.getOrNull()
            ?: return failure("The official provider link is invalid")
        val host = uri.host?.lowercase()
        if (
            !uri.scheme.equals("https", ignoreCase = true) ||
            host !in ALLOWED_HOSTS ||
            uri.userInfo != null ||
            uri.port != -1 ||
            uri.fragment != null
        ) {
            return failure("Only validated official YouTube and YouTube Music HTTPS links are accepted")
        }

        return if (host == "youtu.be") {
            val videoId = uri.path.orEmpty().trim('/').takeIf(::isValidVideoId)
                ?: return failure("The YouTube short link does not contain a valid video ID")
            if (!uri.rawQuery.isNullOrBlank()) {
                return failure("YouTube short links with extra query parameters are not accepted")
            }
            videoRoute(videoId, preferMusic = false)
        } else {
            when {
                uri.path == "/watch" -> {
                    val params = runCatching { parseQuery(uri.rawQuery) }.getOrElse {
                        return failure("The YouTube watch link has an invalid query")
                    }
                    if (params.values.any { it.size != 1 }) {
                        return failure("The YouTube watch link contains duplicate parameters")
                    }
                    val videoId = params["v"]?.singleOrNull()?.takeIf(::isValidVideoId)
                        ?: return failure("The YouTube watch link does not contain a valid video ID")
                    if (params.keys.any { it !in WATCH_QUERY_KEYS }) {
                        return failure("The YouTube watch link contains unsupported parameters")
                    }
                    videoRoute(videoId, preferMusic = host == "music.youtube.com")
                }

                uri.path.orEmpty().startsWith("/shorts/") -> {
                    if (!uri.rawQuery.isNullOrBlank()) {
                        return failure("YouTube Shorts links with query parameters are not accepted")
                    }
                    val videoId = uri.path.removePrefix("/shorts/").trim('/').takeIf(::isValidVideoId)
                        ?: return failure("The YouTube Shorts link does not contain a valid video ID")
                    videoRoute(videoId, preferMusic = false)
                }

                uri.path == "/playlist" -> {
                    val params = runCatching { parseQuery(uri.rawQuery) }.getOrElse {
                        return failure("The YouTube playlist link has an invalid query")
                    }
                    if (params.values.any { it.size != 1 }) {
                        return failure("The YouTube playlist link contains duplicate parameters")
                    }
                    val playlistId = params["list"]?.singleOrNull()?.takeIf(::isValidPlaylistId)
                        ?: return failure("The YouTube playlist link does not contain a valid playlist ID")
                    if (params.keys != setOf("list")) {
                        return failure("The YouTube playlist link contains unsupported parameters")
                    }
                    playlistRoute(playlistId, preferMusic = host == "music.youtube.com")
                }

                else -> failure("This official YouTube link type is not supported for playback")
            }
        }
    }

    override fun routeSearch(query: String): ProviderCallResult<OfficialProviderRoute> {
        val cleaned = query.trim()
        if (cleaned.isBlank()) return failure("Enter a search query")
        if (cleaned.length > MAX_SEARCH_LENGTH || cleaned.any(Char::isISOControl)) {
            return failure("The search query is too long or contains control characters")
        }
        val encoded = URLEncoder.encode(cleaned, StandardCharsets.UTF_8.name())
        val uri = "https://music.youtube.com/search?q=$encoded"
        return ProviderCallResult.Success(
            OfficialProviderRoute(
                webUri = uri,
                androidAppUri = uri,
                preferredPackages = listOf(YOUTUBE_MUSIC_PACKAGE, YOUTUBE_PACKAGE),
                kind = OfficialRouteKind.SEARCH,
            ),
        )
    }

    private fun videoRoute(
        videoId: String,
        preferMusic: Boolean,
    ): ProviderCallResult<OfficialProviderRoute> {
        val webUri = if (preferMusic) {
            "https://music.youtube.com/watch?v=$videoId"
        } else {
            "https://www.youtube.com/watch?v=$videoId"
        }
        return ProviderCallResult.Success(
            OfficialProviderRoute(
                webUri = webUri,
                androidAppUri = "vnd.youtube:$videoId",
                preferredPackages = if (preferMusic) {
                    listOf(YOUTUBE_MUSIC_PACKAGE, YOUTUBE_PACKAGE)
                } else {
                    listOf(YOUTUBE_PACKAGE, YOUTUBE_MUSIC_PACKAGE)
                },
                kind = OfficialRouteKind.VIDEO,
                providerMediaId = videoId,
            ),
        )
    }

    private fun playlistRoute(
        playlistId: String,
        preferMusic: Boolean,
    ): ProviderCallResult<OfficialProviderRoute> {
        val host = if (preferMusic) "music.youtube.com" else "www.youtube.com"
        val webUri = "https://$host/playlist?list=$playlistId"
        return ProviderCallResult.Success(
            OfficialProviderRoute(
                webUri = webUri,
                androidAppUri = webUri,
                preferredPackages = if (preferMusic) {
                    listOf(YOUTUBE_MUSIC_PACKAGE, YOUTUBE_PACKAGE)
                } else {
                    listOf(YOUTUBE_PACKAGE, YOUTUBE_MUSIC_PACKAGE)
                },
                kind = OfficialRouteKind.PLAYLIST,
                providerMediaId = playlistId,
            ),
        )
    }

    private fun parseQuery(rawQuery: String?): Map<String, List<String>> {
        if (rawQuery.isNullOrBlank()) return emptyMap()
        return rawQuery.split('&')
            .filter(String::isNotBlank)
            .map { pair ->
                val separator = pair.indexOf('=')
                val key = if (separator >= 0) pair.substring(0, separator) else pair
                val value = if (separator >= 0) pair.substring(separator + 1) else ""
                URLDecoder.decode(key, StandardCharsets.UTF_8.name()) to
                    URLDecoder.decode(value, StandardCharsets.UTF_8.name())
            }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
    }

    private fun isValidVideoId(value: String): Boolean = VIDEO_ID_PATTERN.matches(value)

    private fun isValidPlaylistId(value: String): Boolean = PLAYLIST_ID_PATTERN.matches(value)

    private fun <T> failure(message: String): ProviderCallResult<T> =
        ProviderCallResult.Failure(message = message, retryable = false)

    private companion object {
        const val MAX_SEARCH_LENGTH = 200
        const val YOUTUBE_PACKAGE = "com.google.android.youtube"
        const val YOUTUBE_MUSIC_PACKAGE = "com.google.android.apps.youtube.music"
        val VIDEO_ID_PATTERN = Regex("^[A-Za-z0-9_-]{6,64}$")
        val PLAYLIST_ID_PATTERN = Regex("^[A-Za-z0-9_-]{10,128}$")
        val WATCH_QUERY_KEYS = setOf("v", "list", "index", "start", "t")
        val ALLOWED_HOSTS = setOf(
            "youtube.com",
            "www.youtube.com",
            "m.youtube.com",
            "music.youtube.com",
            "youtu.be",
        )
    }
}
