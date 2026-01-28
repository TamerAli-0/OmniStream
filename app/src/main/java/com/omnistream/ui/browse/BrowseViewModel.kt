package com.omnistream.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnistream.domain.model.Manga
import com.omnistream.domain.model.Video
import com.omnistream.source.SourceManager
import com.omnistream.source.model.MangaSource
import com.omnistream.source.model.VideoSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val sourceManager: SourceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(BrowseUiState())
    val uiState: StateFlow<BrowseUiState> = _uiState.asStateFlow()

    init {
        loadSources()
    }

    private fun loadSources() {
        val mangaSources = sourceManager.getAllMangaSources()
        val videoSources = sourceManager.getAllVideoSources()

        val allSources = mutableListOf<SourceInfo>()

        mangaSources.forEach { source ->
            allSources.add(SourceInfo(
                id = source.id,
                name = source.name,
                type = SourceType.MANGA
            ))
        }

        videoSources.forEach { source ->
            allSources.add(SourceInfo(
                id = source.id,
                name = source.name,
                type = SourceType.VIDEO
            ))
        }

        _uiState.value = _uiState.value.copy(
            sources = allSources,
            selectedSourceIndex = if (allSources.isNotEmpty()) 0 else -1
        )

        if (allSources.isNotEmpty()) {
            loadSourceContent(allSources[0])
        }
    }

    fun selectSource(index: Int) {
        val sources = _uiState.value.sources
        if (index in sources.indices) {
            _uiState.value = _uiState.value.copy(selectedSourceIndex = index)
            loadSourceContent(sources[index])
        }
    }

    private fun loadSourceContent(sourceInfo: SourceInfo) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null,
                mangaItems = emptyList(),
                videoItems = emptyList()
            )

            try {
                when (sourceInfo.type) {
                    SourceType.MANGA -> {
                        val source = sourceManager.getMangaSource(sourceInfo.id)
                            ?: throw Exception("Source not found")
                        val items = source.getPopular(1)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            mangaItems = items
                        )
                    }
                    SourceType.VIDEO -> {
                        val source = sourceManager.getVideoSource(sourceInfo.id)
                            ?: throw Exception("Source not found")
                        val homeSections = source.getHomePage()
                        val items = homeSections.flatMap { it.items }.take(30)
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            videoItems = items
                        )
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("BrowseViewModel", "Failed to load ${sourceInfo.name}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "${sourceInfo.name}: ${e.message}"
                )
            }
        }
    }

    fun refresh() {
        val index = _uiState.value.selectedSourceIndex
        val sources = _uiState.value.sources
        if (index in sources.indices) {
            loadSourceContent(sources[index])
        }
    }
}

data class BrowseUiState(
    val sources: List<SourceInfo> = emptyList(),
    val selectedSourceIndex: Int = -1,
    val isLoading: Boolean = false,
    val error: String? = null,
    val mangaItems: List<Manga> = emptyList(),
    val videoItems: List<Video> = emptyList()
)

data class SourceInfo(
    val id: String,
    val name: String,
    val type: SourceType
)

enum class SourceType {
    MANGA, VIDEO
}
