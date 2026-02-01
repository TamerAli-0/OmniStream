package com.omnistream.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnistream.data.local.SearchHistoryDao
import com.omnistream.data.local.SearchHistoryEntity
import com.omnistream.domain.model.Manga
import com.omnistream.domain.model.Video
import com.omnistream.source.SourceManager
import com.omnistream.source.model.VideoType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(kotlinx.coroutines.FlowPreview::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val sourceManager: SourceManager,
    private val searchHistoryDao: SearchHistoryDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    val searchHistory: StateFlow<List<SearchHistoryEntity>> =
        searchHistoryDao.getRecentSearches(limit = 5)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(400)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    if (query.isBlank()) {
                        flowOf(SearchUiState())
                    } else {
                        flow {
                            emit(_uiState.value.copy(isLoading = true, error = null, query = query))
                            try {
                                val results = performSearch(query)
                                emit(SearchUiState(
                                    isLoading = false,
                                    results = results,
                                    query = query,
                                    error = if (results.isEmpty()) "No results found" else null,
                                    selectedFilter = _uiState.value.selectedFilter
                                ))
                                saveToHistory(query)  // Save after successful search
                            } catch (e: Exception) {
                                android.util.Log.e("SearchViewModel", "Search failed", e)
                                emit(_uiState.value.copy(
                                    isLoading = false,
                                    error = e.message ?: "Search failed"
                                ))
                            }
                        }
                    }
                }
                .collect { state -> _uiState.value = state }
        }
    }

    fun search(query: String) {
        _searchQuery.value = query
    }

    private suspend fun saveToHistory(query: String) {
        if (query.isNotBlank()) {
            searchHistoryDao.insert(SearchHistoryEntity(query = query.trim()))
        }
    }

    private suspend fun performSearch(query: String): List<SearchResult> = coroutineScope {
        val videoSources = sourceManager.getAllVideoSources()
        val mangaSources = sourceManager.getAllMangaSources()

        // Search all sources in parallel
        val videoResults = videoSources.map { source ->
            async {
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
            async {
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
        (allVideoResults + allMangaResults)
            .distinctBy { result ->
                when (result) {
                    is SearchResult.VideoResult -> "${result.video.title}-${result.video.sourceId}"
                    is SearchResult.MangaResult -> "${result.manga.title}-${result.manga.sourceId}"
                }
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

    fun deleteFromHistory(query: String) {
        viewModelScope.launch {
            searchHistoryDao.deleteByQuery(query)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            searchHistoryDao.clearAll()
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
