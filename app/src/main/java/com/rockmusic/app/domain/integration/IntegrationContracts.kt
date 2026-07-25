package com.rockmusic.app.domain.integration

/**
 * External integrations supported by Rock Music.
 *
 * Provider-backed features must expose an explicit availability state. A UI must never report
 * success when configuration, authentication, connectivity, legal access, or provider capability
 * checks are missing.
 */
enum class IntegrationId {
    SPOTIFY,
    ECHO_FIND,
    LISTEN_TOGETHER,
    DISCORD,
    LICENSED_MUSIC,
    LYRICS,
    PODCAST_SEARCH,
    PERMITTED_DOWNLOADS,
    USER_CLOUD,
    OFFICIAL_YOUTUBE,
}

sealed interface IntegrationAvailability {
    data object Available : IntegrationAvailability

    data class Unconfigured(
        val missingKeys: Set<String>,
    ) : IntegrationAvailability

    data object AuthenticationRequired : IntegrationAvailability

    data object Offline : IntegrationAvailability

    data class Unsupported(
        val reason: String,
    ) : IntegrationAvailability

    data class Error(
        val message: String,
        val retryable: Boolean = true,
    ) : IntegrationAvailability
}

/**
 * Capabilities are evaluated per provider and, when required, per media item.
 * Download support defaults to denied and must be granted explicitly by the source.
 */
data class ProviderCapabilities(
    val canSearch: Boolean = false,
    val canStream: Boolean = false,
    val canOpenOfficialPlayback: Boolean = false,
    val canReadPlaylistMetadata: Boolean = false,
    val canRecognizeAudio: Boolean = false,
    val canProvideLyrics: Boolean = false,
    val canSearchPodcasts: Boolean = false,
    val canCreateListeningRooms: Boolean = false,
    val canShareActivity: Boolean = false,
    val canDownload: Boolean = false,
)

data class DownloadPermission(
    val allowed: Boolean,
    val reason: String,
    val expiresAtEpochMs: Long? = null,
) {
    companion object {
        fun denied(reason: String = "The selected source does not permit downloading") =
            DownloadPermission(allowed = false, reason = reason)
    }
}

interface IntegrationGateway {
    val id: IntegrationId

    suspend fun availability(): IntegrationAvailability

    suspend fun capabilities(): ProviderCapabilities
}
