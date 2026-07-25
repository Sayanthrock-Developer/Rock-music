package com.rockmusic.app.data.integration

import com.rockmusic.app.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

enum class ProviderConfigKey(val propertyName: String) {
    SPOTIFY_CLIENT_ID("ROCK_SPOTIFY_CLIENT_ID"),
    SPOTIFY_REDIRECT_URI("ROCK_SPOTIFY_REDIRECT_URI"),
    ECHO_FIND_BASE_URL("ROCK_ECHO_FIND_BASE_URL"),
    ECHO_FIND_API_KEY("ROCK_ECHO_FIND_API_KEY"),
    LISTEN_TOGETHER_REST_URL("ROCK_LISTEN_TOGETHER_REST_URL"),
    LISTEN_TOGETHER_WS_URL("ROCK_LISTEN_TOGETHER_WS_URL"),
    DISCORD_CLIENT_ID("ROCK_DISCORD_CLIENT_ID"),
    CATALOGUE_BASE_URL("ROCK_CATALOGUE_BASE_URL"),
    CATALOGUE_API_KEY("ROCK_CATALOGUE_API_KEY"),
    LYRICS_BASE_URL("ROCK_LYRICS_BASE_URL"),
    LYRICS_API_KEY("ROCK_LYRICS_API_KEY"),
    PODCAST_SEARCH_BASE_URL("ROCK_PODCAST_SEARCH_BASE_URL"),
    PODCAST_SEARCH_API_KEY("ROCK_PODCAST_SEARCH_API_KEY"),
    DOWNLOADS_BASE_URL("ROCK_DOWNLOADS_BASE_URL"),
    DOWNLOADS_API_KEY("ROCK_DOWNLOADS_API_KEY"),
    CLOUD_CLIENT_ID("ROCK_CLOUD_CLIENT_ID"),
    CLOUD_REDIRECT_URI("ROCK_CLOUD_REDIRECT_URI"),
}

interface ProviderConfigurationSource {
    fun value(key: ProviderConfigKey): String

    fun missing(keys: Set<ProviderConfigKey>): Set<String> =
        keys.filterTo(linkedSetOf()) { value(it).isBlank() }
            .mapTo(linkedSetOf()) { it.propertyName }
}

/**
 * Reads public mobile configuration from generated BuildConfig fields.
 *
 * Android packages cannot keep private API secrets. Only publishable mobile keys, public client
 * identifiers, redirect URIs and service URLs belong here. Private credentials must stay on the
 * licensed service backend.
 */
@Singleton
class BuildConfigProviderConfigurationSource @Inject constructor() : ProviderConfigurationSource {
    override fun value(key: ProviderConfigKey): String = when (key) {
        ProviderConfigKey.SPOTIFY_CLIENT_ID -> BuildConfig.ROCK_SPOTIFY_CLIENT_ID
        ProviderConfigKey.SPOTIFY_REDIRECT_URI -> BuildConfig.ROCK_SPOTIFY_REDIRECT_URI
        ProviderConfigKey.ECHO_FIND_BASE_URL -> BuildConfig.ROCK_ECHO_FIND_BASE_URL
        ProviderConfigKey.ECHO_FIND_API_KEY -> BuildConfig.ROCK_ECHO_FIND_API_KEY
        ProviderConfigKey.LISTEN_TOGETHER_REST_URL -> BuildConfig.ROCK_LISTEN_TOGETHER_REST_URL
        ProviderConfigKey.LISTEN_TOGETHER_WS_URL -> BuildConfig.ROCK_LISTEN_TOGETHER_WS_URL
        ProviderConfigKey.DISCORD_CLIENT_ID -> BuildConfig.ROCK_DISCORD_CLIENT_ID
        ProviderConfigKey.CATALOGUE_BASE_URL -> BuildConfig.ROCK_CATALOGUE_BASE_URL
        ProviderConfigKey.CATALOGUE_API_KEY -> BuildConfig.ROCK_CATALOGUE_API_KEY
        ProviderConfigKey.LYRICS_BASE_URL -> BuildConfig.ROCK_LYRICS_BASE_URL
        ProviderConfigKey.LYRICS_API_KEY -> BuildConfig.ROCK_LYRICS_API_KEY
        ProviderConfigKey.PODCAST_SEARCH_BASE_URL -> BuildConfig.ROCK_PODCAST_SEARCH_BASE_URL
        ProviderConfigKey.PODCAST_SEARCH_API_KEY -> BuildConfig.ROCK_PODCAST_SEARCH_API_KEY
        ProviderConfigKey.DOWNLOADS_BASE_URL -> BuildConfig.ROCK_DOWNLOADS_BASE_URL
        ProviderConfigKey.DOWNLOADS_API_KEY -> BuildConfig.ROCK_DOWNLOADS_API_KEY
        ProviderConfigKey.CLOUD_CLIENT_ID -> BuildConfig.ROCK_CLOUD_CLIENT_ID
        ProviderConfigKey.CLOUD_REDIRECT_URI -> BuildConfig.ROCK_CLOUD_REDIRECT_URI
    }.trim()
}
