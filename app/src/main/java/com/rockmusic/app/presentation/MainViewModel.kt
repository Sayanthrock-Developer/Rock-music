package com.rockmusic.app.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rockmusic.app.data.integration.NetworkStatusProvider
import com.rockmusic.app.data.integration.OfficialProviderRouteLauncher
import com.rockmusic.app.data.integration.YouTubeOfficialPlaybackProvider
import com.rockmusic.app.data.local.LocalMusicRepository
import com.rockmusic.app.domain.integration.IntegrationAvailability
import com.rockmusic.app.domain.integration.OfficialProviderRoute
import com.rockmusic.app.domain.integration.ProviderCallResult
import com.rockmusic.app.domain.model.LocalTrack
import com.rockmusic.app.domain.policy.MediaActionDecision
import com.rockmusic.app.domain.policy.MediaActionPolicyEngine
import com.rockmusic.app.domain.policy.MediaActionRequest
import com.rockmusic.app.domain.policy.MediaOperation
import com.rockmusic.app.domain.policy.MediaOrigin
import com.rockmusic.app.domain.policy.ProviderAccessContext
import com.rockmusic.app.player.PlayerConnection
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

@HiltViewModel
class MainViewModel @Inject constructor(
    private val localMusicRepository: LocalMusicRepository,
    private val playerConnection: PlayerConnection,
    private val mediaActionPolicy: MediaActionPolicyEngine,
    private val youTubeProvider: YouTubeOfficialPlaybackProvider,
    private val officialRouteLauncher: OfficialProviderRouteLauncher,
    private val networkStatusProvider: NetworkStatusProvider,
) : ViewModel() {
    val playerState = playerConnection.state

    private val _libraryState = MutableStateFlow(LocalLibraryState())
    val libraryState: StateFlow<LocalLibraryState> = _libraryState.asStateFlow()

    private val _lastActionDecision = MutableStateFlow<MediaActionDecision?>(null)
    val lastActionDecision: StateFlow<MediaActionDecision?> = _lastActionDecision.asStateFlow()

    private val _youTubeState = MutableStateFlow(YouTubeHomeState())
    val youTubeState: StateFlow<YouTubeHomeState> = _youTubeState.asStateFlow()

    fun loadLocalMusic() {
        scanDeviceMusic(
            loadingMessage = "Scanning songs on this phone…",
            successMessage = { count -> "Found $count songs on this phone." },
            autoPlay = false,
        )
    }

    fun rescanDeviceLibrary() {
        scanDeviceMusic(
            loadingMessage = "Applying folder exclusions and rescanning…",
            successMessage = { count -> "Library updated with $count included songs." },
            autoPlay = false,
        )
    }

    fun addAllSongs(autoPlay: Boolean = true) {
        scanDeviceMusic(
            loadingMessage = "Scanning and adding all included songs…",
            successMessage = { count -> "Added $count included songs." },
            autoPlay = autoPlay,
        )
    }

    private fun scanDeviceMusic(
        loadingMessage: String,
        successMessage: (Int) -> String,
        autoPlay: Boolean,
    ) {
        if (_libraryState.value.isLoading || _libraryState.value.isImporting) return
        viewModelScope.launch {
            _libraryState.value = _libraryState.value.copy(
                isLoading = true,
                error = null,
                statusMessage = loadingMessage,
            )
            runCatching { localMusicRepository.scan() }
                .onSuccess { scanned ->
                    val current = _libraryState.value
                    val manuallyImported = current.tracks.filterNot { track ->
                        track.mediaUri in current.deviceTrackUris
                    }
                    _libraryState.value = current.copy(
                        tracks = mergeTracks(manuallyImported, scanned),
                        deviceTrackUris = scanned.mapTo(mutableSetOf(), LocalTrack::mediaUri),
                        isLoading = false,
                        error = null,
                        statusMessage = successMessage(scanned.size),
                    )
                    if (autoPlay) playAll(scanned)
                }
                .onFailure { error ->
                    _libraryState.value = _libraryState.value.copy(
                        isLoading = false,
                        error = error.message ?: "Unable to scan local music",
                        statusMessage = null,
                    )
                }
        }
    }

    fun openDownloadedAudio(uri: Uri) {
        addDownloadedAudio(listOf(uri), autoPlay = true)
    }

    fun addDownloadedAudio(uris: List<Uri>, autoPlay: Boolean = true) {
        if (uris.isEmpty() || _libraryState.value.isImporting) return
        viewModelScope.launch {
            _libraryState.value = _libraryState.value.copy(
                isImporting = true,
                error = null,
                statusMessage = "Opening ${uris.size} audio file${if (uris.size == 1) "" else "s"}…",
            )
            runCatching { localMusicRepository.resolveAll(uris) }
                .onSuccess { imported ->
                    _libraryState.value = _libraryState.value.copy(
                        tracks = mergeTracks(_libraryState.value.tracks, imported),
                        isImporting = false,
                        error = null,
                        statusMessage = "Added ${imported.size} downloaded song${if (imported.size == 1) "" else "s"}.",
                    )
                    if (autoPlay) playAll(imported)
                }
                .onFailure {
                    _libraryState.value = _libraryState.value.copy(
                        isImporting = false,
                        error = it.message ?: "Unable to open the downloaded audio files",
                        statusMessage = null,
                    )
                }
        }
    }

    fun openSongFolder(treeUri: Uri, autoPlay: Boolean = true) {
        if (_libraryState.value.isImporting) return
        viewModelScope.launch {
            _libraryState.value = _libraryState.value.copy(
                isImporting = true,
                error = null,
                statusMessage = "Scanning the selected song folder…",
            )
            runCatching { localMusicRepository.scanFolder(treeUri) }
                .onSuccess { imported ->
                    _libraryState.value = _libraryState.value.copy(
                        tracks = mergeTracks(_libraryState.value.tracks, imported),
                        isImporting = false,
                        error = null,
                        statusMessage = "Added ${imported.size} songs from the selected folder.",
                    )
                    if (autoPlay) playAll(imported)
                }
                .onFailure {
                    _libraryState.value = _libraryState.value.copy(
                        isImporting = false,
                        error = it.message ?: "Unable to scan the selected song folder",
                        statusMessage = null,
                    )
                }
        }
    }

    fun openOfficialYouTubeSearch(query: String) {
        launchOfficialRoute(youTubeProvider.routeSearch(query))
    }

    fun openOfficialYouTubeLink(link: String) {
        launchOfficialRoute(youTubeProvider.routeTrack(link))
    }

    fun clearYouTubeStatus() {
        if (!_youTubeState.value.isLaunching) {
            _youTubeState.value = YouTubeHomeState()
        }
    }

    private fun launchOfficialRoute(result: ProviderCallResult<OfficialProviderRoute>) {
        if (_youTubeState.value.isLaunching) return
        _youTubeState.value = YouTubeHomeState(isLaunching = true)

        viewModelScope.launch {
            // Allow Compose to display the progress state before provider checks and intent launch.
            yield()
            when (result) {
                is ProviderCallResult.Success -> launchValidatedOfficialRoute(result.value)
                is ProviderCallResult.Failure -> {
                    _youTubeState.value = YouTubeHomeState(error = result.message)
                }
                is ProviderCallResult.Unavailable -> {
                    _youTubeState.value = YouTubeHomeState(
                        error = "Official YouTube routing is currently unavailable.",
                    )
                }
            }
        }
    }

    private suspend fun launchValidatedOfficialRoute(route: OfficialProviderRoute) {
        val availability = runCatching { youTubeProvider.availability() }
            .getOrElse { error ->
                _youTubeState.value = YouTubeHomeState(
                    error = error.message ?: "Unable to check YouTube provider readiness.",
                )
                return
            }
        val capabilities = runCatching { youTubeProvider.capabilities() }
            .getOrElse { error ->
                _youTubeState.value = YouTubeHomeState(
                    error = error.message ?: "Unable to check YouTube provider capabilities.",
                )
                return
            }
        val capabilityGranted =
            availability is IntegrationAvailability.Available && capabilities.canOpenOfficialPlayback
        val access = availability.toProviderAccessContext(
            online = networkStatusProvider.isOnline(),
            capabilityGranted = capabilityGranted,
            officialUri = route.webUri,
        )
        val decision = mediaActionPolicy.decide(route.toOfficialOpenRequest(access))
        _lastActionDecision.value = decision

        if (!capabilityGranted && decision is MediaActionDecision.OpenOfficialProvider) {
            _youTubeState.value = YouTubeHomeState(
                error = "The official YouTube provider does not currently grant playback handoff.",
            )
            return
        }

        when (decision) {
            is MediaActionDecision.OpenOfficialProvider -> {
                officialRouteLauncher.launch(route)
                    .onSuccess { target ->
                        _youTubeState.value = YouTubeHomeState(
                            message = if (target.packageName == null) {
                                "Opened the validated YouTube destination in your browser."
                            } else {
                                "Opened the validated destination in an official YouTube app."
                            },
                        )
                    }
                    .onFailure { error ->
                        _youTubeState.value = YouTubeHomeState(
                            error = error.message
                                ?: "No official YouTube app or browser could open this destination.",
                        )
                    }
            }
            else -> {
                _youTubeState.value = YouTubeHomeState(error = decision.userMessage())
            }
        }
    }

    fun play(track: LocalTrack) {
        playAll(listOf(track))
    }

    fun playAll(tracks: List<LocalTrack>) {
        val decisions = tracks.asSequence().distinctBy(LocalTrack::mediaUri)
            .map { track -> track to mediaActionPolicy.decide(track.toPlayRequest()) }
            .toList()

        _lastActionDecision.value = decisions
            .firstOrNull { (_, decision) -> decision != MediaActionDecision.ExecuteInApp }
            ?.second
            ?: decisions.firstOrNull()?.second

        val approvedTracks = decisions
            .filter { (_, decision) -> decision == MediaActionDecision.ExecuteInApp }
            .map(Pair<LocalTrack, MediaActionDecision>::first)

        if (approvedTracks.isNotEmpty()) {
            playerConnection.playQueue(approvedTracks)
        }
    }

    fun togglePlayPause() = playerConnection.togglePlayPause()
    fun seekTo(positionMs: Long) = playerConnection.seekTo(positionMs)
    fun skipNext() = playerConnection.skipNext()
    fun skipPrevious() = playerConnection.skipPrevious()
    fun clearPlayerError() = playerConnection.clearError()

    private fun LocalTrack.toPlayRequest() = MediaActionRequest(
        operation = MediaOperation.PLAY,
        origin = MediaOrigin.LOCAL_FILE,
        mediaId = id.toString(),
        sourceUri = mediaUri,
    )

    private fun OfficialProviderRoute.toOfficialOpenRequest(
        access: ProviderAccessContext,
    ) = MediaActionRequest(
        operation = MediaOperation.OPEN_OFFICIAL_PROVIDER,
        origin = MediaOrigin.OFFICIAL_PROVIDER_LINK,
        mediaId = providerMediaId ?: webUri,
        sourceUri = webUri,
        access = access,
    )

    private fun IntegrationAvailability.toProviderAccessContext(
        online: Boolean,
        capabilityGranted: Boolean,
        officialUri: String,
    ) = ProviderAccessContext(
        providerName = "YouTube / YouTube Music",
        missingConfigurationKeys = (this as? IntegrationAvailability.Unconfigured)
            ?.missingKeys
            .orEmpty(),
        authenticationRequired = this is IntegrationAvailability.AuthenticationRequired,
        authenticated = this !is IntegrationAvailability.AuthenticationRequired,
        online = online && this !is IntegrationAvailability.Offline,
        providerCapabilityGranted = capabilityGranted,
        requiresOfficialClient = true,
        officialUri = officialUri.takeIf { capabilityGranted },
    )

    private fun MediaActionDecision.userMessage(): String = when (this) {
        MediaActionDecision.ExecuteInApp ->
            "This official provider action cannot run inside Rock Music."
        is MediaActionDecision.OpenOfficialProvider -> reason
        is MediaActionDecision.RequireConfiguration ->
            "Required provider configuration is missing: ${missingKeys.joinToString()}."
        is MediaActionDecision.RequireAuthentication ->
            "Sign in to $providerName before continuing."
        MediaActionDecision.Offline -> "Connect to the internet and try again."
        is MediaActionDecision.Blocked -> reason
    }

    private fun mergeTracks(
        existing: List<LocalTrack>,
        incoming: List<LocalTrack>,
    ): List<LocalTrack> = (incoming + existing).distinctBy(LocalTrack::mediaUri)
}

data class LocalLibraryState(
    val tracks: List<LocalTrack> = emptyList(),
    val deviceTrackUris: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isImporting: Boolean = false,
    val error: String? = null,
    val statusMessage: String? = null,
)

data class YouTubeHomeState(
    val isLaunching: Boolean = false,
    val message: String? = null,
    val error: String? = null,
)
