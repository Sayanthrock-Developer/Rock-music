package com.rockmusic.app.domain.integration

import java.net.URI
import kotlinx.coroutines.flow.Flow

sealed interface ProviderCallResult<out T> {
    data class Success<T>(val value: T) : ProviderCallResult<T>
    data class Unavailable(val availability: IntegrationAvailability) : ProviderCallResult<Nothing>
    data class Failure(
        val message: String,
        val retryable: Boolean = true,
    ) : ProviderCallResult<Nothing>
}

fun <T, R> ProviderCallResult<T>.mapValue(transform: (T) -> R): ProviderCallResult<R> = when (this) {
    is ProviderCallResult.Success -> ProviderCallResult.Success(transform(value))
    is ProviderCallResult.Unavailable -> this
    is ProviderCallResult.Failure -> this
}

data class ProviderTrack(
    val providerId: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationMs: Long? = null,
    val artworkUrl: String? = null,
    val officialUri: String? = null,
)

data class PlaylistImportSummary(
    val playlistName: String,
    val matched: List<ProviderTrack>,
    val uncertain: List<ProviderTrack>,
    val unavailable: List<ProviderTrack>,
)

data class SpotifyTokenExchangeRequest(
    val code: String,
    val codeVerifier: String,
    val redirectUri: String,
    val requestedAtEpochMs: Long,
)

interface SpotifyPkceService : IntegrationGateway {
    fun createAuthorizationUri(codeChallenge: String, state: String): ProviderCallResult<String>
    suspend fun exchangeAuthorizationCode(code: String, codeVerifier: String): ProviderCallResult<Unit>
    suspend fun exchangeAuthorizationCode(request: SpotifyTokenExchangeRequest): ProviderCallResult<Unit> =
        exchangeAuthorizationCode(request.code, request.codeVerifier)
    suspend fun importPlaylist(playlistUrl: String): ProviderCallResult<PlaylistImportSummary>
}

data class RecognitionConsent(
    val grantedAtEpochMs: Long,
    val privacyNoticeVersion: String,
    val allowProviderHistory: Boolean = false,
)

data class EchoFindRecognitionRequest(
    val audioSample: ByteArray,
    val mimeType: String,
    val durationMs: Long,
    val sampleSha256: String,
    val locale: String? = null,
    val consent: RecognitionConsent,
)

data class RecognitionMatch(
    val track: ProviderTrack,
    val confidence: Double,
    val providerResultId: String,
    val providerName: String? = null,
    val termsUrl: String? = null,
    val historyEntryId: String? = null,
)

interface EchoFindProvider : IntegrationGateway {
    suspend fun identify(audioSample: ByteArray, mimeType: String): ProviderCallResult<RecognitionMatch>
    suspend fun identify(request: EchoFindRecognitionRequest): ProviderCallResult<RecognitionMatch> =
        identify(request.audioSample, request.mimeType)
    suspend fun deleteRecognitionHistory(): ProviderCallResult<Unit>
}

data class ListeningRoom(
    val id: String,
    val inviteCode: String,
    val hostUserId: String,
    val revision: Long = 0L,
    val websocketResumeToken: String? = null,
)

data class ListeningRoomPolicy(
    val maxParticipants: Int = 8,
    val chatEnabled: Boolean = true,
    val reactionsEnabled: Boolean = true,
    val hostOnlyQueueChanges: Boolean = true,
)

data class CreateListeningRoomRequest(
    val displayName: String,
    val policy: ListeningRoomPolicy = ListeningRoomPolicy(),
)

data class JoinListeningRoomRequest(
    val inviteCode: String,
    val displayName: String,
)

sealed interface ListeningRoomEvent {
    data class Playback(
        val mediaId: String,
        val positionMs: Long,
        val playing: Boolean,
        val serverTimestampMs: Long,
    ) : ListeningRoomEvent

    data class QueueChanged(val mediaIds: List<String>) : ListeningRoomEvent
    data class ChatMessage(val userId: String, val message: String) : ListeningRoomEvent
    data class Reaction(val userId: String, val emoji: String) : ListeningRoomEvent
    data class HostTransferred(val newHostUserId: String) : ListeningRoomEvent
}

