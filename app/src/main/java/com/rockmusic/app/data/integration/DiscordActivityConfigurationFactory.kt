package com.rockmusic.app.data.integration

import com.rockmusic.app.domain.integration.DiscordActivityConfiguration
import java.net.URI
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

        require(DISCORD_CLIENT_ID_PATTERN.matches(clientId)) {
            "ROCK_DISCORD_CLIENT_ID must be a Discord application ID"
        }
        validateRedirectUri(redirectUri)
        validateBackendUrl(backendBaseUrl)

        DiscordActivityConfiguration(
            clientId = clientId,
            redirectUri = redirectUri,
            backendBaseUrl = backendBaseUrl.trimEnd('/'),
            enabledByUser = enabledByUser,
        )
    }

    private fun validateRedirectUri(value: String) {
        require(value.isNotBlank()) { "ROCK_DISCORD_REDIRECT_URI is not configured" }
        val uri = runCatching { URI(value) }.getOrNull()
        require(
            uri != null &&
                uri.scheme.equals("https", ignoreCase = true) &&
                !uri.host.isNullOrBlank(),
        ) { "ROCK_DISCORD_REDIRECT_URI must use HTTPS" }
        require(uri.userInfo == null && uri.query == null && uri.fragment == null) {
            "ROCK_DISCORD_REDIRECT_URI must not contain credentials, a query, or a fragment"
        }
    }

    private fun validateBackendUrl(value: String) {
        require(value.isNotBlank()) { "ROCK_DISCORD_ACTIVITY_BACKEND_URL is not configured" }
        val uri = runCatching { URI(value) }.getOrNull()
        require(
            uri != null &&
                uri.scheme.equals("https", ignoreCase = true) &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo == null &&
                uri.query == null &&
                uri.fragment == null,
        ) {
            "ROCK_DISCORD_ACTIVITY_BACKEND_URL must be a clean HTTPS URL"
        }
    }

    private companion object {
        val DISCORD_CLIENT_ID_PATTERN = Regex("^[0-9]{17,30}$")
    }
}
