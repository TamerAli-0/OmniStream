package com.omnistream.ui.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnistream.domain.model.Chapter
import com.omnistream.domain.model.Manga
import com.omnistream.domain.model.Page
import com.omnistream.source.SourceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val sourceManager: SourceManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sourceId: String = savedStateHandle["sourceId"] ?: ""
    private val mangaId: String = java.net.URLDecoder.decode(savedStateHandle["mangaId"] ?: "", "UTF-8")
    private var currentChapterId: String = java.net.URLDecoder.decode(savedStateHandle["chapterId"] ?: "", "UTF-8")

    private var chapterList: List<Chapter> = emptyList()
    private var currentChapterIndex: Int = -1

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
    }

    fun goToPreviousChapter() {
        if (currentChapterIndex <= 0) return
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
