package com.rockmusic.app.data.integration

import com.rockmusic.app.security.FakeTokenVault
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.security.SecureRandom

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SpotifyAuthorizationCallbackHandlerTest {

    @Test
    fun `fails when no pending request exists`() = runBlocking {
        val vault = FakeTokenVault()
        val requestStore = EncryptedSpotifyPkceRequestStore(vault)

        val authorizationStore = IntegrationAuthorizationStore(vault)
        val secureStore = SecureProviderConfigurationStore(vault, authorizationStore)
        val buildConfiguration = BuildConfigProviderConfigurationSource(secureStore)
        val requestFactory = SpotifyPkceRequestFactory(buildConfiguration, SecureRandom())
        val tokenClient = SpotifyPkceTokenClient(buildConfiguration, vault)

        val handler = SpotifyAuthorizationCallbackHandler(
            requestFactory = requestFactory,
            requestStore = requestStore,
            tokenClient = tokenClient,
            authorizationStore = authorizationStore
        )

        val result = handler.handle("rockmusic://oauth/spotify?code=abc123&state=xyz")

        assertTrue(result.isFailure)
        assertEquals("No pending Spotify authorization request was found.", result.exceptionOrNull()?.message)
    }
}
