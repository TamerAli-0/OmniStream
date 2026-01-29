package com.omnistream.ui.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnistream.domain.model.Episode
import com.omnistream.domain.model.VideoLink
import com.omnistream.source.SourceManager
import com.omnistream.source.model.VideoType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val sourceManager: SourceManager,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sourceId: String = savedStateHandle["sourceId"] ?: ""
    private val videoId: String = java.net.URLDecoder.decode(savedStateHandle["videoId"] ?: "", "UTF-8")
    private val episodeId: String = java.net.URLDecoder.decode(savedStateHandle["episodeId"] ?: "", "UTF-8")

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        loadVideoLinks()
    }

    private fun loadVideoLinks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            android.util.Log.d("PlayerViewModel", "Loading video links: sourceId=$sourceId, videoId=$videoId, episodeId=$episodeId")

            try {
                val source = sourceManager.getVideoSource(sourceId)
                    ?: throw Exception("Source not found: $sourceId")

                // Construct episode URL based on source type
                val episodeUrl = when (sourceId) {
                    "gogoanime" -> "${source.baseUrl}/$episodeId/"  // Trailing slash required
                    "vidsrc" -> episodeId // VidSrc episode ID contains the full embed URL path
                    else -> "${source.baseUrl}/episode/$episodeId"
                }
                android.util.Log.d("PlayerViewModel", "Constructed episode URL: $episodeUrl")

                // Extract season and episode numbers from episodeId (format: tv-12345_s1_e5)
                val seasonEpisodeMatch = Regex("""_s(\d+)_e(\d+)""").find(episodeId)
                val season = seasonEpisodeMatch?.groupValues?.get(1)?.toIntOrNull()
                val episodeNum = seasonEpisodeMatch?.groupValues?.get(2)?.toIntOrNull()
                    ?: extractEpisodeNumber(episodeId)

                // Create episode object
                val episode = Episode(
                    id = episodeId,
                    videoId = videoId,
                    sourceId = sourceId,
                    url = episodeUrl,
                    number = episodeNum,
                    season = season
                )

                // Get video links
                val links = source.getLinks(episode)
                android.util.Log.d("PlayerViewModel", "Loaded ${links.size} video links")

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    links = links,
                    episodeTitle = episode.title ?: "Episode ${episode.number}",
                    // Auto-select first link
                    selectedLink = links.firstOrNull()
                )

            } catch (e: Exception) {
                android.util.Log.e("PlayerViewModel", "Failed to load video links", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load video"
                )
            }
        }
    }

    private fun extractEpisodeNumber(episodeId: String): Int {
        return Regex("""(\d+)""").findAll(episodeId).lastOrNull()?.groupValues?.get(1)?.toIntOrNull() ?: 1
    }

    fun selectLink(link: VideoLink) {
        _uiState.value = _uiState.value.copy(selectedLink = link)
    }
}

data class PlayerUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val links: List<VideoLink> = emptyList(),
    val selectedLink: VideoLink? = null,
    val episodeTitle: String? = null
)
