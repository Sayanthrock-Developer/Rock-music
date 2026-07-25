package com.rockmusic.app.data.integration

import com.rockmusic.app.domain.integration.IntegrationAvailability
import com.rockmusic.app.domain.integration.IntegrationDefinition
import com.rockmusic.app.domain.integration.IntegrationGateway
import com.rockmusic.app.domain.integration.IntegrationId
import com.rockmusic.app.domain.integration.ProviderCapabilities
import com.rockmusic.app.domain.integration.ProviderDefinitions
import javax.inject.Inject
import javax.inject.Singleton

data class IntegrationSnapshot(
    val id: IntegrationId,
    val displayName: String,
    val availability: IntegrationAvailability,
    val capabilities: ProviderCapabilities,
    val officialProviderOnly: Boolean,
)

@Singleton
class IntegrationRegistry @Inject constructor(
    configuration: BuildConfigProviderConfigurationSource,
) {
    private val gateways: Map<IntegrationId, ConfiguredProviderGateway> =
        ProviderDefinitions.all.associate { definition ->
            definition.id to ConfiguredProviderGateway(definition, configuration)
        }

    fun gateway(id: IntegrationId): IntegrationGateway =
        checkNotNull(gateways[id]) { "No gateway registered for $id" }

    suspend fun snapshots(): List<IntegrationSnapshot> =
        ProviderDefinitions.all.map { definition ->
            val gateway = checkNotNull(gateways[definition.id])
            IntegrationSnapshot(
                id = definition.id,
                displayName = definition.displayName,
                availability = gateway.availability(),
                capabilities = gateway.capabilities(),
                officialProviderOnly = definition.officialProviderOnly,
            )
        }
}

private class ConfiguredProviderGateway(
    private val definition: IntegrationDefinition,
    private val configuration: ProviderConfigurationSource,
) : IntegrationGateway {
    override val id: IntegrationId = definition.id

    override suspend fun availability(): IntegrationAvailability {
        val missing = configuration.missing(definition.requiredConfiguration)
        return when {
            missing.isNotEmpty() -> IntegrationAvailability.Unconfigured(missing)
            definition.requiresUserAuthentication -> IntegrationAvailability.AuthenticationRequired
            else -> IntegrationAvailability.Available
        }
    }

    override suspend fun capabilities(): ProviderCapabilities = definition.capabilities
}
