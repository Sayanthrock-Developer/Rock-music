package com.rockmusic.app.data.integration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscordActivityConfigurationFactoryTest {
    @Test
    fun `creates disabled by default validated public configuration`() {
        val result = factory().create(enabledByUser = false).getOrThrow()

        assertEquals("123456789012345678", result.clientId)
        assertEquals("rockmusic://oauth/discord", result.redirectUri)
        assertEquals("https://activity.example.com", result.backendBaseUrl)
        assertEquals(false, result.enabledByUser)
    }

    @Test
    fun `rejects insecure backend and malformed application id`() {
        assertTrue(factory(backend = "http://activity.example.com").create(false).isFailure)
        assertTrue(factory(clientId = "not-an-id").create(false).isFailure)
    }

    private fun factory(
        clientId: String = "123456789012345678",
        redirect: String = "rockmusic://oauth/discord",
        backend: String = "https://activity.example.com/",
    ) = DiscordActivityConfigurationFactory(
        configuration = object : ProviderConfigurationSource {
            override fun value(key: ProviderConfigKey): String = when (key) {
                ProviderConfigKey.DISCORD_CLIENT_ID -> clientId
                ProviderConfigKey.DISCORD_REDIRECT_URI -> redirect
                ProviderConfigKey.DISCORD_ACTIVITY_BACKEND_URL -> backend
                else -> ""
            }
        },
    )
}
