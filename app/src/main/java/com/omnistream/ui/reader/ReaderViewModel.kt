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
    private val chapterId: String = java.net.URLDecoder.decode(savedStateHandle["chapterId"] ?: "", "UTF-8")

    private val _uiState = MutableStateFlow(ReaderUiState())
    val uiState: StateFlow<ReaderUiState> = _uiState.asStateFlow()

    init {
        loadChapterPages()
    }

    private fun loadChapterPages() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            android.util.Log.d("ReaderViewModel", "Loading chapter: sourceId=$sourceId, mangaId=$mangaId, chapterId=$chapterId")

            try {
                val source = sourceManager.getMangaSource(sourceId)
                    ?: throw Exception("Source not found: $sourceId")

                // Construct chapter URL based on source type
                val chapterUrl = when (sourceId) {
                    "mangadex" -> "${source.baseUrl}/chapter/$chapterId"
                    "asuracomic" -> "${source.baseUrl}/series/$mangaId/chapter/$chapterId"
                    "manhuaplus" -> chapterId // ManhuaPlus chapter ID is the full URL path
                    else -> "${source.baseUrl}/chapter/$chapterId"
                }

                // Create chapter object
                val chapter = Chapter(
                    id = chapterId,
                    mangaId = mangaId,
                    sourceId = sourceId,
                    url = chapterUrl,
                    number = extractChapterNumber(chapterId)
                )

                // Get pages
                val pages = source.getPages(chapter)
                android.util.Log.d("ReaderViewModel", "Loaded ${pages.size} pages")

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    pages = pages,
                    chapterNumber = chapter.number,
                    referer = source.baseUrl
                )

            } catch (e: Exception) {
                android.util.Log.e("ReaderViewModel", "Failed to load chapter", e)
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
