package com.rockmusic.app.data.integration

import com.rockmusic.app.domain.integration.IntegrationId
import com.rockmusic.app.security.TokenVault
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IntegrationAuthorizationStore @Inject constructor(
    private val vault: TokenVault,
) {
    fun isAuthorized(id: IntegrationId): Boolean =
        vault.get(stateKey(id)) == AUTHORIZED

    fun markAuthorized(id: IntegrationId, authorizedAtEpochMs: Long = System.currentTimeMillis()) {
        vault.put(stateKey(id), AUTHORIZED)
        vault.put(timestampKey(id), authorizedAtEpochMs.toString())
    }

    fun clear(id: IntegrationId) {
        vault.remove(stateKey(id))
        vault.remove(timestampKey(id))
    }

    private fun stateKey(id: IntegrationId): String = "provider.authorization.${id.name}.state"
    private fun timestampKey(id: IntegrationId): String = "provider.authorization.${id.name}.timestamp"

    private companion object {
        const val AUTHORIZED = "authorized"
    }
}
