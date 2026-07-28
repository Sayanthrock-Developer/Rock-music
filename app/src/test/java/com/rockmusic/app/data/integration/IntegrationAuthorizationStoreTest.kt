package com.rockmusic.app.data.integration

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.rockmusic.app.domain.integration.IntegrationId
import com.rockmusic.app.security.FakeTokenVault
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class IntegrationAuthorizationStoreTest {

    private lateinit var tokenVault: FakeTokenVault
    private lateinit var store: IntegrationAuthorizationStore

    @Before
    fun setUp() {
        tokenVault = FakeTokenVault()
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
