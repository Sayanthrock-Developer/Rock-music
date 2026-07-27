package com.rockmusic.app.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FavouriteStoreTest {

    private lateinit var sharedPreferences: FakeSharedPreferences
    private lateinit var context: FakeContext
    private lateinit var favouriteStore: FavouriteStore

    @Before
    fun setup() {
        sharedPreferences = FakeSharedPreferences()
        context = FakeContext(sharedPreferences)
        favouriteStore = FavouriteStore(context)
    }

    @Test
    fun `isFavourite returns false for blank mediaId`() {
        assertFalse(favouriteStore.isFavourite(""))
        assertFalse(favouriteStore.isFavourite("   "))
    }

    @Test
    fun `isFavourite returns false when mediaId is not in preferences`() {
        assertFalse(favouriteStore.isFavourite("media_1"))
    }

    @Test
    fun `isFavourite returns true when mediaId is true in preferences`() {
        sharedPreferences.map["media_1"] = true
        assertTrue(favouriteStore.isFavourite("media_1"))
    }

    @Test
    fun `isFavourite returns false when mediaId is false in preferences`() {
        sharedPreferences.map["media_1"] = false
        assertFalse(favouriteStore.isFavourite("media_1"))
    }

    @Test
    fun `toggle returns false for blank mediaId`() {
        assertFalse(favouriteStore.toggle(""))
        assertFalse(favouriteStore.toggle("   "))
    }

    @Test
    fun `toggle changes false to true`() {
        assertFalse(favouriteStore.isFavourite("media_1"))

        assertTrue(favouriteStore.toggle("media_1"))
        assertTrue(favouriteStore.isFavourite("media_1"))
        assertTrue(sharedPreferences.map["media_1"] == true)
    }

    @Test
    fun `toggle changes true to false`() {
        sharedPreferences.map["media_1"] = true
        assertTrue(favouriteStore.isFavourite("media_1"))

        assertFalse(favouriteStore.toggle("media_1"))
        assertFalse(favouriteStore.isFavourite("media_1"))
        assertTrue(sharedPreferences.map["media_1"] == false)
    }
}
