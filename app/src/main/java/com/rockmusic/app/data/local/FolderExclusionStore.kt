package com.rockmusic.app.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.folderExclusionDataStore by preferencesDataStore(name = "folder_exclusions")

@Singleton
class FolderExclusionStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.folderExclusionDataStore

    val excludedFolderIds: Flow<Set<String>> = dataStore.data
        .catch { error ->
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { preferences -> preferences[EXCLUDED_FOLDER_IDS].orEmpty().toSet() }

    suspend fun setExcluded(folderId: String, excluded: Boolean) {
        val normalizedId = folderId.trim()
        require(normalizedId.isNotBlank()) { "Folder ID cannot be blank" }

        dataStore.edit { preferences ->
            val current = preferences[EXCLUDED_FOLDER_IDS].orEmpty().toMutableSet()
            if (excluded) current += normalizedId else current -= normalizedId
            preferences[EXCLUDED_FOLDER_IDS] = current
        }
    }

    suspend fun includeAll() {
        dataStore.edit { preferences -> preferences[EXCLUDED_FOLDER_IDS] = emptySet() }
    }

    private companion object {
        val EXCLUDED_FOLDER_IDS = stringSetPreferencesKey("excluded_folder_ids")
    }
}
