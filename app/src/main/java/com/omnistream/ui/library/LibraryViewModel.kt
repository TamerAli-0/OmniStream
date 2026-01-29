package com.omnistream.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnistream.data.remote.dto.LibraryEntryDto
import com.omnistream.data.repository.SyncRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val favorites: List<LibraryEntryDto> = emptyList(),
    val reading: List<LibraryEntryDto> = emptyList(),
    val planToRead: List<LibraryEntryDto> = emptyList(),
    val completed: List<LibraryEntryDto> = emptyList(),
    val onHold: List<LibraryEntryDto> = emptyList()
)

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val syncRepository: SyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState

    init {
        loadLibrary()
    }

    fun loadLibrary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)

            val result = syncRepository.fetchSyncData()
            result.fold(
                onSuccess = { data ->
                    val grouped = data.library.groupBy { it.category }
                    _uiState.value = LibraryUiState(
                        isLoading = false,
                        favorites = grouped["favorites"] ?: emptyList(),
                        reading = grouped["reading"] ?: emptyList(),
                        planToRead = grouped["plan_to_read"] ?: emptyList(),
                        completed = grouped["completed"] ?: emptyList(),
                        onHold = grouped["on_hold"] ?: emptyList()
                    )
                },
                onFailure = { e ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load library"
                    )
                }
            )
        }
    }
}
