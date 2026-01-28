package com.omnistream.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnistream.domain.model.HomeSection
import com.omnistream.domain.model.Manga
import com.omnistream.domain.model.Video
import com.omnistream.source.SourceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sourceManager: SourceManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadHomeContent()
    }

    fun loadHomeContent() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            android.util.Log.d("HomeViewModel", "Starting to load home content")

            try {
                // Load manga from all sources
                val mangaDeferred = async { loadMangaContent() }

                // Load video content (anime + movies)
                val videoDeferred = async { loadVideoContent() }

                val mangaSections = mangaDeferred.await()
                val videoSections = videoDeferred.await()

                android.util.Log.d("HomeViewModel", "Loaded ${mangaSections.size} manga sections, ${videoSections.size} video sections")

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    mangaSections = mangaSections,
                    videoSections = videoSections
                )
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Failed to load home content", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load content"
                )
            }
        }
    }

    private suspend fun loadMangaContent(): List<MangaSection> {
        val sections = mutableListOf<MangaSection>()
        val errors = mutableListOf<String>()

        android.util.Log.d("HomeViewModel", "Loading manga from ${sourceManager.getAllMangaSources().size} sources")

        sourceManager.getAllMangaSources().forEach { source ->
            android.util.Log.d("HomeViewModel", "Loading manga source: ${source.name}")
            try {
                val popular = source.getPopular(1).take(10)
                android.util.Log.d("HomeViewModel", "${source.name} popular: ${popular.size} items")
                if (popular.isNotEmpty()) {
                    sections.add(MangaSection(
                        title = "${source.name} - Popular",
                        items = popular,
                        sourceId = source.id
                    ))
                }

                val latest = source.getLatest(1).take(10)
                android.util.Log.d("HomeViewModel", "${source.name} latest: ${latest.size} items")
                if (latest.isNotEmpty()) {
                    sections.add(MangaSection(
                        title = "${source.name} - Latest",
                        items = latest,
                        sourceId = source.id
                    ))
                }
            } catch (e: Exception) {
                errors.add("${source.name}: ${e.message}")
                android.util.Log.e("HomeViewModel", "Failed to load ${source.name}: ${e.message}", e)
            }
        }

        if (sections.isEmpty() && errors.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Manga sources failed:\n${errors.joinToString("\n")}")
        }

        return sections
    }

    private suspend fun loadVideoContent(): List<VideoSection> {
        val sections = mutableListOf<VideoSection>()
        val errors = mutableListOf<String>()

        android.util.Log.d("HomeViewModel", "Loading video from ${sourceManager.getAllVideoSources().size} sources")

        sourceManager.getAllVideoSources().forEach { source ->
            android.util.Log.d("HomeViewModel", "Loading video source: ${source.name}")
            try {
                val homeSections = source.getHomePage()
                android.util.Log.d("HomeViewModel", "${source.name}: ${homeSections.size} sections loaded")
                homeSections.forEach { section ->
                    android.util.Log.d("HomeViewModel", "${source.name} - ${section.name}: ${section.items.size} items")
                    sections.add(VideoSection(
                        title = "${source.name} - ${section.name}",
                        items = section.items.take(10),
                        sourceId = source.id
                    ))
                }
            } catch (e: Exception) {
                errors.add("${source.name}: ${e.message}")
                android.util.Log.e("HomeViewModel", "Failed to load ${source.name}: ${e.message}", e)
            }
        }

        if (sections.isEmpty() && errors.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Video sources failed:\n${errors.joinToString("\n")}")
        }

        return sections
    }

    fun refresh() {
        loadHomeContent()
    }
}

data class HomeUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val mangaSections: List<MangaSection> = emptyList(),
    val videoSections: List<VideoSection> = emptyList()
)

data class MangaSection(
    val title: String,
    val items: List<Manga>,
    val sourceId: String
)

data class VideoSection(
    val title: String,
    val items: List<Video>,
    val sourceId: String
)
