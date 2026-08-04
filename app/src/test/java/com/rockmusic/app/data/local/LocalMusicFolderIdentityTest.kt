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

    @Test
    fun `keeps a top-level folder name`() {
        assertEquals("Music", LocalMusicFolderIdentity.displayName("Music/"))
    }

    @Test
    fun `normalizes edge cases with complex separators`() {
        // multiple slashes
        assertEquals("foo/bar", LocalMusicFolderIdentity.displayPath("foo///bar"))
        // mixed slashes and backslashes
        assertEquals("foo/bar", LocalMusicFolderIdentity.displayPath("foo\\//bar"))
        // multiple backslashes
        assertEquals("foo/bar", LocalMusicFolderIdentity.displayPath("foo\\\\\\bar"))
        // leading and trailing mixed separators
        assertEquals("foo", LocalMusicFolderIdentity.displayPath("\\/foo/\\"))
        // only separators
        assertEquals("Internal storage", LocalMusicFolderIdentity.displayPath("///\\\\\\"))
        // spaces as segments (note: filter(String::isNotBlank) drops these)
        assertEquals("foo/bar", LocalMusicFolderIdentity.displayPath("foo/ /bar/   /"))
    }
}
