package com.omnistream.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnistream.domain.model.Manga
import com.omnistream.domain.model.Video
import com.omnistream.source.SourceManager
import com.omnistream.source.model.VideoType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val sourceManager: SourceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var searchJob: Job? = null

    fun search(query: String) {
        if (query.isBlank()) {
            _uiState.value = SearchUiState()
            return
        }

        // Debounce search
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300) // Debounce 300ms
            performSearch(query)
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null,
            query = query
        )

        try {
            val videoSources = sourceManager.getAllVideoSources()
            val mangaSources = sourceManager.getAllMangaSources()

            // Search all sources in parallel
            val videoResults = videoSources.map { source ->
                viewModelScope.async {
                    try {
                        source.search(query, 1).map { video ->
                            SearchResult.VideoResult(video)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SearchViewModel", "Search failed for ${source.name}", e)
                        emptyList()
                    }
                }
            }

            val mangaResults = mangaSources.map { source ->
                viewModelScope.async {
                    try {
                        source.search(query, 1).map { manga ->
                            SearchResult.MangaResult(manga)
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("SearchViewModel", "Search failed for ${source.name}", e)
                        emptyList()
                    }
                }
            }

            val allVideoResults = videoResults.awaitAll().flatten()
            val allMangaResults = mangaResults.awaitAll().flatten()

            // Combine and dedupe by title (keep first occurrence)
            val allResults = (allVideoResults + allMangaResults)
                .distinctBy { result ->
                    when (result) {
                        is SearchResult.VideoResult -> "${result.video.title}-${result.video.sourceId}"
                        is SearchResult.MangaResult -> "${result.manga.title}-${result.manga.sourceId}"
                    }
                }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                results = allResults,
                error = if (allResults.isEmpty()) "No results found" else null
            )

        } catch (e: Exception) {
            android.util.Log.e("SearchViewModel", "Search failed", e)
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = e.message ?: "Search failed"
            )
        }
    }

    fun setFilter(filter: SearchFilter) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
    }

    fun getFilteredResults(): List<SearchResult> {
        val results = _uiState.value.results
        return when (_uiState.value.selectedFilter) {
            SearchFilter.ALL -> results
            SearchFilter.MOVIES -> results.filterIsInstance<SearchResult.VideoResult>()
                .filter { it.video.type == VideoType.MOVIE }
            SearchFilter.TV -> results.filterIsInstance<SearchResult.VideoResult>()
                .filter { it.video.type == VideoType.TV_SERIES }
            SearchFilter.ANIME -> results.filterIsInstance<SearchResult.VideoResult>()
                .filter { it.video.type == VideoType.ANIME }
            SearchFilter.MANGA -> results.filterIsInstance<SearchResult.MangaResult>()
        }
    }
}

data class SearchUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val query: String = "",
    val results: List<SearchResult> = emptyList(),
    val selectedFilter: SearchFilter = SearchFilter.ALL
)

sealed class SearchResult {
    data class VideoResult(val video: Video) : SearchResult()
    data class MangaResult(val manga: Manga) : SearchResult()
}

enum class SearchFilter(val label: String) {
    ALL("All"),
    MOVIES("Movies"),
    TV("TV Shows"),
    ANIME("Anime"),
    MANGA("Manga")
}
