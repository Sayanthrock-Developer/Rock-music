package com.rockmusic.app.domain.integration

import kotlinx.coroutines.flow.Flow

sealed interface ProviderCallResult<out T> {
    data class Success<T>(val value: T) : ProviderCallResult<T>

    data class Unavailable(
        val availability: IntegrationAvailability,
    ) : ProviderCallResult<Nothing>

    data class Failure(
        val message: String,
        val retryable: Boolean = true,
    ) : ProviderCallResult<Nothing>
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

interface SpotifyPkceService : IntegrationGateway {
    fun createAuthorizationUri(
        codeChallenge: String,
        state: String,
    ): ProviderCallResult<String>

    suspend fun exchangeAuthorizationCode(
        code: String,
        codeVerifier: String,
    ): ProviderCallResult<Unit>

    suspend fun importPlaylist(playlistUrl: String): ProviderCallResult<PlaylistImportSummary>
}

data class RecognitionMatch(
    val track: ProviderTrack,
    val confidence: Double,
    val providerResultId: String,
)

interface EchoFindProvider : IntegrationGateway {
    suspend fun identify(
        audioSample: ByteArray,
        mimeType: String,
    ): ProviderCallResult<RecognitionMatch>

    suspend fun deleteRecognitionHistory(): ProviderCallResult<Unit>
}

data class ListeningRoom(
    val id: String,
    val inviteCode: String,
    val hostUserId: String,
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

interface ListenTogetherBackend : IntegrationGateway {
    suspend fun createRoom(): ProviderCallResult<ListeningRoom>
    suspend fun joinRoom(inviteCode: String): ProviderCallResult<ListeningRoom>
    suspend fun leaveRoom(roomId: String): ProviderCallResult<Unit>
    suspend fun sendEvent(roomId: String, event: ListeningRoomEvent): ProviderCallResult<Unit>
    fun observeEvents(roomId: String): Flow<ProviderCallResult<ListeningRoomEvent>>
}

data class DiscordActivity(
    val title: String,
    val artist: String,
    val album: String? = null,
    val startedAtEpochMs: Long? = null,
    val officialUri: String? = null,
)

interface DiscordActivityProvider : IntegrationGateway {
    suspend fun connect(): ProviderCallResult<Unit>
    suspend fun publish(activity: DiscordActivity): ProviderCallResult<Unit>
    suspend fun clear(): ProviderCallResult<Unit>
    suspend fun disconnect(): ProviderCallResult<Unit>
}

data class CataloguePage(
    val items: List<ProviderTrack>,
    val nextPageToken: String? = null,
)

interface LicensedCatalogueProvider : IntegrationGateway {
    suspend fun search(query: String, pageToken: String? = null): ProviderCallResult<CataloguePage>
    suspend fun playbackAccess(providerId: String): ProviderCallResult<ProviderTrack>
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

interface LyricsProvider : IntegrationGateway {
    suspend fun lyricsFor(track: ProviderTrack): ProviderCallResult<List<SynchronizedLyricLine>>
}

data class PodcastSearchItem(
    val title: String,
    val publisher: String,
    val feedUrl: String,
    val artworkUrl: String? = null,
)

interface PodcastSearchProvider : IntegrationGateway {
    suspend fun searchPodcasts(query: String): ProviderCallResult<List<PodcastSearchItem>>
}

data class DownloadGrant(
    val mediaId: String,
    val downloadUrl: String,
    val expiresAtEpochMs: Long?,
    val expectedSha256: String? = null,
)

interface PermittedDownloadProvider : IntegrationGateway {
    suspend fun requestGrant(mediaId: String): ProviderCallResult<DownloadGrant>
    suspend fun revalidate(grant: DownloadGrant): ProviderCallResult<DownloadGrant>
}

data class CloudFile(
    val id: String,
    val name: String,
    val mimeType: String,
    val sizeBytes: Long?,
    val modifiedAtEpochMs: Long?,
)

interface CloudStorageProvider : IntegrationGateway {
    fun createAuthorizationUri(state: String): ProviderCallResult<String>
    suspend fun exchangeAuthorizationCode(code: String): ProviderCallResult<Unit>
    suspend fun listAudioFiles(pageToken: String? = null): ProviderCallResult<List<CloudFile>>
    suspend fun playbackAccess(fileId: String): ProviderCallResult<ProviderTrack>
    suspend fun downloadGrant(fileId: String): ProviderCallResult<DownloadGrant>
}

interface OfficialPlaybackProvider : IntegrationGateway {
    fun openTrack(trackUri: String): ProviderCallResult<String>
    fun openSearch(query: String): ProviderCallResult<String>
}
