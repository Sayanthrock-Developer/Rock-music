package com.rockmusic.app.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FolderExclusionStoreTest {

    private lateinit var store: FolderExclusionStore

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        store = FolderExclusionStore(context)
        runBlocking {
            store.includeAll()
        }
    }

    @Test
    fun `initial state is empty`() = runBlocking {
        val excluded = store.excludedFolderIds.first()
        assertTrue(excluded.isEmpty())
    }

    @Test
    fun `setExcluded true adds folder ID`() = runBlocking {
        store.setExcluded("folder1", true)
        val excluded = store.excludedFolderIds.first()
        assertEquals(setOf("folder1"), excluded)
    }

    @Test
    fun `setExcluded false removes folder ID`() = runBlocking {
        store.setExcluded("folder1", true)
        store.setExcluded("folder2", true)

        store.setExcluded("folder1", false)
        val excluded = store.excludedFolderIds.first()
        assertEquals(setOf("folder2"), excluded)
    }

    @Test
    fun `setExcluded normalizes folder ID`() = runBlocking {
        store.setExcluded("  folder1  ", true)
        val excluded = store.excludedFolderIds.first()
        assertEquals(setOf("folder1"), excluded)
    }

    @Test
    fun `setExcluded throws when folder ID is blank`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { store.setExcluded("   ", true) }
        }
    }

    @Test
    fun `includeAll clears all excluded folders`() = runBlocking {
        store.setExcluded("folder1", true)
        store.setExcluded("folder2", true)

        store.includeAll()

        val excluded = store.excludedFolderIds.first()
        assertTrue(excluded.isEmpty())
    }
}
