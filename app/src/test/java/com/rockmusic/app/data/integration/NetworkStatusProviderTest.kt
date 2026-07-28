package com.rockmusic.app.data.integration

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowConnectivityManager
import org.robolectric.shadows.ShadowNetworkCapabilities
import org.robolectric.shadows.ShadowNetwork

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NetworkStatusProviderTest {

    private lateinit var context: Context
    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var shadowConnectivityManager: ShadowConnectivityManager
    private lateinit var provider: NetworkStatusProvider

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        shadowConnectivityManager = shadowOf(connectivityManager)
        provider = NetworkStatusProvider(context)
    }

    @Test
    fun `isOnline returns false when there is no active network`() {
        shadowConnectivityManager.setDefaultNetworkActive(false)
        assertFalse(provider.isOnline())
    }

    @Test
    fun `isOnline returns false when there is active network but no capabilities`() {
        val network = ShadowNetwork.newInstance(1)
        shadowConnectivityManager.setDefaultNetworkActive(true)
        shadowConnectivityManager.setNetworkCapabilities(network, null)

        assertFalse(provider.isOnline())
    }

    @Test
    fun `isOnline returns false when there is active network but missing internet capability`() {
        val networkCapabilities = ShadowNetworkCapabilities.newInstance()
        shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        val network = ShadowNetwork.newInstance(1)
        shadowConnectivityManager.setDefaultNetworkActive(true)
        shadowConnectivityManager.setNetworkCapabilities(network, networkCapabilities)

        assertFalse(provider.isOnline())
    }

    @Test
    fun `isOnline returns false when there is active network but missing validated capability`() {
        val networkCapabilities = ShadowNetworkCapabilities.newInstance()
        shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

        val network = ShadowNetwork.newInstance(1)
        shadowConnectivityManager.setDefaultNetworkActive(true)
        shadowConnectivityManager.setNetworkCapabilities(network, networkCapabilities)

        assertFalse(provider.isOnline())
    }

    @Test
    fun `isOnline returns true when internet and validated capabilities are present`() {
        val networkCapabilities = ShadowNetworkCapabilities.newInstance()
        shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        shadowOf(networkCapabilities).addCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        val network = ShadowNetwork.newInstance(1)
        shadowConnectivityManager.setDefaultNetworkActive(true)

        // This simulates active network returning capabilities
        val activeNetwork = connectivityManager.activeNetwork
        shadowConnectivityManager.setNetworkCapabilities(activeNetwork, networkCapabilities)

        assertTrue(provider.isOnline())
    }
}
