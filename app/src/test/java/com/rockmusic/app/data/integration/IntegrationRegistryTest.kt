package com.rockmusic.app.data.integration

import com.rockmusic.app.domain.integration.IntegrationAvailability
import com.rockmusic.app.domain.integration.IntegrationId
import com.rockmusic.app.security.FakeTokenVault
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IntegrationRegistryTest {

    private lateinit var vault: FakeTokenVault
    private lateinit var authorizationStore: IntegrationAuthorizationStore
    private lateinit var secureStore: SecureProviderConfigurationStore
    private lateinit var buildConfig: BuildConfigProviderConfigurationSource
    private lateinit var configurationSource: RuntimeProviderConfigurationSource
    private lateinit var registry: IntegrationRegistry

    @Before
    fun setUp() {
        vault = FakeTokenVault()
        authorizationStore = IntegrationAuthorizationStore(vault)
        secureStore = SecureProviderConfigurationStore(vault, authorizationStore)
        buildConfig = BuildConfigProviderConfigurationSource(secureStore)
        configurationSource = RuntimeProviderConfigurationSource(buildConfig, secureStore)
        registry = IntegrationRegistry(configurationSource, authorizationStore)
    }

    @Test
    fun `snapshots generation maps definition and gateway availability correctly`() = runBlocking {
        // Mock a specific integration, e.g., SPOTIFY
        val id = IntegrationId.SPOTIFY

        // Mock unlock by supplying valid required values
        val suppliedValues = mapOf(
            ProviderConfigKey.SPOTIFY_CLIENT_ID to "client_id",
            ProviderConfigKey.SPOTIFY_REDIRECT_URI to "rockmusic://oauth/spotify"
        )
        registry.unlock(id, suppliedValues)

        // Mark authorized
        authorizationStore.markAuthorized(id)

        val snapshots = registry.snapshots()

        val spotifySnapshot = snapshots.find { it.id == id }!!
        assertEquals("Spotify playlist import", spotifySnapshot.displayName)
        assertEquals(IntegrationAvailability.Available, spotifySnapshot.availability)
        assertTrue(spotifySnapshot.capabilities.canOpenOfficialPlayback)
        assertTrue(spotifySnapshot.capabilities.canReadPlaylistMetadata)
        assertFalse(spotifySnapshot.officialProviderOnly)
        assertTrue(spotifySnapshot.isUnlocked)
        assertTrue(spotifySnapshot.canUnlockWithoutInput)
    }

    @Test
    fun `gateway delegation returns Locked when configuration is incomplete`() = runBlocking {
        val id = IntegrationId.SPOTIFY

        // Supply valid configuration values
        val suppliedValues = mapOf(
            ProviderConfigKey.SPOTIFY_CLIENT_ID to "client_id",
            ProviderConfigKey.SPOTIFY_REDIRECT_URI to "rockmusic://oauth/spotify"
        )
        registry.unlock(id, suppliedValues)

        // Remove a required configuration key to simulate an incomplete configuration.
        // The registry checks `hasCompleteConfiguration` before returning the unlocked state.
        vault.remove("provider.config.SPOTIFY_CLIENT_ID")

        val gateway = registry.gateway(id)
        val availability = gateway.availability()

        assertTrue("Expected Locked but got $availability", availability is IntegrationAvailability.Locked)
    }

    @Test
    fun `gateway delegation returns AuthenticationRequired when configuration is complete but not authorized`() = runBlocking {
        val id = IntegrationId.SPOTIFY

        // Supply valid configuration values to complete the configuration
        val suppliedValues = mapOf(
            ProviderConfigKey.SPOTIFY_CLIENT_ID to "client_id",
            ProviderConfigKey.SPOTIFY_REDIRECT_URI to "rockmusic://oauth/spotify"
        )
        registry.unlock(id, suppliedValues)

        // Integration is NOT authorized in authorizationStore

        val gateway = registry.gateway(id)
        val availability = gateway.availability()

        assertTrue("Expected AuthenticationRequired but got $availability", availability is IntegrationAvailability.AuthenticationRequired)
    }

    @Test
    fun `unlocking integration calls configuration source`() {
        val id = IntegrationId.SPOTIFY
        registry.unlock(id, mapOf(
            ProviderConfigKey.SPOTIFY_CLIENT_ID to "new_client_id",
            ProviderConfigKey.SPOTIFY_REDIRECT_URI to "rockmusic://oauth/spotify"
        ))

        assertTrue(configurationSource.isUnlocked(id))
        assertEquals("new_client_id", configurationSource.value(ProviderConfigKey.SPOTIFY_CLIENT_ID))
    }

    @Test
    fun `locking integration updates configuration source`() {
        val id = IntegrationId.SPOTIFY
        registry.unlock(id, mapOf(
            ProviderConfigKey.SPOTIFY_CLIENT_ID to "client_id",
            ProviderConfigKey.SPOTIFY_REDIRECT_URI to "rockmusic://oauth/spotify"
        ))

        registry.lock(id)

        assertFalse(configurationSource.isUnlocked(id))
    }

    @Test
    fun `reset clears config and authorization`() {
        val id = IntegrationId.SPOTIFY
        registry.unlock(id, mapOf(
            ProviderConfigKey.SPOTIFY_CLIENT_ID to "client_id",
            ProviderConfigKey.SPOTIFY_REDIRECT_URI to "rockmusic://oauth/spotify"
        ))
        authorizationStore.markAuthorized(id)

        registry.reset(id)

        assertFalse(configurationSource.isUnlocked(id))
        assertFalse(authorizationStore.isAuthorized(id))
    }
}
