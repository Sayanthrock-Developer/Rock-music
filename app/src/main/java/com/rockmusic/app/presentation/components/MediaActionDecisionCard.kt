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
import com.rockmusic.app.domain.policy.MediaActionDecision

@Composable
fun MediaActionDecisionCard(
    decision: MediaActionDecision,
    modifier: Modifier = Modifier,
    onAction: (() -> Unit)? = null,
) {
    if (decision == MediaActionDecision.ExecuteInApp) return

    val presentation = decision.presentation()
    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "${presentation.title}. ${presentation.message}"
            },
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = presentation.title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = presentation.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (presentation.actionLabel != null && onAction != null) {
                Button(onClick = onAction) {
                    Text(presentation.actionLabel)
                }
            }
        }
    }
}

private data class DecisionPresentation(
    val title: String,
    val message: String,
    val actionLabel: String? = null,
)

private fun MediaActionDecision.presentation(): DecisionPresentation = when (this) {
    MediaActionDecision.ExecuteInApp -> DecisionPresentation(
        title = "Ready",
        message = "Rock Music can complete this action.",
    )

    is MediaActionDecision.OpenOfficialProvider -> DecisionPresentation(
        title = "Continue with the official provider",
        message = reason,
        actionLabel = "Open provider",
    )

    is MediaActionDecision.RequireConfiguration -> DecisionPresentation(
        title = "Configuration required",
        message = "Missing: ${missingKeys.sorted().joinToString()}",
        actionLabel = "Open settings",
    )

    is MediaActionDecision.RequireAuthentication -> DecisionPresentation(
        title = "Sign in required",
        message = "Connect your $providerName account to continue.",
        actionLabel = "Connect account",
    )

    MediaActionDecision.Offline -> DecisionPresentation(
        title = "You are offline",
        message = "Connect to the internet or choose content already stored on this device.",
        actionLabel = "Retry",
    )

    is MediaActionDecision.Blocked -> DecisionPresentation(
        title = "Action unavailable",
        message = reason,
    )
}
