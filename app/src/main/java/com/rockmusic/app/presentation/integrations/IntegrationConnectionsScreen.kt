package com.rockmusic.app.presentation.integrations

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
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.rockmusic.app.data.integration.IntegrationRegistry
import com.rockmusic.app.data.integration.IntegrationSnapshot
import com.rockmusic.app.domain.integration.IntegrationAvailability
import com.rockmusic.app.domain.integration.ProviderCapabilities
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class IntegrationConnectionsViewModel @Inject constructor(
    private val registry: IntegrationRegistry,
) : ViewModel() {
    private val _state = MutableStateFlow(IntegrationConnectionsState())
    val state: StateFlow<IntegrationConnectionsState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            _state.value = runCatching { registry.snapshots() }
                .fold(
                    onSuccess = { IntegrationConnectionsState(items = it) },
                    onFailure = {
                        IntegrationConnectionsState(
                            error = it.message ?: "Unable to read provider configuration",
                        )
                    },
                )
        }
    }
}

data class IntegrationConnectionsState(
    val items: List<IntegrationSnapshot> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
)

@Composable
fun IntegrationConnectionsScreen(
    onClose: () -> Unit,
    viewModel: IntegrationConnectionsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
                        text = "Connections",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Black,
                    )
                    Text(
                        text = "Provider readiness, sign-in requirements, and official playback routes.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close connections")
                }
            }
        }

        when {
            state.isLoading -> item {
                CircularProgressIndicator()
            }

            state.error != null -> item {
                Card(Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("Connections unavailable", fontWeight = FontWeight.Bold)
                        Text(state.error.orEmpty())
                        Button(onClick = viewModel::refresh) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Retry")
                        }
                    }
                }
            }

            else -> items(state.items, key = { it.id.name }) { snapshot ->
                IntegrationConnectionCard(snapshot)
            }
        }

        item {
            HorizontalDivider()
            Text(
                text = "Private provider secrets must remain on the licensed backend. The Android app accepts only public mobile client IDs, redirect URIs, publishable keys, and service URLs.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}

@Composable
private fun IntegrationConnectionCard(snapshot: IntegrationSnapshot) {
    val (status, detail) = snapshot.availability.toStatusText()
    val capabilityText = snapshot.capabilities.toReadableCapabilities()

    Card(Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
        }
    }
}

private fun IntegrationAvailability.toStatusText(): Pair<String, String> = when (this) {
    IntegrationAvailability.Available ->
        "Available" to "The adapter is ready for its runtime capability and connectivity checks."

    is IntegrationAvailability.Unconfigured ->
        "Setup required" to "Missing: ${missingKeys.sorted().joinToString()}"

    IntegrationAvailability.AuthenticationRequired ->
        "Sign in" to "Public application configuration is present; user authorisation is still required."

    IntegrationAvailability.Offline ->
        "Offline" to "This provider cannot be reached while the device is offline."

    is IntegrationAvailability.Unsupported ->
        "Unsupported" to reason

    is IntegrationAvailability.Error ->
        "Error" to message
}

private fun ProviderCapabilities.toReadableCapabilities(): String {
    val capabilities = buildList {
        if (canSearch) add("search")
        if (canStream) add("licensed streaming")
        if (canOpenOfficialPlayback) add("official playback")
        if (canReadPlaylistMetadata) add("playlist metadata")
        if (canRecognizeAudio) add("audio recognition")
        if (canProvideLyrics) add("lyrics")
        if (canSearchPodcasts) add("podcast search")
        if (canCreateListeningRooms) add("listening rooms")
        if (canShareActivity) add("activity sharing")
        if (canDownload) add("provider-permitted downloads")
    }
    return if (capabilities.isEmpty()) "" else capabilities.joinToString(prefix = "Capabilities: ")
}
