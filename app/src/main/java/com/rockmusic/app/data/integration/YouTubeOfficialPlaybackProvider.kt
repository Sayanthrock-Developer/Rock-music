package com.rockmusic.app.data.integration

import com.rockmusic.app.domain.integration.IntegrationAvailability
import com.rockmusic.app.domain.integration.IntegrationId
import com.rockmusic.app.domain.integration.OfficialPlaybackProvider
import com.rockmusic.app.domain.integration.ProviderCallResult
import com.rockmusic.app.domain.integration.ProviderCapabilities
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class YouTubeOfficialPlaybackProvider @Inject constructor() : OfficialPlaybackProvider {
    override val id: IntegrationId = IntegrationId.OFFICIAL_YOUTUBE

    override suspend fun availability(): IntegrationAvailability = IntegrationAvailability.Available

    override suspend fun capabilities(): ProviderCapabilities =
        ProviderCapabilities(canOpenOfficialPlayback = true)

    override fun openTrack(trackUri: String): ProviderCallResult<String> {
        val uri = runCatching { URI(trackUri.trim()) }.getOrNull()
            ?: return ProviderCallResult.Failure("The official provider link is invalid", retryable = false)
        val host = uri.host?.lowercase()
        val allowed = uri.scheme.equals("https", ignoreCase = true) && host in ALLOWED_HOSTS

        return if (allowed) {
            ProviderCallResult.Success(uri.toString())
        } else {
            ProviderCallResult.Failure(
                message = "Only official YouTube and YouTube Music HTTPS links are accepted",
                retryable = false,
            )
        }
    }

    override fun openSearch(query: String): ProviderCallResult<String> {
        val cleaned = query.trim()
        if (cleaned.isBlank()) {
            return ProviderCallResult.Failure("Enter a search query", retryable = false)
        }
        val encoded = URLEncoder.encode(cleaned, StandardCharsets.UTF_8.name())
        return ProviderCallResult.Success("https://music.youtube.com/search?q=$encoded")
    }

    private companion object {
        val ALLOWED_HOSTS = setOf(
            "youtube.com",
            "www.youtube.com",
            "m.youtube.com",
            "music.youtube.com",
            "youtu.be",
        )
    }
}
