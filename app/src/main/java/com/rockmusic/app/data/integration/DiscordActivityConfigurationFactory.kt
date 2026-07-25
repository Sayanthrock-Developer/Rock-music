package com.rockmusic.app.data.integration

import com.rockmusic.app.domain.integration.DiscordActivityConfiguration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DiscordActivityConfigurationFactory internal constructor(
    private val configuration: ProviderConfigurationSource,
) {
    @Inject
    constructor(configuration: RuntimeProviderConfigurationSource) : this(
        configuration = configuration as ProviderConfigurationSource,
    )

    fun create(enabledByUser: Boolean): Result<DiscordActivityConfiguration> = runCatching {
        val clientId = configuration.value(ProviderConfigKey.DISCORD_CLIENT_ID)
        val redirectUri = configuration.value(ProviderConfigKey.DISCORD_REDIRECT_URI)
        val backendBaseUrl = configuration.value(ProviderConfigKey.DISCORD_ACTIVITY_BACKEND_URL)

        ProviderConfigurationValidator.validate(ProviderConfigKey.DISCORD_CLIENT_ID, clientId)
        ProviderConfigurationValidator.validate(ProviderConfigKey.DISCORD_REDIRECT_URI, redirectUri)
        ProviderConfigurationValidator.validate(
            ProviderConfigKey.DISCORD_ACTIVITY_BACKEND_URL,
            backendBaseUrl,
        )

        DiscordActivityConfiguration(
            clientId = clientId,
            redirectUri = redirectUri,
            backendBaseUrl = backendBaseUrl.trimEnd('/'),
            enabledByUser = enabledByUser,
        )
    }
}
