package com.rockmusic.app.presentation.integrations

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rockmusic.app.data.integration.EncryptedSpotifyPkceRequestStore
import com.rockmusic.app.data.integration.IntegrationRegistry
import com.rockmusic.app.data.integration.IntegrationSnapshot
import com.rockmusic.app.data.integration.ProviderConfigKey
import com.rockmusic.app.data.integration.SpotifyPkceRequestFactory
import com.rockmusic.app.data.integration.SpotifyPlaylistClient
import com.rockmusic.app.data.integration.SpotifyPlaylistPreview
import com.rockmusic.app.data.integration.SpotifyPlaylistReferenceParser
import com.rockmusic.app.data.integration.YouTubeOfficialPlaybackProvider
import com.rockmusic.app.domain.integration.IntegrationAvailability
import com.rockmusic.app.domain.integration.IntegrationId
import com.rockmusic.app.domain.integration.ProviderCallResult
import com.rockmusic.app.domain.integration.ProviderCapabilities
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@HiltViewModel
class IntegrationConnectionsViewModel @Inject constructor(
    private val registry: IntegrationRegistry,
    private val spotifyRequestFactory: SpotifyPkceRequestFactory,
    private val spotifyRequestStore: EncryptedSpotifyPkceRequestStore,
    private val spotifyPlaylistClient: SpotifyPlaylistClient,
    private val youtubeProvider: YouTubeOfficialPlaybackProvider,
) : ViewModel() {
    private val _state = MutableStateFlow(IntegrationConnectionsState())
    val state: StateFlow<IntegrationConnectionsState> = _state.asStateFlow()
    private val operationMutex = Mutex()

    init {
        refresh()
    }

    fun refresh() = runExclusive {
        loadSnapshots()
    }

    fun unlock(id: IntegrationId, values: Map<ProviderConfigKey, String>) = runExclusive {
        setBusy(id)
        registry.unlock(id, values)
            .onSuccess { loadSnapshots("${id.readableName()} unlocked securely.") }
            .onFailure { error ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    busyId = null,
                    error = error.message ?: "Unable to unlock this provider.",
                )
            }
    }

    fun lock(id: IntegrationId) = runExclusive {
        setBusy(id)
        registry.lock(id)
        if (id == IntegrationId.SPOTIFY) {
            _state.value = _state.value.copy(spotifyPlaylist = null)
        }
        loadSnapshots("${id.readableName()} locked. Encrypted configuration was retained.")
    }

    fun reset(id: IntegrationId) = runExclusive {
        setBusy(id)
        registry.reset(id)
        if (id == IntegrationId.SPOTIFY) {
            _state.value = _state.value.copy(spotifyPlaylist = null)
        }
        loadSnapshots("${id.readableName()} configuration and authorization were removed.")
    }

    fun activate(id: IntegrationId) = runExclusive {
        setBusy(id)
        when (id) {
            IntegrationId.SPOTIFY -> prepareSpotifyAuthorization()
            IntegrationId.OFFICIAL_YOUTUBE -> prepareYouTubeSearch()
            else -> {
                val availability = registry.gateway(id).availability()
                _state.value = _state.value.copy(
                    isLoading = false,
                    busyId = null,
                    message = availability.activationMessage(id),
                )
            }
        }
    }

    fun updateSpotifyPlaylistReference(value: String) {
        _state.value = _state.value.copy(
            spotifyPlaylistReference = value,
            spotifyPlaylist = null,
            error = null,
            message = null,
        )
    }

    fun loadSpotifyPlaylist() = runExclusive {
        val reference = _state.value.spotifyPlaylistReference
        setBusy(IntegrationId.SPOTIFY)
        spotifyPlaylistClient.load(reference)
            .onSuccess { playlist ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    busyId = null,
                    spotifyPlaylist = playlist,
                    spotifyPlaylistReference = SpotifyPlaylistReferenceParser.canonicalWebUrl(playlist.id),
                    message = "Loaded ${playlist.name} securely from Spotify.",
                    error = null,
                )
            }
            .onFailure { error ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    busyId = null,
                    error = error.message ?: "Unable to load the Spotify playlist.",
                )
            }
    }

    fun openSpotifyPlaylist() = runExclusive {
        setBusy(IntegrationId.SPOTIFY)
        SpotifyPlaylistReferenceParser.parse(_state.value.spotifyPlaylistReference)
            .onSuccess { playlistId ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    launchUri = SpotifyPlaylistReferenceParser.canonicalWebUrl(playlistId),
                    message = "Opening the playlist through the official Spotify route.",
                    error = null,
                )
            }
            .onFailure { error ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    busyId = null,
                    error = error.message ?: "The Spotify playlist link is invalid.",
                )
            }
    }

    fun launchConsumed(error: String? = null) {
        _state.value = _state.value.copy(
            launchUri = null,
            busyId = null,
            error = error,
        )
    }

    private fun runExclusive(block: suspend () -> Unit) {
        viewModelScope.launch {
            operationMutex.withLock { block() }
        }
    }

    private fun setBusy(id: IntegrationId) {
        _state.value = _state.value.copy(
            isLoading = false,
            busyId = id,
            error = null,
            message = null,
        )
    }

    private suspend fun prepareSpotifyAuthorization() {
        spotifyRequestStore.clearExpired(System.currentTimeMillis())
        spotifyRequestFactory.create(
            scopes = setOf("playlist-read-private", "playlist-read-collaborative"),
        ).onSuccess { request ->
            spotifyRequestStore.save(request)
            _state.value = _state.value.copy(
                isLoading = false,
                launchUri = request.authorizationUri,
                message = "A fresh Spotify PKCE request was generated and encrypted on this device.",
            )
        }.onFailure { error ->
            _state.value = _state.value.copy(
                isLoading = false,
                busyId = null,
                error = error.message ?: "Unable to create the Spotify authorization request.",
            )
        }
    }

    private fun prepareYouTubeSearch() {
        when (val route = youtubeProvider.routeSearch("Rock Music")) {
            is ProviderCallResult.Success -> {
                _state.value = _state.value.copy(
                    isLoading = false,
                    launchUri = route.value.webUri,
                    message = "Opening the validated official YouTube Music search route.",
                )
            }

            is ProviderCallResult.Failure -> {
                _state.value = _state.value.copy(
                    isLoading = false,
                    busyId = null,
                    error = route.message,
                )
            }

            is ProviderCallResult.Unavailable -> {
                _state.value = _state.value.copy(
                    isLoading = false,
                    busyId = null,
                    error = route.availability.activationMessage(IntegrationId.OFFICIAL_YOUTUBE),
                )
            }
        }
    }

    private suspend fun loadSnapshots(message: String? = null) {
        val busyId = _state.value.busyId
        _state.value = _state.value.copy(
            isLoading = busyId == null && _state.value.items.isEmpty(),
            error = null,
        )
        runCatching { registry.snapshots() }
            .onSuccess { snapshots ->
                val spotifyAvailable = snapshots.any { snapshot ->
                    snapshot.id == IntegrationId.SPOTIFY &&
                        snapshot.availability == IntegrationAvailability.Available
                }
                _state.value = _state.value.copy(
                    items = snapshots,
                    isLoading = false,
                    busyId = null,
                    message = message,
                    error = null,
                    spotifyPlaylist = _state.value.spotifyPlaylist.takeIf { spotifyAvailable },
                )
            }
            .onFailure { error ->
                _state.value = _state.value.copy(
                    isLoading = false,
                    busyId = null,
                    error = error.message ?: "Unable to read provider configuration",
                )
            }
    }
}

