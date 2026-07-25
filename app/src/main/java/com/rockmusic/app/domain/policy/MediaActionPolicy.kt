package com.rockmusic.app.domain.policy

import com.rockmusic.app.domain.integration.DownloadPermission
import javax.inject.Inject
import javax.inject.Singleton

enum class MediaOperation {
    PLAY,
    DOWNLOAD,
    SHARE,
    OPEN_OFFICIAL_PROVIDER,
    RECOGNISE,
    CREATE_ROOM,
    JOIN_ROOM,
}

enum class MediaOrigin {
    LOCAL_FILE,
    PODCAST_RSS,
    USER_CLOUD,
    LICENSED_CATALOGUE,
    OFFICIAL_PROVIDER_LINK,
    PUBLIC_DOMAIN,
    CREATIVE_COMMONS,
    UNKNOWN,
}

data class ProviderAccessContext(
    val providerName: String? = null,
    val missingConfigurationKeys: Set<String> = emptySet(),
    val authenticationRequired: Boolean = false,
    val authenticated: Boolean = false,
    val online: Boolean = true,
    val regionalAccessAllowed: Boolean = true,
    val playbackEntitled: Boolean = false,
    val providerCapabilityGranted: Boolean = false,
    val requiresOfficialClient: Boolean = false,
    val officialUri: String? = null,
    val downloadPermission: DownloadPermission = DownloadPermission.denied(),
    val userConsentGranted: Boolean = false,
    val participantAccessConfirmed: Boolean = false,
)

data class MediaActionRequest(
    val operation: MediaOperation,
    val origin: MediaOrigin,
    val mediaId: String,
    val sourceUri: String? = null,
    val access: ProviderAccessContext = ProviderAccessContext(),
)

sealed interface MediaActionDecision {
    data object ExecuteInApp : MediaActionDecision

    data class OpenOfficialProvider(
        val uri: String,
        val reason: String,
    ) : MediaActionDecision

    data class RequireConfiguration(
        val missingKeys: Set<String>,
    ) : MediaActionDecision

    data class RequireAuthentication(
        val providerName: String,
    ) : MediaActionDecision

    data object Offline : MediaActionDecision

    data class Blocked(
        val reason: String,
    ) : MediaActionDecision
}

/**
 * Single decision point for every media action in Rock Music.
 *
 * Screens and services must ask this policy before playing, downloading, recognising, sharing,
 * or synchronising media. Remote actions fail closed. Protected-provider content is handed to an
 * official client when in-app execution is not explicitly permitted.
 */
@Singleton
class MediaActionPolicyEngine @Inject constructor() {
    fun decide(request: MediaActionRequest): MediaActionDecision {
        val access = request.access
        val providerBacked = request.origin in REMOTE_ORIGINS ||
            request.operation in PROVIDER_OPERATIONS

        if (providerBacked && access.missingConfigurationKeys.isNotEmpty()) {
            return MediaActionDecision.RequireConfiguration(access.missingConfigurationKeys)
        }

        if (providerBacked && !access.online) {
            return MediaActionDecision.Offline
        }

        if (providerBacked && access.authenticationRequired && !access.authenticated) {
            return MediaActionDecision.RequireAuthentication(
                providerName = access.providerName ?: "Provider",
            )
        }

        if (providerBacked && !access.regionalAccessAllowed) {
            return MediaActionDecision.Blocked(
                reason = "This provider does not grant access in the current region.",
            )
        }

        return when (request.operation) {
            MediaOperation.PLAY -> decidePlayback(request)
            MediaOperation.DOWNLOAD -> decideDownload(request)
            MediaOperation.SHARE -> decideShare(request)
            MediaOperation.OPEN_OFFICIAL_PROVIDER -> openOfficial(access)
            MediaOperation.RECOGNISE -> decideRecognition(access)
            MediaOperation.CREATE_ROOM,
            MediaOperation.JOIN_ROOM,
            -> decideRoom(access)
        }
    }

