package com.rockmusic.app.domain.integration

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class IntegrationContractsTest {
    @Test
    fun `downloads are denied by default`() {
        val capabilities = ProviderCapabilities()

        assertFalse(capabilities.canDownload)
    }

    @Test
    fun `unconfigured state reports every missing key`() {
        val availability = IntegrationAvailability.Unconfigured(
            missingKeys = setOf("ROCK_SPOTIFY_CLIENT_ID", "ROCK_SPOTIFY_REDIRECT_URI"),
        )

        assertTrue("ROCK_SPOTIFY_CLIENT_ID" in availability.missingKeys)
        assertTrue("ROCK_SPOTIFY_REDIRECT_URI" in availability.missingKeys)
    }

    @Test
    fun `download permission denial is explicit`() {
        val permission = DownloadPermission.denied()

        assertFalse(permission.allowed)
        assertTrue(permission.reason.isNotBlank())
    }
}
