package com.rockmusic.app.data.integration

import com.rockmusic.app.security.FakeTokenVault
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EncryptedSpotifyPkceRequestStoreTest {

    private lateinit var vault: FakeTokenVault
    private lateinit var store: EncryptedSpotifyPkceRequestStore

    @Before
    fun setUp() {
        vault = FakeTokenVault()
        store = EncryptedSpotifyPkceRequestStore(vault)
    }

    @Test
    fun `consume returns null and removes request if expired`() = runBlocking {
        val now = System.currentTimeMillis()
        val expiredRequest = SpotifyPkceRequest(
            authorizationUri = "https://example.com/auth",
            codeVerifier = "verifier",
            codeChallenge = "challenge",
            state = "test-state",
            redirectUri = "https://example.com/callback",
            scopes = setOf("scope1", "scope2"),
            createdAtEpochMs = now - 2000,
            expiresAtEpochMs = now - 1000 // Expired
        )

        store.save(expiredRequest)

        val consumed = store.consume("test-state")

        assertNull(consumed)
        assertNull(store.current())
    }

    @Test
    fun `consume returns request and removes it if valid`() = runBlocking {
        val now = System.currentTimeMillis()
        val validRequest = SpotifyPkceRequest(
            authorizationUri = "https://example.com/auth",
            codeVerifier = "verifier",
            codeChallenge = "challenge",
            state = "test-state",
            redirectUri = "https://example.com/callback",
            scopes = setOf("scope1", "scope2"),
            createdAtEpochMs = now,
            expiresAtEpochMs = now + 10000 // Valid
        )

        store.save(validRequest)

        val consumed = store.consume("test-state")

        assertEquals(validRequest, consumed)
        assertNull(store.current())
    }

    @Test
    fun `consume returns null if state does not match`() = runBlocking {
        val now = System.currentTimeMillis()
        val validRequest = SpotifyPkceRequest(
            authorizationUri = "https://example.com/auth",
            codeVerifier = "verifier",
            codeChallenge = "challenge",
            state = "test-state",
            redirectUri = "https://example.com/callback",
            scopes = setOf("scope1", "scope2"),
            createdAtEpochMs = now,
            expiresAtEpochMs = now + 10000 // Valid
        )

        store.save(validRequest)

        val consumed = store.consume("wrong-state")

        assertNull(consumed)
        assertEquals(validRequest, store.current()) // Request should remain if state doesn't match
    }

    @Test
    fun `clearExpired removes request if expired`() = runBlocking {
        val now = System.currentTimeMillis()
        val expiredRequest = SpotifyPkceRequest(
            authorizationUri = "https://example.com/auth",
            codeVerifier = "verifier",
            codeChallenge = "challenge",
            state = "test-state",
            redirectUri = "https://example.com/callback",
            scopes = setOf("scope1", "scope2"),
            createdAtEpochMs = now - 2000,
            expiresAtEpochMs = now - 1000 // Expired
        )

        store.save(expiredRequest)
        store.clearExpired(now)
        assertNull(store.current())
    }

    @Test
    fun `clearExpired keeps request if valid`() = runBlocking {
        val now = System.currentTimeMillis()
        val validRequest = SpotifyPkceRequest(
            authorizationUri = "https://example.com/auth",
            codeVerifier = "verifier",
            codeChallenge = "challenge",
            state = "test-state",
            redirectUri = "https://example.com/callback",
            scopes = setOf("scope1", "scope2"),
            createdAtEpochMs = now,
            expiresAtEpochMs = now + 10000 // Valid
        )

        store.save(validRequest)
        store.clearExpired(now)
        assertEquals(validRequest, store.current())
    }
}