    private fun decidePlayback(request: MediaActionRequest): MediaActionDecision = when (request.origin) {
        MediaOrigin.LOCAL_FILE,
        MediaOrigin.PODCAST_RSS,
        MediaOrigin.PUBLIC_DOMAIN,
        MediaOrigin.CREATIVE_COMMONS,
        -> MediaActionDecision.ExecuteInApp

        MediaOrigin.USER_CLOUD,
        MediaOrigin.LICENSED_CATALOGUE,
        -> {
            when {
                request.access.requiresOfficialClient -> openOfficial(request.access)
                request.access.playbackEntitled && request.access.providerCapabilityGranted ->
                    MediaActionDecision.ExecuteInApp
                request.access.officialUri != null -> openOfficial(request.access)
                else -> MediaActionDecision.Blocked(
                    reason = "The provider has not granted in-app playback for this item.",
                )
            }
        }

        MediaOrigin.OFFICIAL_PROVIDER_LINK -> openOfficial(request.access)
        MediaOrigin.UNKNOWN -> MediaActionDecision.Blocked(
            reason = "The media source cannot be verified.",
        )
    }

    private fun decideDownload(request: MediaActionRequest): MediaActionDecision {
        if (request.origin == MediaOrigin.LOCAL_FILE) {
            return MediaActionDecision.Blocked("This item is already stored on the device.")
        }

        if (request.origin == MediaOrigin.OFFICIAL_PROVIDER_LINK) {
            return if (request.access.officialUri != null) {
                openOfficial(request.access)
            } else {
                MediaActionDecision.Blocked(
                    "Offline access is controlled by the official provider application.",
                )
            }
        }

        val permission = request.access.downloadPermission
        return if (permission.allowed && request.access.providerCapabilityGranted) {
            MediaActionDecision.ExecuteInApp
        } else {
            MediaActionDecision.Blocked(permission.reason)
        }
    }

    private fun decideShare(request: MediaActionRequest): MediaActionDecision = when (request.origin) {
        MediaOrigin.UNKNOWN -> MediaActionDecision.Blocked(
            reason = "The media source cannot be verified for sharing.",
        )
        else -> MediaActionDecision.ExecuteInApp
    }

    private fun decideRecognition(access: ProviderAccessContext): MediaActionDecision = when {
        !access.userConsentGranted -> MediaActionDecision.Blocked(
            reason = "Microphone consent is required before recognition starts.",
        )
        !access.providerCapabilityGranted -> MediaActionDecision.Blocked(
            reason = "The configured provider does not support music recognition.",
        )
        else -> MediaActionDecision.ExecuteInApp
    }

    private fun decideRoom(access: ProviderAccessContext): MediaActionDecision = when {
        !access.providerCapabilityGranted -> MediaActionDecision.Blocked(
            reason = "The configured service does not support listening rooms.",
        )
        !access.participantAccessConfirmed -> MediaActionDecision.Blocked(
            reason = "Legal provider access must be confirmed for every participant.",
        )
        else -> MediaActionDecision.ExecuteInApp
    }

    private fun openOfficial(access: ProviderAccessContext): MediaActionDecision =
        access.officialUri
            ?.takeIf(String::isNotBlank)
            ?.let {
                MediaActionDecision.OpenOfficialProvider(
                    uri = it,
                    reason = "Playback or offline access is controlled by the official provider.",
                )
            }
            ?: MediaActionDecision.Blocked(
                reason = "No official provider destination is available for this item.",
            )

    private companion object {
        val REMOTE_ORIGINS = setOf(
            MediaOrigin.USER_CLOUD,
            MediaOrigin.LICENSED_CATALOGUE,
            MediaOrigin.OFFICIAL_PROVIDER_LINK,
        )

        val PROVIDER_OPERATIONS = setOf(
            MediaOperation.RECOGNISE,
            MediaOperation.CREATE_ROOM,
            MediaOperation.JOIN_ROOM,
        )
    }
}