data class ListeningRoomEventEnvelope(
    val roomId: String,
    val eventId: String,
    val sequence: Long,
    val senderUserId: String,
    val serverTimestampMs: Long,
    val event: ListeningRoomEvent,
)

interface ListenTogetherRestContract : IntegrationGateway {
    suspend fun createRoom(request: CreateListeningRoomRequest): ProviderCallResult<ListeningRoom>
    suspend fun joinRoom(request: JoinListeningRoomRequest): ProviderCallResult<ListeningRoom>
    suspend fun leaveRoom(roomId: String): ProviderCallResult<Unit>
}

interface ListenTogetherRealtimeContract : IntegrationGateway {
    suspend fun sendEvent(
        roomId: String,
        expectedRevision: Long,
        event: ListeningRoomEvent,
    ): ProviderCallResult<ListeningRoomEventEnvelope>

    fun observeEvents(
        roomId: String,
        resumeAfterSequence: Long? = null,
    ): Flow<ProviderCallResult<ListeningRoomEventEnvelope>>
}

interface ListenTogetherBackend : IntegrationGateway {
    suspend fun createRoom(): ProviderCallResult<ListeningRoom>
    suspend fun joinRoom(inviteCode: String): ProviderCallResult<ListeningRoom>
    suspend fun leaveRoom(roomId: String): ProviderCallResult<Unit>
    suspend fun sendEvent(roomId: String, event: ListeningRoomEvent): ProviderCallResult<Unit>
    fun observeEvents(roomId: String): Flow<ProviderCallResult<ListeningRoomEvent>>
}

data class DiscordActivityConfiguration(
    val clientId: String,
    val redirectUri: String,
    val backendBaseUrl: String,
    val enabledByUser: Boolean,
    val shareOfficialLinks: Boolean = true,
)

data class DiscordActivity(
    val title: String,
    val artist: String,
    val album: String? = null,
    val startedAtEpochMs: Long? = null,
    val officialUri: String? = null,
)

interface DiscordActivityProvider : IntegrationGateway {
    suspend fun configure(configuration: DiscordActivityConfiguration): ProviderCallResult<Unit> =
        ProviderCallResult.Failure(
            message = "Discord activity configuration is not implemented by this provider",
            retryable = false,
        )
    suspend fun connect(): ProviderCallResult<Unit>
    suspend fun publish(activity: DiscordActivity): ProviderCallResult<Unit>
    suspend fun clear(): ProviderCallResult<Unit>
    suspend fun disconnect(): ProviderCallResult<Unit>
}

data class CatalogueSearchRequest(
    val query: String,
    val pageSize: Int = 25,
    val pageToken: String? = null,
    val market: String? = null,
    val includeExplicit: Boolean = true,
)

data class CataloguePage(
    val items: List<ProviderTrack>,
    val nextPageToken: String? = null,
)

data class CataloguePlaybackRequest(
    val providerId: String,
    val market: String? = null,
    val requestedAtEpochMs: Long,
)

data class CataloguePlaybackGrant(
    val track: ProviderTrack,
    val playbackUri: String,
    val entitlementId: String,
    val expiresAtEpochMs: Long?,
    val officialHandoffRequired: Boolean,
)

interface LicensedCatalogueProvider : IntegrationGateway {
    suspend fun search(query: String, pageToken: String? = null): ProviderCallResult<CataloguePage>
    suspend fun search(request: CatalogueSearchRequest): ProviderCallResult<CataloguePage> =
        search(request.query, request.pageToken)
    suspend fun playbackAccess(providerId: String): ProviderCallResult<ProviderTrack>
    suspend fun playbackAccess(
        request: CataloguePlaybackRequest,
    ): ProviderCallResult<CataloguePlaybackGrant> = playbackAccess(request.providerId).mapValue { track ->
        CataloguePlaybackGrant(
            track = track,
            playbackUri = track.officialUri.orEmpty(),
            entitlementId = request.providerId,
            expiresAtEpochMs = null,
            officialHandoffRequired = true,
        )
    }
}

data class SynchronizedLyricWord(
    val text: String,
    val startMs: Long,
    val endMs: Long,
)

