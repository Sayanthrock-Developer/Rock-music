package com.rockmusic.app.domain.integration

import com.rockmusic.app.data.integration.ProviderConfigKey
import com.rockmusic.app.data.integration.ProviderConfigurationSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProviderDefinitionsTest {
    @Test
    fun `every integration id has exactly one definition`() {
        assertEquals(
            IntegrationId.entries.toSet(),
            ProviderDefinitions.all.map(IntegrationDefinition::id).toSet(),
        )
    }

    @Test
    fun `official youtube uses provider handoff without mobile credentials`() {
        val definition = ProviderDefinitions.all.single {
            it.id == IntegrationId.OFFICIAL_YOUTUBE
        }

        assertTrue(definition.officialProviderOnly)
        assertTrue(definition.requiredConfiguration.isEmpty())
        assertTrue(definition.capabilities.canOpenOfficialPlayback)
        assertFalse(definition.capabilities.canStream)
        assertFalse(definition.capabilities.canDownload)
    }

    @Test
    fun `spotify supports metadata import but not protected audio streaming`() {
        val definition = ProviderDefinitions.all.single {
            it.id == IntegrationId.SPOTIFY
        }

        assertTrue(definition.capabilities.canReadPlaylistMetadata)
        assertTrue(definition.capabilities.canOpenOfficialPlayback)
        assertFalse(definition.capabilities.canStream)
        assertFalse(definition.capabilities.canDownload)
    }

    @Test
    fun `missing configuration reports exact Gradle property names`() {
        val source = FakeProviderConfigurationSource(
            values = mapOf(ProviderConfigKey.SPOTIFY_CLIENT_ID to "public-client-id"),
        )

        val missing = source.missing(
            setOf(
                ProviderConfigKey.SPOTIFY_CLIENT_ID,
                ProviderConfigKey.SPOTIFY_REDIRECT_URI,
            ),
        )

        assertEquals(setOf("ROCK_SPOTIFY_REDIRECT_URI"), missing)
    }

    private class FakeProviderConfigurationSource(
        private val values: Map<ProviderConfigKey, String>,
    ) : ProviderConfigurationSource {
        override fun value(key: ProviderConfigKey): String = values[key].orEmpty()
    }
}
