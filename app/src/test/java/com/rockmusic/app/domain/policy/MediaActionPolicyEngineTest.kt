package com.rockmusic.app.domain.policy

import com.rockmusic.app.domain.integration.DownloadPermission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaActionPolicyEngineTest {
    private val policy = MediaActionPolicyEngine()

    @Test
    fun `local playback executes in app`() {
        val decision = policy.decide(
            MediaActionRequest(
                operation = MediaOperation.PLAY,
                origin = MediaOrigin.LOCAL_FILE,
                mediaId = "local-1",
                sourceUri = "content://media/audio/1",
            ),
        )

        assertEquals(MediaActionDecision.ExecuteInApp, decision)
    }

    @Test
    fun `licensed playback requiring official client opens official provider`() {
        val decision = policy.decide(
            MediaActionRequest(
                operation = MediaOperation.PLAY,
                origin = MediaOrigin.LICENSED_CATALOGUE,
                mediaId = "licensed-1",
                access = ProviderAccessContext(
                    online = true,
                    authenticated = true,
                    authenticationRequired = true,
                    regionalAccessAllowed = true,
                    requiresOfficialClient = true,
                    officialUri = "spotify:track:123",
                ),
            ),
        )

        assertTrue(decision is MediaActionDecision.OpenOfficialProvider)
        assertEquals(
            "spotify:track:123",
            (decision as MediaActionDecision.OpenOfficialProvider).uri,
        )
    }

    @Test
    fun `download is denied without explicit permission`() {
        val decision = policy.decide(
            MediaActionRequest(
                operation = MediaOperation.DOWNLOAD,
                origin = MediaOrigin.LICENSED_CATALOGUE,
                mediaId = "licensed-2",
                access = ProviderAccessContext(
                    online = true,
                    authenticated = true,
                    authenticationRequired = true,
                    providerCapabilityGranted = true,
                ),
            ),
        )

        assertTrue(decision is MediaActionDecision.Blocked)
    }

    @Test
    fun `podcast download executes only with provider permission`() {
        val decision = policy.decide(
            MediaActionRequest(
                operation = MediaOperation.DOWNLOAD,
                origin = MediaOrigin.PODCAST_RSS,
                mediaId = "episode-1",
                access = ProviderAccessContext(
                    online = true,
                    providerCapabilityGranted = true,
                    downloadPermission = DownloadPermission(
                        allowed = true,
                        reason = "Publisher permits enclosure downloads",
                    ),
                ),
            ),
        )

        assertEquals(MediaActionDecision.ExecuteInApp, decision)
    }

    @Test
    fun `cached podcast remains playable offline`() {
        val decision = policy.decide(
            MediaActionRequest(
                operation = MediaOperation.PLAY,
                origin = MediaOrigin.PODCAST_RSS,
                mediaId = "episode-2",
                access = ProviderAccessContext(
                    online = false,
                    locallyAvailable = true,
                ),
            ),
        )

        assertEquals(MediaActionDecision.ExecuteInApp, decision)
    }

    @Test
    fun `uncached remote podcast reports offline`() {
        val decision = policy.decide(
            MediaActionRequest(
                operation = MediaOperation.PLAY,
                origin = MediaOrigin.PODCAST_RSS,
                mediaId = "episode-3",
                access = ProviderAccessContext(
                    online = false,
                    locallyAvailable = false,
                ),
            ),
        )

        assertEquals(MediaActionDecision.Offline, decision)
    }

    @Test
    fun `recognition is blocked until microphone consent is granted`() {
        val decision = policy.decide(
            MediaActionRequest(
                operation = MediaOperation.RECOGNISE,
                origin = MediaOrigin.UNKNOWN,
                mediaId = "recognition-session",
                access = ProviderAccessContext(
                    online = true,
                    providerCapabilityGranted = true,
                    userConsentGranted = false,
                ),
            ),
        )

        assertTrue(decision is MediaActionDecision.Blocked)
    }

    @Test
    fun `listening room requires participant legal access confirmation`() {
        val decision = policy.decide(
            MediaActionRequest(
                operation = MediaOperation.CREATE_ROOM,
                origin = MediaOrigin.LICENSED_CATALOGUE,
                mediaId = "room-track",
                access = ProviderAccessContext(
                    online = true,
                    authenticated = true,
                    providerCapabilityGranted = true,
                    participantAccessConfirmed = false,
                ),
            ),
        )

        assertTrue(decision is MediaActionDecision.Blocked)
    }
}