data class SynchronizedLyricLine(
    val text: String,
    val startMs: Long,
    val endMs: Long?,
    val translation: String? = null,
    val words: List<SynchronizedLyricWord> = emptyList(),
)

data class LyricsRequest(
    val track: ProviderTrack,
    val language: String? = null,
    val translationLanguage: String? = null,
    val includeWordTiming: Boolean = true,
)

data class SynchronizedLyricsDocument(
    val providerName: String,
    val providerTrackId: String,
    val language: String?,
    val lines: List<SynchronizedLyricLine>,
    val attribution: String? = null,
    val termsUrl: String? = null,
)

interface LyricsProvider : IntegrationGateway {
    suspend fun lyricsFor(track: ProviderTrack): ProviderCallResult<List<SynchronizedLyricLine>>
    suspend fun lyricsFor(request: LyricsRequest): ProviderCallResult<SynchronizedLyricsDocument> =
        lyricsFor(request.track).mapValue { lines ->
            SynchronizedLyricsDocument(
                providerName = id.name,
                providerTrackId = request.track.providerId,
                language = request.language,
                lines = lines,
            )
        }
}

data class PodcastSearchItem(
    val title: String,
    val publisher: String,
    val feedUrl: String,
    val artworkUrl: String? = null,
    val providerId: String? = null,
    val description: String? = null,
)

data class PodcastSearchRequest(
    val query: String,
    val pageSize: Int = 25,
    val pageToken: String? = null,
    val language: String? = null,
    val explicitAllowed: Boolean = true,
)

data class PodcastSearchPage(
    val items: List<PodcastSearchItem>,
    val nextPageToken: String? = null,
)

interface PodcastSearchProvider : IntegrationGateway {
    suspend fun searchPodcasts(query: String): ProviderCallResult<List<PodcastSearchItem>>
    suspend fun searchPodcasts(request: PodcastSearchRequest): ProviderCallResult<PodcastSearchPage> =
        searchPodcasts(request.query).mapValue { items ->
            PodcastSearchPage(items = items.take(request.pageSize.coerceIn(1, 100)))
        }
}

data class DownloadItemRef(
    val providerId: String,
    val mediaId: String,
    val accountId: String? = null,
)

data class DownloadGrantPermission(
    val permitted: Boolean,
    val reason: String? = null,
    val canRedownload: Boolean = false,
    val maximumDownloads: Int? = null,
)

data class DownloadGrant(
    val mediaId: String,
    val downloadUrl: String,
    val expiresAtEpochMs: Long?,
    val expectedSha256: String? = null,
    val grantId: String = mediaId,
    val providerId: String = "",
    val issuedAtEpochMs: Long? = null,
    val notBeforeEpochMs: Long? = null,
    val mimeType: String? = null,
    val contentLengthBytes: Long? = null,
    val revalidationToken: String? = null,
    val permission: DownloadGrantPermission = DownloadGrantPermission(permitted = false),
)

data class DownloadGrantRequest(
    val item: DownloadItemRef,
    val requestedAtEpochMs: Long,
    val deviceBindingId: String? = null,
)

data class DownloadGrantRevalidationRequest(
    val item: DownloadItemRef,
    val grant: DownloadGrant,
    val checkedAtEpochMs: Long,
)

sealed interface DownloadGrantValidation {
    data object Valid : DownloadGrantValidation
    data class Denied(val reason: String) : DownloadGrantValidation
    data object NotYetValid : DownloadGrantValidation
    data object Expired : DownloadGrantValidation
    data object ItemMismatch : DownloadGrantValidation
    data object InvalidTransport : DownloadGrantValidation
    data object InvalidDigest : DownloadGrantValidation
}

object DownloadGrantValidator {
    private val sha256Pattern = Regex("^[a-fA-F0-9]{64}$")

