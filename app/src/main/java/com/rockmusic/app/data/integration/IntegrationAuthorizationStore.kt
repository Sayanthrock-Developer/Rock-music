package com.rockmusic.app.data.integration

import com.rockmusic.app.domain.integration.IntegrationId
import com.rockmusic.app.security.TokenVault
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntegrationAuthorizationStore @Inject constructor(
    private val tokenVault: TokenVault,
) {
    fun isAuthorized(id: IntegrationId): Boolean {
        return tokenVault.get(keyFor(id)) != null
    }

    fun markAuthorized(id: IntegrationId) {
        tokenVault.put(keyFor(id), "true")
    }

    fun clear(id: IntegrationId) {
        tokenVault.remove(keyFor(id))
    }

    private fun keyFor(id: IntegrationId): String = "authorized_${id.name}"
}
