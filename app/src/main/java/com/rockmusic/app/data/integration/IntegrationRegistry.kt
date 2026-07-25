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
    val requiredConfiguration: List<ProviderConfigKey>,
    val isUnlocked: Boolean,
    val canUnlockWithoutInput: Boolean,
)

@Singleton
class IntegrationRegistry @Inject constructor(
    private val configuration: RuntimeProviderConfigurationSource,
) {
    private val definitions = ProviderDefinitions.all.associateBy(IntegrationDefinition::id)
    private val gateways: Map<IntegrationId, ConfiguredProviderGateway> =
        ProviderDefinitions.all.associate { definition ->
            definition.id to ConfiguredProviderGateway(definition, configuration)
        }

    fun gateway(id: IntegrationId): IntegrationGateway =
        checkNotNull(gateways[id]) { "No gateway registered for $id" }

    fun unlock(
        id: IntegrationId,
        suppliedValues: Map<ProviderConfigKey, String> = emptyMap(),
    ): Result<Unit> = configuration.unlock(id, suppliedValues)

    fun lock(id: IntegrationId) = configuration.lock(id)

    fun reset(id: IntegrationId) = configuration.reset(id)

    suspend fun snapshots(): List<IntegrationSnapshot> =
        ProviderDefinitions.all.map { definition ->
            val gateway = checkNotNull(gateways[definition.id])
            IntegrationSnapshot(
                id = definition.id,
                displayName = definition.displayName,
                availability = gateway.availability(),
                capabilities = gateway.capabilities(),
                officialProviderOnly = definition.officialProviderOnly,
                requiredConfiguration = definition.requiredConfiguration.sortedBy(ProviderConfigKey::name),
                isUnlocked = configuration.isUnlocked(definition.id),
                canUnlockWithoutInput = configuration.hasCompleteConfiguration(definition.id),
            )
        }

    fun definition(id: IntegrationId): IntegrationDefinition =
        checkNotNull(definitions[id]) { "No provider definition registered for $id" }
}

private class ConfiguredProviderGateway(
    private val definition: IntegrationDefinition,
    private val configuration: RuntimeProviderConfigurationSource,
) : IntegrationGateway {
    override val id: IntegrationId = definition.id

    override suspend fun availability(): IntegrationAvailability {
        if (!configuration.isUnlocked(definition.id)) {
            return IntegrationAvailability.Locked
        }
        val missing = configuration.missing(definition.requiredConfiguration)
        return when {
            missing.isNotEmpty() -> IntegrationAvailability.Unconfigured(missing)
            definition.requiresUserAuthentication -> IntegrationAvailability.AuthenticationRequired
            else -> IntegrationAvailability.Available
        }
    }

    override suspend fun capabilities(): ProviderCapabilities = definition.capabilities
}