    fun validate(
        grant: DownloadGrant,
        item: DownloadItemRef,
        nowEpochMs: Long,
    ): DownloadGrantValidation {
        if (!grant.permission.permitted) {
            return DownloadGrantValidation.Denied(
                grant.permission.reason ?: "The provider did not permit this item for download",
            )
        }
        if (
            grant.providerId.isBlank() ||
            item.providerId.isBlank() ||
            grant.mediaId != item.mediaId ||
            grant.providerId != item.providerId
        ) {
            return DownloadGrantValidation.ItemMismatch
        }
        if (grant.notBeforeEpochMs?.let { nowEpochMs < it } == true) {
            return DownloadGrantValidation.NotYetValid
        }
        if (grant.expiresAtEpochMs?.let { nowEpochMs >= it } == true) {
            return DownloadGrantValidation.Expired
        }
        val uri = runCatching { URI(grant.downloadUrl) }.getOrNull()
        if (uri?.scheme?.equals("https", ignoreCase = true) != true || uri.host.isNullOrBlank()) {
            return DownloadGrantValidation.InvalidTransport
        }
        if (grant.expectedSha256?.let(sha256Pattern::matches) == false) {
            return DownloadGrantValidation.InvalidDigest
        }
        return DownloadGrantValidation.Valid
    }
}

interface PermittedDownloadProvider : IntegrationGateway {
    suspend fun requestGrant(mediaId: String): ProviderCallResult<DownloadGrant>
    suspend fun requestGrant(request: DownloadGrantRequest): ProviderCallResult<DownloadGrant> =
        requestGrant(request.item.mediaId)
    suspend fun revalidate(grant: DownloadGrant): ProviderCallResult<DownloadGrant>
    suspend fun revalidate(request: DownloadGrantRevalidationRequest): ProviderCallResult<DownloadGrant> =
        revalidate(request.grant)
}

data class CloudFile(
    val id: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long?,
    val modifiedAtEpochMs: Long?,
)

data class CloudAuthorizationRequest(
    val authorizationUri: String,
    val state: String,
    val redirectUri: String,
    val codeVerifier: String? = null,
    val codeChallenge: String? = null,
    val createdAtEpochMs: Long,
    val expiresAtEpochMs: Long,
)

data class CloudPlaybackAccess(
    val file: CloudFile,
    val playbackUri: String,
    val expiresAtEpochMs: Long?,
)

interface CloudStorageProvider : IntegrationGateway {
    fun createAuthorizationUri(state: String): ProviderCallResult<String>
    fun createAuthorizationRequest(
        state: String,
        redirectUri: String,
        createdAtEpochMs: Long,
        expiresAtEpochMs: Long,
    ): ProviderCallResult<CloudAuthorizationRequest> = createAuthorizationUri(state).mapValue { uri ->
        CloudAuthorizationRequest(
            authorizationUri = uri,
            state = state,
            redirectUri = redirectUri,
            createdAtEpochMs = createdAtEpochMs,
            expiresAtEpochMs = expiresAtEpochMs,
        )
    }
    suspend fun exchangeAuthorizationCode(code: String): ProviderCallResult<Unit>
    suspend fun listAudioFiles(pageToken: String? = null): ProviderCallResult<List<CloudFile>>
    suspend fun playbackAccess(fileId: String): ProviderCallResult<ProviderTrack>
    suspend fun downloadGrant(fileId: String): ProviderCallResult<DownloadGrant>
}

enum class OfficialRouteKind {
    VIDEO,
    PLAYLIST,
    SEARCH,
}

data class OfficialProviderRoute(
    val webUri: String,
    val androidAppUri: String?,
    val preferredPackages: List<String>,
    val kind: OfficialRouteKind,
    val providerMediaId: String? = null,
)

interface OfficialPlaybackProvider : IntegrationGateway {
    fun openTrack(trackUri: String): ProviderCallResult<String>
    fun openSearch(query: String): ProviderCallResult<String>
    fun routeTrack(trackUri: String): ProviderCallResult<OfficialProviderRoute> =
        openTrack(trackUri).mapValue { uri ->
            OfficialProviderRoute(
                webUri = uri,
                androidAppUri = null,
                preferredPackages = emptyList(),
                kind = OfficialRouteKind.VIDEO,
            )
        }
    fun routeSearch(query: String): ProviderCallResult<OfficialProviderRoute> =
        openSearch(query).mapValue { uri ->
            OfficialProviderRoute(
                webUri = uri,
                androidAppUri = null,
                preferredPackages = emptyList(),
                kind = OfficialRouteKind.SEARCH,
            )
        }
}