data class IntegrationConnectionsState(
    val items: List<IntegrationSnapshot> = emptyList(),
    val isLoading: Boolean = false,
    val busyId: IntegrationId? = null,
    val launchUri: String? = null,
    val message: String? = null,
    val error: String? = null,
    val spotifyPlaylistReference: String = DEFAULT_SPOTIFY_PLAYLIST_REFERENCE,
    val spotifyPlaylist: SpotifyPlaylistPreview? = null,
)

@Composable
fun IntegrationConnectionsScreen(
    onClose: () -> Unit,
    viewModel: IntegrationConnectionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var editing by remember { mutableStateOf<IntegrationSnapshot?>(null) }
    val spotifyAvailable = state.items.any { snapshot ->
        snapshot.id == IntegrationId.SPOTIFY &&
            snapshot.availability == IntegrationAvailability.Available
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(state.launchUri) {
        val uri = state.launchUri ?: return@LaunchedEffect
        val result = runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(uri)).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                },
            )
        }
        viewModel.launchConsumed(result.exceptionOrNull()?.message)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Connections & Unlock",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "Securely configure licensed providers and official playback routes.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close connections")
                }
            }
        }

        state.message?.let { message ->
            item {
                Card(Modifier.fillMaxWidth()) {
                    Text(message, modifier = Modifier.padding(16.dp))
                }
            }
        }

        state.error?.let { error ->
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Connection action failed", fontWeight = FontWeight.Bold)
                        Text(error)
                        OutlinedButton(onClick = viewModel::refresh) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Refresh status")
                        }
                    }
                }
            }
        }

        if (state.isLoading && state.items.isEmpty()) {
            item { CircularProgressIndicator() }
        } else {
            items(state.items, key = { it.id.name }) { snapshot ->
                IntegrationConnectionCard(
                    snapshot = snapshot,
                    isBusy = state.busyId == snapshot.id,
                    onConfigure = { editing = snapshot },
                    onLock = { viewModel.lock(snapshot.id) },
                    onReset = { viewModel.reset(snapshot.id) },
                    onActivate = { viewModel.activate(snapshot.id) },
                )
            }
        }

        if (spotifyAvailable) {
            item(key = "spotify-playlist-preview") {
                SpotifyPlaylistPreviewCard(
                    reference = state.spotifyPlaylistReference,
                    preview = state.spotifyPlaylist,
                    isBusy = state.busyId == IntegrationId.SPOTIFY,
                    onReferenceChange = viewModel::updateSpotifyPlaylistReference,
                    onLoad = viewModel::loadSpotifyPlaylist,
                    onOpen = viewModel::openSpotifyPlaylist,
                )
            }
        }

        item {
            HorizontalDivider()
            Text(
                text = "Only public mobile client IDs, registered HTTPS app links or exact Rock Music callback routes, publishable or Android-restricted keys, and licensed service URLs may be entered. OAuth client secrets, signing secrets, unrestricted keys, and privileged backend credentials must never be placed in the app.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }

    editing?.let { snapshot ->
        UnlockProviderDialog(
            snapshot = snapshot,
            onDismiss = { editing = null },
            onUnlock = { values ->
                editing = null
                viewModel.unlock(snapshot.id, values.filterValues(String::isNotBlank))
            },
        )
    }
}

@Composable
private fun IntegrationConnectionCard(
    snapshot: IntegrationSnapshot,
    isBusy: Boolean,
    onConfigure: () -> Unit,
    onLock: () -> Unit,
    onReset: () -> Unit,
    onActivate: () -> Unit,
) {
    val (status, detail) = snapshot.availability.toStatusText()
    val capabilityText = snapshot.capabilities.toReadableCapabilities()

    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (snapshot.isUnlocked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                    contentDescription = null,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = snapshot.displayName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = status,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(detail, style = MaterialTheme.typography.bodyMedium)
            if (capabilityText.isNotBlank()) {
                Text(
                    text = capabilityText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (snapshot.officialProviderOnly) {
                Text(
                    text = "Playback and offline access stay inside the official provider.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                when {
                    isBusy -> CircularProgressIndicator()
                    !snapshot.isUnlocked -> Button(onClick = onConfigure) {
                        Icon(Icons.Rounded.LockOpen, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (snapshot.canUnlockWithoutInput) "Unlock" else "Configure & unlock")
                    }

                    snapshot.id == IntegrationId.SPOTIFY -> Button(onClick = onActivate) {
                        Icon(Icons.Rounded.OpenInNew, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (snapshot.availability == IntegrationAvailability.Available) {
                                "Re-authorise"
                            } else {
                                "Authorise"
                            },
                        )
                    }

                    snapshot.id == IntegrationId.OFFICIAL_YOUTUBE -> Button(onClick = onActivate) {
                        Icon(Icons.Rounded.OpenInNew, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Open official search")
                    }

                    else -> OutlinedButton(onClick = onActivate) {
                        Text("Check readiness")
                    }
                }

                if (snapshot.isUnlocked && snapshot.id != IntegrationId.OFFICIAL_YOUTUBE) {
                    TextButton(onClick = onLock, enabled = !isBusy) { Text("Lock") }
                    if (snapshot.requiredConfiguration.isNotEmpty()) {
                        TextButton(onClick = onReset, enabled = !isBusy) { Text("Reset") }
                    }
                }
            }
        }
    }
}

@Composable
private fun UnlockProviderDialog(
    snapshot: IntegrationSnapshot,
    onDismiss: () -> Unit,
    onUnlock: (Map<ProviderConfigKey, String>) -> Unit,
) {
    val values = remember(snapshot.id) { mutableStateMapOf<ProviderConfigKey, String>() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Unlock ${snapshot.displayName}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (snapshot.canUnlockWithoutInput) {
                        "Encrypted or managed configuration is already available. Leave fields blank to unlock it, or enter replacement public values."
                    } else {
                        "Enter the public provider configuration required for this feature. Values are encrypted with Android Keystore."
                    },
                )
                snapshot.requiredConfiguration.forEach { key ->
                    OutlinedTextField(
                        value = values[key].orEmpty(),
                        onValueChange = { values[key] = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(key.readableLabel()) },
                        singleLine = true,
                        visualTransformation = if (key.isSensitiveInput()) {
                            PasswordVisualTransformation()
                        } else {
                            VisualTransformation.None
                        },
                        supportingText = { Text(key.inputHint()) },
                    )
                }
                if (snapshot.requiredConfiguration.isEmpty()) {
                    Text("This integration uses validated official routing and requires no private configuration.")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onUnlock(values.filterValues(String::isNotBlank)) }) {
                Text("Unlock securely")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

private fun IntegrationAvailability.toStatusText(): Pair<String, String> = when (this) {
    IntegrationAvailability.Locked ->
        "Locked" to "Unlock this provider before Rock Music exposes its configured capability."

    IntegrationAvailability.Available ->
        "Available" to "Configuration, authorization, and capability checks passed."

    is IntegrationAvailability.Unconfigured ->
        "Setup required" to "Missing: ${missingKeys.sorted().joinToString()}"

    IntegrationAvailability.AuthenticationRequired ->
        "Sign in" to "Public configuration is valid; user authorisation is still required."

    IntegrationAvailability.Offline ->
        "Offline" to "This provider cannot be reached while the device is offline."

    is IntegrationAvailability.Unsupported ->
        "Unsupported" to reason

    is IntegrationAvailability.Error ->
        "Error" to message
}

private fun IntegrationAvailability.activationMessage(id: IntegrationId): String = when (this) {
    IntegrationAvailability.Locked -> "${id.readableName()} is locked."
    IntegrationAvailability.Available -> "${id.readableName()} is unlocked and authorised."
    is IntegrationAvailability.Unconfigured -> "Missing: ${missingKeys.sorted().joinToString()}"
    IntegrationAvailability.AuthenticationRequired -> "${id.readableName()} still requires user authorisation."
    IntegrationAvailability.Offline -> "${id.readableName()} is unavailable while offline."
    is IntegrationAvailability.Unsupported -> reason
    is IntegrationAvailability.Error -> message
}

private fun ProviderCapabilities.toReadableCapabilities(): String {
    val capabilities = buildList {
        if (canSearch) add("search")
        if (canStream) add("licensed streaming")
        if (canOpenOfficialPlayback) add("official playback")
        if (canReadPlaylistMetadata) add("playlist metadata")
        if (canRecognizeAudio) add("audio recognition")
        if (canProvideLyrics) add("synchronized lyrics")
        if (canSearchPodcasts) add("podcast search")
        if (canCreateListeningRooms) add("listening rooms")
        if (canShareActivity) add("activity sharing")
        if (canDownload) add("provider-permitted downloads")
    }
    return if (capabilities.isEmpty()) "" else capabilities.joinToString(prefix = "Capabilities: ")
}

private fun ProviderConfigKey.readableLabel(): String = propertyName
    .removePrefix("ROCK_")
    .lowercase()
    .split('_')
    .joinToString(" ") { it.replaceFirstChar(Char::titlecase) }

private fun ProviderConfigKey.inputHint(): String = when (this) {
    ProviderConfigKey.SPOTIFY_REDIRECT_URI ->
        "Registered HTTPS app link or rockmusic://oauth/spotify"

    ProviderConfigKey.DISCORD_REDIRECT_URI ->
        "Registered HTTPS app link or rockmusic://oauth/discord"

    ProviderConfigKey.CLOUD_REDIRECT_URI ->
        "Registered HTTPS app link or rockmusic://oauth/cloud"

    else -> when {
        name.endsWith("_WS_URL") -> "Secure wss:// endpoint"
        name.endsWith("_URL") || name.endsWith("_URI") -> "Registered secure https:// address"
        name.endsWith("_API_KEY") -> "Publishable or Android-restricted key only"
        else -> "Public application identifier"
    }
}

private fun ProviderConfigKey.isSensitiveInput(): Boolean =
    name.endsWith("_API_KEY")

private fun IntegrationId.readableName(): String = name
    .lowercase()
    .split('_')
    .joinToString(" ") { it.replaceFirstChar(Char::titlecase) }

private const val DEFAULT_SPOTIFY_PLAYLIST_REFERENCE =
    "https://open.spotify.com/playlist/25Y5z4jvx8H5UHUFxSY95g"
