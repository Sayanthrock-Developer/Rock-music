package com.rockmusic.app.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.rockmusic.app.domain.integration.IntegrationAvailability

@Composable
fun IntegrationUnavailableCard(
    title: String,
    availability: IntegrationAvailability,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    val message = availability.toUserMessage()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "$title unavailable. $message"
            },
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (actionLabel != null && onAction != null) {
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

private fun IntegrationAvailability.toUserMessage(): String = when (this) {
    IntegrationAvailability.Locked -> "This option is locked. Open Connections to unlock it securely."
    IntegrationAvailability.Available -> "Available"
    is IntegrationAvailability.Unconfigured ->
        "Configuration required: ${missingKeys.sorted().joinToString()}"
    IntegrationAvailability.AuthenticationRequired -> "Sign in is required to use this option."
    IntegrationAvailability.Offline -> "This option is unavailable while offline."
    is IntegrationAvailability.Unsupported -> reason
    is IntegrationAvailability.Error -> message
}
