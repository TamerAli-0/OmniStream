package com.omnistream.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnistream.data.local.FavoriteDao
import com.omnistream.data.local.FavoriteEntity
import com.omnistream.data.local.WatchHistoryEntity
import com.omnistream.data.repository.WatchHistoryRepository
import com.omnistream.domain.model.Chapter
import com.omnistream.domain.model.Manga
import com.omnistream.source.SourceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MangaDetailViewModel @Inject constructor(
    private val sourceManager: SourceManager,
    private val favoriteDao: FavoriteDao,
    private val watchHistoryRepository: WatchHistoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sourceId: String = savedStateHandle["sourceId"] ?: ""
    private val mangaId: String = java.net.URLDecoder.decode(savedStateHandle["mangaId"] ?: "", "UTF-8")

    private val _uiState = MutableStateFlow(MangaDetailUiState())
    val uiState: StateFlow<MangaDetailUiState> = _uiState.asStateFlow()

    init {
        loadMangaDetails()
        observeFavoriteStatus()
        loadReadingProgress()
    }

    private fun loadReadingProgress() {
        viewModelScope.launch {
            val saved = watchHistoryRepository.getProgress(mangaId, sourceId)
            if (saved != null && !saved.isCompleted) {
                _uiState.value = _uiState.value.copy(savedProgress = saved)
            }
        }
    }

    fun toggleChapterSort() {
        val current = _uiState.value
        val newAscending = !current.chaptersAscending
        val sorted = if (newAscending) current.chapters.sortedBy { it.number }
                     else current.chapters.sortedByDescending { it.number }
        _uiState.value = current.copy(chapters = sorted, chaptersAscending = newAscending)
    }

    private fun observeFavoriteStatus() {
        viewModelScope.launch {
            favoriteDao.isFavorite(sourceId, mangaId).collect { isFav ->
                _uiState.value = _uiState.value.copy(isFavorite = isFav)
            }
        }
    }

    private fun loadMangaDetails() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            android.util.Log.d("MangaDetailViewModel", "Loading manga: sourceId=$sourceId, mangaId=$mangaId")

            try {
                val source = sourceManager.getMangaSource(sourceId)
                    ?: throw Exception("Source not found: $sourceId")

                // Construct URL based on source type
                val mangaUrl = when (sourceId) {
                    "mangadex" -> "${source.baseUrl}/manga/$mangaId"
                    "asuracomic" -> "${source.baseUrl}/series/$mangaId"
                    else -> "${source.baseUrl}/manga/$mangaId"
                }

                // Create initial manga object with ID
                val initialManga = Manga(
                    id = mangaId,
                    sourceId = sourceId,
                    title = "",
                    url = mangaUrl
                )

                // Get full details
                val manga = source.getDetails(initialManga)
                android.util.Log.d("MangaDetailViewModel", "Loaded manga: ${manga.title}")

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    manga = manga
                )

                // Load chapters
                loadChapters(source, manga)

            } catch (e: Exception) {
                android.util.Log.e("MangaDetailViewModel", "Failed to load manga", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load manga"
                )
            }
        }
    }

    fun retryLoad() {
        loadMangaDetails()
    }

    private suspend fun loadChapters(source: com.omnistream.source.model.MangaSource, manga: Manga) {
        try {
            val chapters = source.getChapters(manga)
            _uiState.value = _uiState.value.copy(chapters = chapters)
        } catch (e: Exception) {
            android.util.Log.e("MangaDetailViewModel", "Failed to load chapters", e)
            _uiState.value = _uiState.value.copy(
                chaptersError = e.message ?: "Failed to load chapters"
            )
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val manga = _uiState.value.manga ?: return@launch
            if (_uiState.value.isFavorite) {
                favoriteDao.removeFavorite(sourceId, mangaId)
            } else {
                favoriteDao.addFavorite(
                    FavoriteEntity(
                        id = "$sourceId:$mangaId",
                        contentId = mangaId,
                        sourceId = sourceId,
                        contentType = "manga",
                        title = manga.title,
                        coverUrl = manga.coverUrl
                    )
                )
            }
        }
    }
}

data class MangaDetailUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val chaptersError: String? = null,
    val manga: Manga? = null,
    val chapters: List<Chapter> = emptyList(),
    val isFavorite: Boolean = false,
    val chaptersAscending: Boolean = true,
    val savedProgress: WatchHistoryEntity? = null
)
