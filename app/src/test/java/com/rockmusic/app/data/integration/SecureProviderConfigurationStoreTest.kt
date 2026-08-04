package com.rockmusic.app.data.integration

import com.rockmusic.app.domain.integration.IntegrationId
import com.rockmusic.app.security.FakeTokenVault
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SecureProviderConfigurationStoreTest {

    private lateinit var tokenVault: FakeTokenVault
    private lateinit var authorizationStore: IntegrationAuthorizationStore
    private lateinit var store: SecureProviderConfigurationStore

    @Before
    fun setUp() {
        tokenVault = FakeTokenVault()
        authorizationStore = IntegrationAuthorizationStore(tokenVault)
        store = SecureProviderConfigurationStore(tokenVault, authorizationStore)
    }

    @Test
    fun `value returns empty string when no value exists`() {
        val result = store.value(ProviderConfigKey.SPOTIFY_CLIENT_ID)
        assertEquals("", result)
    }

    @Test
    fun `value returns trimmed value when a value exists`() {
        tokenVault.put("provider.config.SPOTIFY_CLIENT_ID", "  my-client-id  ")
        val result = store.value(ProviderConfigKey.SPOTIFY_CLIENT_ID)
        assertEquals("my-client-id", result)
    }

    @Test
    fun `explicitUnlockState returns true when vault has unlocked state`() {
        tokenVault.put("provider.state.SPOTIFY", "unlocked")
        assertEquals(true, store.explicitUnlockState(IntegrationId.SPOTIFY))
    }

    @Test
    fun `explicitUnlockState returns false when vault has locked state`() {
        tokenVault.put("provider.state.SPOTIFY", "locked")
        assertEquals(false, store.explicitUnlockState(IntegrationId.SPOTIFY))
    }

    @Test
    fun `explicitUnlockState returns null when no state is set`() {
        assertNull(store.explicitUnlockState(IntegrationId.SPOTIFY))
    }

    private class FakeProviderConfigurationSource : ProviderConfigurationSource {
        val values = mutableMapOf<ProviderConfigKey, String>()
        override fun value(key: ProviderConfigKey): String = values[key] ?: ""
    }

    @Test
    fun `unlock OFFICIAL_YOUTUBE unlocks immediately without validation`() {
        val result = store.unlock(IntegrationId.OFFICIAL_YOUTUBE, emptyMap(), FakeProviderConfigurationSource())
        assertTrue(result.isSuccess)
        assertEquals(true, store.explicitUnlockState(IntegrationId.OFFICIAL_YOUTUBE))
    }

    @Test
    fun `unlock returns failure when required configuration is missing`() {
        val result = store.unlock(IntegrationId.SPOTIFY, emptyMap(), FakeProviderConfigurationSource())
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(result.exceptionOrNull()?.message?.contains("Missing") == true)
    }

    @Test
    fun `unlock returns failure when supplied configuration fails validation`() {
        val suppliedValues = mapOf(
            ProviderConfigKey.SPOTIFY_CLIENT_ID to "invalid client id with spaces",
            ProviderConfigKey.SPOTIFY_REDIRECT_URI to "rockmusic://oauth/spotify"
        )
        val result = store.unlock(IntegrationId.SPOTIFY, suppliedValues, FakeProviderConfigurationSource())
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(result.exceptionOrNull()?.message?.contains("malformed") == true)
    }

    @Test
    fun `unlock successfully unlocks and stores configuration values`() {
        val suppliedValues = mapOf(
            ProviderConfigKey.SPOTIFY_CLIENT_ID to "validclientid1234567890",
            ProviderConfigKey.SPOTIFY_REDIRECT_URI to "rockmusic://oauth/spotify"
        )
        val result = store.unlock(IntegrationId.SPOTIFY, suppliedValues, FakeProviderConfigurationSource())

        assertTrue(result.isSuccess)
        assertEquals("validclientid1234567890", store.value(ProviderConfigKey.SPOTIFY_CLIENT_ID))
        assertEquals("rockmusic://oauth/spotify", store.value(ProviderConfigKey.SPOTIFY_REDIRECT_URI))
        assertEquals(true, store.explicitUnlockState(IntegrationId.SPOTIFY))
    }

    @Test
    fun `unlock clears authorization store when configuration changes`() {
        // Initial setup
        val initialValues = mapOf(
            ProviderConfigKey.SPOTIFY_CLIENT_ID to "validclientid1234567890",
            ProviderConfigKey.SPOTIFY_REDIRECT_URI to "rockmusic://oauth/spotify"
        )
        store.unlock(IntegrationId.SPOTIFY, initialValues, FakeProviderConfigurationSource())
        authorizationStore.markAuthorized(IntegrationId.SPOTIFY)
        assertTrue(authorizationStore.isAuthorized(IntegrationId.SPOTIFY))

        // Change configuration
        val newValues = mapOf(
            ProviderConfigKey.SPOTIFY_CLIENT_ID to "differentclientid123456789",
            ProviderConfigKey.SPOTIFY_REDIRECT_URI to "rockmusic://oauth/spotify"
        )
        store.unlock(IntegrationId.SPOTIFY, newValues, FakeProviderConfigurationSource())

        assertFalse(authorizationStore.isAuthorized(IntegrationId.SPOTIFY))
    }

    @Test
    fun `unlock retains authorization when configuration is unchanged`() {
        // Initial setup
        val initialValues = mapOf(
            ProviderConfigKey.SPOTIFY_CLIENT_ID to "validclientid1234567890",
            ProviderConfigKey.SPOTIFY_REDIRECT_URI to "rockmusic://oauth/spotify"
        )
        store.unlock(IntegrationId.SPOTIFY, initialValues, FakeProviderConfigurationSource())
        authorizationStore.markAuthorized(IntegrationId.SPOTIFY)
        assertTrue(authorizationStore.isAuthorized(IntegrationId.SPOTIFY))

        // Unlock with same configuration
        val newValues = mapOf(
            ProviderConfigKey.SPOTIFY_CLIENT_ID to "validclientid1234567890",
            ProviderConfigKey.SPOTIFY_REDIRECT_URI to "rockmusic://oauth/spotify"
        )
        store.unlock(IntegrationId.SPOTIFY, newValues, FakeProviderConfigurationSource())

        assertTrue(authorizationStore.isAuthorized(IntegrationId.SPOTIFY))
    }

    @Test
    fun `lock sets state to locked for regular provider`() {
        store.lock(IntegrationId.SPOTIFY)
        assertEquals(false, store.explicitUnlockState(IntegrationId.SPOTIFY))
    }

    @Test
    fun `lock does not set state to locked for OFFICIAL_YOUTUBE`() {
        store.lock(IntegrationId.OFFICIAL_YOUTUBE)
        assertNull(store.explicitUnlockState(IntegrationId.OFFICIAL_YOUTUBE))
    }

    @Test
    fun `reset removes all required configuration values and state`() {
        val suppliedValues = mapOf(
            ProviderConfigKey.SPOTIFY_CLIENT_ID to "validclientid1234567890",
            ProviderConfigKey.SPOTIFY_REDIRECT_URI to "rockmusic://oauth/spotify"
        )
        store.unlock(IntegrationId.SPOTIFY, suppliedValues, FakeProviderConfigurationSource())

        store.reset(IntegrationId.SPOTIFY)

        assertEquals("", store.value(ProviderConfigKey.SPOTIFY_CLIENT_ID))
        assertEquals("", store.value(ProviderConfigKey.SPOTIFY_REDIRECT_URI))
        assertNull(store.explicitUnlockState(IntegrationId.SPOTIFY))
    }
}
