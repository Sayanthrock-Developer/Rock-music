package com.rockmusic.app.domain.integration

import com.rockmusic.app.data.integration.ProviderConfigKey

data class IntegrationDefinition(
    val id: IntegrationId,
    val displayName: String,
    val requiredConfiguration: Set<ProviderConfigKey>,
    val capabilities: ProviderCapabilities,
    val requiresUserAuthentication: Boolean = false,
    val officialProviderOnly: Boolean = false,
)

object ProviderDefinitions {
    val all: List<IntegrationDefinition> = listOf(
        IntegrationDefinition(
            id = IntegrationId.SPOTIFY,
            displayName = "Spotify playlist import",
            requiredConfiguration = setOf(
                ProviderConfigKey.SPOTIFY_CLIENT_ID,
                ProviderConfigKey.SPOTIFY_REDIRECT_URI,
            ),
            capabilities = ProviderCapabilities(
                canOpenOfficialPlayback = true,
                canReadPlaylistMetadata = true,
            ),
            requiresUserAuthentication = true,
        ),
        IntegrationDefinition(
            id = IntegrationId.ECHO_FIND,
            displayName = "Echo Find recognition",
            requiredConfiguration = setOf(
                ProviderConfigKey.ECHO_FIND_BASE_URL,
                ProviderConfigKey.ECHO_FIND_API_KEY,
            ),
            capabilities = ProviderCapabilities(canRecognizeAudio = true),
        ),
        IntegrationDefinition(
            id = IntegrationId.LISTEN_TOGETHER,
            displayName = "Listen Together",
            requiredConfiguration = setOf(
                ProviderConfigKey.LISTEN_TOGETHER_REST_URL,
                ProviderConfigKey.LISTEN_TOGETHER_WS_URL,
            ),
            capabilities = ProviderCapabilities(canCreateListeningRooms = true),
            requiresUserAuthentication = true,
        ),
        IntegrationDefinition(
            id = IntegrationId.DISCORD,
            displayName = "Discord activity",
            requiredConfiguration = setOf(ProviderConfigKey.DISCORD_CLIENT_ID),
            capabilities = ProviderCapabilities(canShareActivity = true),
            requiresUserAuthentication = true,
        ),
        IntegrationDefinition(
            id = IntegrationId.LICENSED_MUSIC,
            displayName = "Licensed music catalogue",
            requiredConfiguration = setOf(
                ProviderConfigKey.CATALOGUE_BASE_URL,
                ProviderConfigKey.CATALOGUE_API_KEY,
            ),
            capabilities = ProviderCapabilities(
                canSearch = true,
                canStream = true,
                canOpenOfficialPlayback = true,
            ),
            requiresUserAuthentication = true,
        ),
        IntegrationDefinition(
            id = IntegrationId.LYRICS,
            displayName = "Synchronized lyrics",
            requiredConfiguration = setOf(
                ProviderConfigKey.LYRICS_BASE_URL,
                ProviderConfigKey.LYRICS_API_KEY,
            ),
            capabilities = ProviderCapabilities(canProvideLyrics = true),
        ),
        IntegrationDefinition(
            id = IntegrationId.PODCAST_SEARCH,
            displayName = "Podcast search",
            requiredConfiguration = setOf(
                ProviderConfigKey.PODCAST_SEARCH_BASE_URL,
                ProviderConfigKey.PODCAST_SEARCH_API_KEY,
            ),
            capabilities = ProviderCapabilities(canSearchPodcasts = true),
        ),
        IntegrationDefinition(
            id = IntegrationId.PERMITTED_DOWNLOADS,
            displayName = "Permitted downloads",
            requiredConfiguration = setOf(
                ProviderConfigKey.DOWNLOADS_BASE_URL,
                ProviderConfigKey.DOWNLOADS_API_KEY,
            ),
            capabilities = ProviderCapabilities(canDownload = true),
        ),
        IntegrationDefinition(
            id = IntegrationId.USER_CLOUD,
            displayName = "Cloud storage",
            requiredConfiguration = setOf(
                ProviderConfigKey.CLOUD_CLIENT_ID,
                ProviderConfigKey.CLOUD_REDIRECT_URI,
            ),
            capabilities = ProviderCapabilities(
                canSearch = true,
                canStream = true,
                canDownload = true,
            ),
            requiresUserAuthentication = true,
        ),
        IntegrationDefinition(
            id = IntegrationId.OFFICIAL_YOUTUBE,
            displayName = "YouTube / YouTube Music",
            requiredConfiguration = emptySet(),
            capabilities = ProviderCapabilities(canOpenOfficialPlayback = true),
            officialProviderOnly = true,
        ),
    )

    init {
        check(all.map(IntegrationDefinition::id).toSet().size == IntegrationId.entries.size) {
            "Every IntegrationId must have exactly one provider definition"
        }
    }
}
