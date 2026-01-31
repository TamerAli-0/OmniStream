package com.omnistream.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnistream.data.local.WatchHistoryEntity
import com.omnistream.data.repository.WatchHistoryRepository
import com.omnistream.domain.model.Chapter
import com.omnistream.domain.model.Manga
import com.omnistream.domain.model.Page
import com.omnistream.source.SourceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val sourceManager: SourceManager,
    private val watchHistoryRepository: WatchHistoryRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sourceId: String = savedStateHandle["sourceId"] ?: ""
    private val mangaId: String = java.net.URLDecoder.decode(savedStateHandle["mangaId"] ?: "", "UTF-8")
    private var currentChapterId: String = java.net.URLDecoder.decode(savedStateHandle["chapterId"] ?: "", "UTF-8")
    private val mangaTitle: String = savedStateHandle["title"] ?: ""
    private val coverUrl: String? = savedStateHandle["coverUrl"]

    private var chapterList: List<Chapter> = emptyList()
    private var currentChapterIndex: Int = -1
    private var autoSaveJob: Job? = null

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    init {
        loadChapterListThenPages()
    }

    private fun loadChapterListThenPages() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            android.util.Log.d("ReaderViewModel", "Loading chapter: sourceId=$sourceId, mangaId=$mangaId, chapterId=$currentChapterId")

            try {
                val source = sourceManager.getMangaSource(sourceId)
                    ?: throw Exception("Source not found: $sourceId")

                // Load chapter list for navigation
                if (chapterList.isEmpty()) {
                    try {
                        val mangaUrl = when (sourceId) {
                            "mangadex" -> "${source.baseUrl}/manga/$mangaId"
                            "asuracomic" -> "${source.baseUrl}/series/$mangaId"
                            else -> "${source.baseUrl}/manga/$mangaId"
                        }
                        val manga = Manga(
                            id = mangaId,
                            sourceId = sourceId,
                            title = "",
                            url = mangaUrl
                        )
                        chapterList = source.getChapters(manga).sortedBy { it.number }
                    } catch (e: Exception) {
                        android.util.Log.w("ReaderViewModel", "Could not load chapter list for navigation", e)
                    }
                }

                // Find current chapter index
                currentChapterIndex = chapterList.indexOfFirst { it.id == currentChapterId }

                loadCurrentChapterPages(source)

                // After pages are loaded, check for saved progress to resume
                val savedProgress = watchHistoryRepository.getProgress(mangaId, sourceId)
                if (savedProgress != null && savedProgress.chapterId == currentChapterId) {
                    val pages = _uiState.value.pages
                    val savedPage = savedProgress.progressPosition.toInt()
                        .coerceIn(0, (pages.size - 1).coerceAtLeast(0))
                    _uiState.value = _uiState.value.copy(currentPage = savedPage)
                }

            } catch (e: Exception) {
                android.util.Log.e("ReaderViewModel", "Failed to load chapter", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load chapter"
                )
            }
        }
    }

    private suspend fun loadCurrentChapterPages(source: com.omnistream.source.model.MangaSource) {
        // Construct chapter URL based on source type
        val chapterUrl = when (sourceId) {
            "mangadex" -> "${source.baseUrl}/chapter/$currentChapterId"
            "asuracomic" -> "${source.baseUrl}/series/$mangaId/chapter/$currentChapterId"
            "manhuaplus" -> currentChapterId
            else -> "${source.baseUrl}/chapter/$currentChapterId"
        }

        val chapter = Chapter(
            id = currentChapterId,
            mangaId = mangaId,
            sourceId = sourceId,
            url = chapterUrl,
            number = extractChapterNumber(currentChapterId)
        )

        val pages = source.getPages(chapter)
        android.util.Log.d("ReaderViewModel", "Loaded ${pages.size} pages")

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            pages = pages,
            currentPage = 0,
            chapterNumber = chapter.number,
            referer = source.baseUrl,
            hasPreviousChapter = currentChapterIndex > 0,
            hasNextChapter = currentChapterIndex >= 0 && currentChapterIndex < chapterList.size - 1
        )

        startAutoSave()
    }

    fun goToPreviousChapter() {
        if (currentChapterIndex <= 0) return
        viewModelScope.launch { saveCurrentProgress() }
        currentChapterIndex--
        currentChapterId = chapterList[currentChapterIndex].id
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val source = sourceManager.getMangaSource(sourceId)
                    ?: throw Exception("Source not found: $sourceId")
                loadCurrentChapterPages(source)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load chapter"
                )
            }
        }
    }

    fun goToNextChapter() {
        if (currentChapterIndex < 0 || currentChapterIndex >= chapterList.size - 1) return
        viewModelScope.launch { saveCurrentProgress() }
        currentChapterIndex++
        currentChapterId = chapterList[currentChapterIndex].id
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val source = sourceManager.getMangaSource(sourceId)
                    ?: throw Exception("Source not found: $sourceId")
                loadCurrentChapterPages(source)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load chapter"
                )
            }
        }
    }

    private fun extractChapterNumber(chapterId: String): Float {
        return Regex("""(\d+(?:\.\d+)?)""").find(chapterId)?.groupValues?.get(1)?.toFloatOrNull() ?: 0f
    }

    fun setCurrentPage(page: Int) {
        _uiState.value = _uiState.value.copy(currentPage = page)
    }

    private fun startAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            while (isActive) {
                delay(12_000) // 12 seconds
                saveCurrentProgress()
            }
        }
    }

    private suspend fun saveCurrentProgress() {
        val state = _uiState.value
        if (state.pages.isEmpty()) return
        watchHistoryRepository.upsert(
            WatchHistoryEntity(
                id = "$sourceId:$mangaId",
                contentId = mangaId,
                sourceId = sourceId,
                contentType = "manga",
                title = mangaTitle,
                coverUrl = coverUrl,
                chapterId = currentChapterId,
                chapterIndex = currentChapterIndex.coerceAtLeast(0),
                totalChapters = chapterList.size,
                progressPosition = state.currentPage.toLong(),
                totalDuration = state.pages.size.toLong(),
                progressPercentage = if (chapterList.isNotEmpty() && currentChapterIndex >= 0)
                    (currentChapterIndex + 1).toFloat() / chapterList.size
                else
                    (state.currentPage + 1).toFloat() / state.pages.size.coerceAtLeast(1),
                lastWatchedAt = System.currentTimeMillis(),
                isCompleted = chapterList.isNotEmpty() &&
                    currentChapterIndex == chapterList.size - 1 &&
                    state.currentPage >= state.pages.size - 1
            )
        )
    }

    override fun onCleared() {
        super.onCleared()
        autoSaveJob?.cancel()
    }
}

data class ReaderUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val pages: List<Page> = emptyList(),
    val currentPage: Int = 0,
    val chapterNumber: Float = 0f,
    val referer: String? = null,
    val hasPreviousChapter: Boolean = false,
    val hasNextChapter: Boolean = false
)
