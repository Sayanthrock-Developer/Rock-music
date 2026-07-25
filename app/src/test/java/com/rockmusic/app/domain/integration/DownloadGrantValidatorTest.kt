package com.rockmusic.app.domain.integration

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadGrantValidatorTest {
    private val item = DownloadItemRef(providerId = "licensed", mediaId = "track-1")

    @Test
    fun `accepts an item matched unexpired HTTPS grant`() {
        val grant = grant(permission = DownloadGrantPermission(permitted = true))

        assertEquals(
            DownloadGrantValidation.Valid,
            DownloadGrantValidator.validate(grant, item, nowEpochMs = 1_500L),
        )
    }

    @Test
    fun `denies by default when provider permission is absent`() {
        val result = DownloadGrantValidator.validate(
            grant = grant(permission = DownloadGrantPermission(permitted = false, reason = "Not entitled")),
            item = item,
            nowEpochMs = 1_500L,
        )

        assertTrue(result is DownloadGrantValidation.Denied)
        assertEquals("Not entitled", (result as DownloadGrantValidation.Denied).reason)
    }

    @Test
    fun `rejects expired mismatched and insecure grants`() {
        assertEquals(
            DownloadGrantValidation.Expired,
            DownloadGrantValidator.validate(
                grant(DownloadGrantPermission(permitted = true), expiresAt = 1_500L),
                item,
                nowEpochMs = 1_500L,
            ),
        )
        assertEquals(
            DownloadGrantValidation.ItemMismatch,
            DownloadGrantValidator.validate(
                grant(DownloadGrantPermission(permitted = true)),
                item.copy(mediaId = "other"),
                nowEpochMs = 1_500L,
            ),
        )
        assertEquals(
            DownloadGrantValidation.InvalidTransport,
            DownloadGrantValidator.validate(
                grant(
                    permission = DownloadGrantPermission(permitted = true),
                    downloadUrl = "http://example.com/track.mp3",
                ),
                item,
                nowEpochMs = 1_500L,
            ),
        )
    }

    @Test
    fun `rejects malformed SHA256 metadata`() {
        val result = DownloadGrantValidator.validate(
            grant = grant(
                permission = DownloadGrantPermission(permitted = true),
                expectedSha256 = "not-a-sha256",
            ),
            item = item,
            nowEpochMs = 1_500L,
        )

        assertEquals(DownloadGrantValidation.InvalidDigest, result)
    }

    @Test
    fun `rejects blank provider identity even when item identity is blank`() {
        val blankItem = item.copy(providerId = "")
        val result = DownloadGrantValidator.validate(
            grant = grant(
                permission = DownloadGrantPermission(permitted = true),
                providerId = "",
            ),
            item = blankItem,
            nowEpochMs = 1_500L,
        )

        assertEquals(DownloadGrantValidation.ItemMismatch, result)
    }

    private fun grant(
        permission: DownloadGrantPermission,
        expiresAt: Long = 2_000L,
        downloadUrl: String = "https://downloads.example.com/track.mp3",
        expectedSha256: String? = null,
        providerId: String = item.providerId,
    ) = DownloadGrant(
        mediaId = item.mediaId,
        providerId = providerId,
        grantId = "grant-1",
        downloadUrl = downloadUrl,
        issuedAtEpochMs = 1_000L,
        expiresAtEpochMs = expiresAt,
        expectedSha256 = expectedSha256,
        permission = permission,
    )
}
