package com.rockmusic.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class LocalMusicFolderIdentityTest {
    @Test
    fun `normalizes path separators and casing for stable ids`() {
        assertEquals(
            "music/rock",
            LocalMusicFolderIdentity.id("/Music\\Rock//"),
        )
    }

    @Test
    fun `uses a stable root identity for missing paths`() {
        assertEquals("__root__", LocalMusicFolderIdentity.id(null))
        assertEquals("Internal storage", LocalMusicFolderIdentity.displayName(""))
    }

    @Test
    fun `uses the final path segment as the folder name`() {
        assertEquals("Downloads", LocalMusicFolderIdentity.displayName("Music/Downloads/"))
        assertEquals("Music/Downloads", LocalMusicFolderIdentity.displayPath("Music/Downloads/"))
    }
}
