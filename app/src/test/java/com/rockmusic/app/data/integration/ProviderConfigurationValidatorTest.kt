package com.rockmusic.app.data.integration

import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderConfigurationValidatorTest {
    @Test
    fun `accepts secure endpoints public identifiers and registered callbacks`() {
        ProviderConfigurationValidator.validate(
            ProviderConfigKey.SPOTIFY_CLIENT_ID,
            "0123456789abcdef0123456789abcdef",
        )
        ProviderConfigurationValidator.validate(
            ProviderConfigKey.SPOTIFY_REDIRECT_URI,
            "rockmusic://oauth/spotify",
        )
        ProviderConfigurationValidator.validate(
            ProviderConfigKey.DISCORD_REDIRECT_URI,
            "https://music.example.com/oauth/discord",
        )
        ProviderConfigurationValidator.validate(
            ProviderConfigKey.DISCORD_REDIRECT_URI,
            "rockmusic://oauth/discord",
        )
        ProviderConfigurationValidator.validate(
            ProviderConfigKey.CLOUD_REDIRECT_URI,
            "rockmusic://oauth/cloud",
        )
        ProviderConfigurationValidator.validate(
            ProviderConfigKey.LISTEN_TOGETHER_WS_URL,
            "wss://listen.example.com/socket",
        )
        ProviderConfigurationValidator.validate(
            ProviderConfigKey.DISCORD_CLIENT_ID,
            "123456789012345678",
        )
    }

    @Test
    fun `rejects insecure unhandled or mismatched redirect backend and websocket routes`() {
        assertTrue(
            runCatching {
                ProviderConfigurationValidator.validate(
                    ProviderConfigKey.SPOTIFY_REDIRECT_URI,
                    "https://music.example.com/oauth/spotify",
                )
            }.isFailure,
        )
        assertTrue(
            runCatching {
                ProviderConfigurationValidator.validate(
                    ProviderConfigKey.SPOTIFY_REDIRECT_URI,
                    "http://example.com/callback",
                )
            }.isFailure,
        )
        assertTrue(
            runCatching {
                ProviderConfigurationValidator.validate(
                    ProviderConfigKey.SPOTIFY_REDIRECT_URI,
                    "rockmusic://oauth/discord",
                )
            }.isFailure,
        )
        assertTrue(
            runCatching {
                ProviderConfigurationValidator.validate(
                    ProviderConfigKey.CATALOGUE_BASE_URL,
                    "http://catalogue.example.com",
                )
            }.isFailure,
        )
        assertTrue(
            runCatching {
                ProviderConfigurationValidator.validate(
                    ProviderConfigKey.LISTEN_TOGETHER_WS_URL,
                    "ws://listen.example.com/socket",
                )
            }.isFailure,
        )
    }

    @Test
    fun `rejects credentials inside provider urls`() {
        assertTrue(
            runCatching {
                ProviderConfigurationValidator.validate(
                    ProviderConfigKey.LYRICS_BASE_URL,
                    "https://user:password@lyrics.example.com",
                )
            }.isFailure,
        )
    }
}
