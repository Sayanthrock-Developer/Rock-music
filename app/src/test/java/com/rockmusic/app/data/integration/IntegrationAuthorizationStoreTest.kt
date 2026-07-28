package com.rockmusic.app.data.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.rockmusic.app.domain.integration.IntegrationId
import com.rockmusic.app.security.TokenVault
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements

// Use a Robolectric shadow to completely intercept TokenVault without calling its real constructor
@Implements(TokenVault::class)
class ShadowTokenVault {
    private val store = mutableMapOf<String, String>()

    @Implementation
    fun __constructor__(context: Context) {
        // Do nothing to avoid calling the real constructor which touches Android Keystore
    }

    @Implementation
    fun put(key: String, value: String) {
        store[key] = value
    }

    @Implementation
    fun get(key: String): String? {
        return store[key]
    }

    @Implementation
    fun remove(key: String) {
        store.remove(key)
    }

    @Implementation
    fun clear() {
        store.clear()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], shadows = [ShadowTokenVault::class])
class IntegrationAuthorizationStoreTest {

    private lateinit var tokenVault: TokenVault
    private lateinit var store: IntegrationAuthorizationStore

    @Before
    fun setUp() {
        tokenVault = TokenVault(ApplicationProvider.getApplicationContext())
        store = IntegrationAuthorizationStore(tokenVault)
    }

    @Test
    fun `isAuthorized returns false initially`() {
        assertFalse(store.isAuthorized(IntegrationId.SPOTIFY))
    }

    @Test
    fun `markAuthorized sets state to authorized`() {
        store.markAuthorized(IntegrationId.SPOTIFY)
        assertTrue(store.isAuthorized(IntegrationId.SPOTIFY))
    }

    @Test
    fun `clear removes authorization`() {
        store.markAuthorized(IntegrationId.SPOTIFY)
        store.clear(IntegrationId.SPOTIFY)
        assertFalse(store.isAuthorized(IntegrationId.SPOTIFY))
    }
}
