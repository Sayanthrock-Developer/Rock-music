package com.rockmusic.app.data.integration

import com.rockmusic.app.domain.integration.IntegrationId
import com.rockmusic.app.domain.integration.ProviderDefinitions
import com.rockmusic.app.security.TokenVault
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SecureProviderConfigurationStore @Inject constructor(
    private val vault: TokenVault,
    private val authorizationStore: IntegrationAuthorizationStore,
) {
    fun value(key: ProviderConfigKey): String =
        vault.get(valueKey(key)).orEmpty().trim()

    fun explicitUnlockState(id: IntegrationId): Boolean? = when (vault.get(stateKey(id))) {
        STATE_UNLOCKED -> true
        STATE_LOCKED -> false
        else -> null
    }

    fun unlock(
        id: IntegrationId,
        suppliedValues: Map<ProviderConfigKey, String>,
        fallback: ProviderConfigurationSource,
    ): Result<Unit> = runCatching {
        if (id == IntegrationId.OFFICIAL_YOUTUBE) {
            vault.put(stateKey(id), STATE_UNLOCKED)
            return@runCatching
        }

        val definition = ProviderDefinitions.all.first { it.id == id }
        val previousValues = definition.requiredConfiguration.associateWith { key ->
            value(key).ifBlank { fallback.value(key).trim() }
        }
        val normalized = definition.requiredConfiguration.associateWith { key ->
            suppliedValues[key]?.trim().orEmpty()
                .ifBlank { previousValues[key].orEmpty() }
        }
        val missing = normalized.filterValues(String::isBlank).keys
        require(missing.isEmpty()) {
            "Missing: ${missing.joinToString { it.propertyName }}"
        }

        normalized.forEach { (key, configuredValue) ->
            ProviderConfigurationValidator.validate(key, configuredValue)
        }

        val suppliedOverrides = normalized.filterKeys { key ->
            suppliedValues[key]?.isNotBlank() == true
        }
        val configurationChanged = suppliedOverrides.any { (key, configuredValue) ->
            configuredValue != previousValues[key]
        }
        if (configurationChanged) authorizationStore.clear(id)

        suppliedOverrides.forEach { (key, configuredValue) ->
            vault.put(valueKey(key), configuredValue)
        }
        vault.put(stateKey(id), STATE_UNLOCKED)
    }

    fun lock(id: IntegrationId) {
        if (id != IntegrationId.OFFICIAL_YOUTUBE) {
            vault.put(stateKey(id), STATE_LOCKED)
        }
    }

    fun reset(id: IntegrationId) {
        ProviderDefinitions.all.first { it.id == id }
            .requiredConfiguration
            .forEach { vault.remove(valueKey(it)) }
        vault.remove(stateKey(id))
    }

    private fun valueKey(key: ProviderConfigKey): String = "provider.config.${key.name}"
    private fun stateKey(id: IntegrationId): String = "provider.state.${id.name}"

    private companion object {
        const val STATE_UNLOCKED = "unlocked"
        const val STATE_LOCKED = "locked"
    }
}

object ProviderConfigurationValidator {
    private val genericId = Regex("^[A-Za-z0-9._:-]{6,256}$")
    private val discordId = Regex("^[0-9]{17,30}$")

    fun validate(key: ProviderConfigKey, value: String) {
        require(value.isNotBlank()) { "${key.propertyName} cannot be empty" }
        when (key) {
            ProviderConfigKey.SPOTIFY_CLIENT_ID -> require(genericId.matches(value)) {
                "Spotify client ID is malformed"
            }

            ProviderConfigKey.DISCORD_CLIENT_ID -> require(discordId.matches(value)) {
                "Discord client ID must be a numeric application ID"
            }

            ProviderConfigKey.SPOTIFY_REDIRECT_URI ->
                validateRedirectUri(key, value, expectedPrivatePath = "/spotify")

            ProviderConfigKey.DISCORD_REDIRECT_URI ->
                validateRedirectUri(key, value, expectedPrivatePath = "/discord")

            ProviderConfigKey.CLOUD_REDIRECT_URI ->
                validateRedirectUri(key, value, expectedPrivatePath = "/cloud")

            ProviderConfigKey.LISTEN_TOGETHER_WS_URL -> validateWssUri(key, value)

            ProviderConfigKey.ECHO_FIND_BASE_URL,
            ProviderConfigKey.LISTEN_TOGETHER_REST_URL,
            ProviderConfigKey.DISCORD_ACTIVITY_BACKEND_URL,
            ProviderConfigKey.CATALOGUE_BASE_URL,
            ProviderConfigKey.LYRICS_BASE_URL,
            ProviderConfigKey.PODCAST_SEARCH_BASE_URL,
            ProviderConfigKey.DOWNLOADS_BASE_URL,
            -> validateHttpsUri(key, value)

            ProviderConfigKey.ECHO_FIND_API_KEY,
            ProviderConfigKey.CATALOGUE_API_KEY,
            ProviderConfigKey.LYRICS_API_KEY,
            ProviderConfigKey.PODCAST_SEARCH_API_KEY,
            ProviderConfigKey.DOWNLOADS_API_KEY,
            ProviderConfigKey.CLOUD_CLIENT_ID,
            -> require(value.length in 6..512 && value.none { it.isWhitespace() }) {
                "${key.propertyName} is malformed"
            }
        }
    }

    private fun validateRedirectUri(
        key: ProviderConfigKey,
        value: String,
        expectedPrivatePath: String,
    ) {
        val uri = runCatching { URI(value) }.getOrNull()
        val clean = uri != null &&
            uri.userInfo == null &&
            uri.query == null &&
            uri.fragment == null
        val isHttpsAppLink = clean &&
            uri?.scheme?.equals("https", ignoreCase = true) == true &&
            !uri.host.isNullOrBlank() &&
            uri.port == -1
        val isRegisteredPrivateCallback = clean &&
            uri?.scheme?.equals("rockmusic", ignoreCase = true) == true &&
            uri.host.equals("oauth", ignoreCase = true) &&
            uri.path == expectedPrivatePath &&
            uri.port == -1
        require(isHttpsAppLink || isRegisteredPrivateCallback) {
            "${key.propertyName} must use a clean HTTPS app link or the registered rockmusic://oauth$expectedPrivatePath callback"
        }
    }

    private fun validateHttpsUri(key: ProviderConfigKey, value: String) {
        val uri = runCatching { URI(value) }.getOrNull()
        require(
            uri?.scheme?.equals("https", ignoreCase = true) == true &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo == null &&
                uri.fragment == null,
        ) { "${key.propertyName} must be a secure HTTPS URI" }
    }

    private fun validateWssUri(key: ProviderConfigKey, value: String) {
        val uri = runCatching { URI(value) }.getOrNull()
        require(
            uri?.scheme?.equals("wss", ignoreCase = true) == true &&
                !uri.host.isNullOrBlank() &&
                uri.userInfo == null &&
                uri.fragment == null,
        ) { "${key.propertyName} must be a secure WSS URI" }
    }
}
