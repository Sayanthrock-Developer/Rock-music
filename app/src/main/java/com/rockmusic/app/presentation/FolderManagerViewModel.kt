package com.rockmusic.app.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rockmusic.app.data.local.FolderExclusionStore
import com.rockmusic.app.data.local.LocalMusicFolder
import com.rockmusic.app.data.local.LocalMusicRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class FolderManagerViewModel @Inject constructor(
    private val localMusicRepository: LocalMusicRepository,
    private val folderExclusionStore: FolderExclusionStore,
) : ViewModel() {
    private val _state = MutableStateFlow(FolderManagerState())
    val state: StateFlow<FolderManagerState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            folderExclusionStore.excludedFolderIds.collect { excluded ->
                _state.value = _state.value.copy(excludedFolderIds = excluded)
            }
        }
    }

    fun loadFolders() {
        if (_state.value.isLoading) return
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true,
                error = null,
                statusMessage = "Scanning music folders…",
            )
            runCatching { localMusicRepository.folders() }
                .onSuccess { folders ->
                    _state.value = _state.value.copy(
                        folders = folders,
                        isLoading = false,
                        error = null,
                        statusMessage = "Found ${folders.size} music folder${if (folders.size == 1) "" else "s"}.",
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Unable to scan music folders",
                        statusMessage = null,
                    )
                }
        }
    }

    fun setFolderIncluded(folder: LocalMusicFolder, included: Boolean) {
        if (_state.value.isSaving) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            runCatching { folderExclusionStore.setExcluded(folder.id, excluded = !included) }
                .onSuccess {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        statusMessage = if (included) {
                            "${folder.displayName} will be included in device scans."
                        } else {
                            "${folder.displayName} will be excluded from device scans."
                        },
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isSaving = false,
                        error = error.message ?: "Unable to update the folder",
                        statusMessage = null,
                    )
                }
        }
    }

    fun includeAll() {
        if (_state.value.isSaving) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isSaving = true, error = null)
            runCatching { folderExclusionStore.includeAll() }
                .onSuccess {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        statusMessage = "All music folders are included.",
                    )
                }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isSaving = false,
                        error = error.message ?: "Unable to include all folders",
                        statusMessage = null,
                    )
                }
        }
    }
}

data class FolderManagerState(
    val folders: List<LocalMusicFolder> = emptyList(),
    val excludedFolderIds: Set<String> = emptySet(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    val statusMessage: String? = null,
)
