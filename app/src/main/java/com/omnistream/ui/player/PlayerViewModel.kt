package com.omnistream.ui.player

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.omnistream.data.local.DownloadDao
import com.omnistream.data.local.WatchHistoryEntity
import com.omnistream.data.repository.WatchHistoryRepository
import com.omnistream.domain.model.Episode
import com.omnistream.domain.model.VideoLink
import com.omnistream.source.SourceManager
import com.omnistream.source.model.VideoType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val sourceManager: SourceManager,
    private val watchHistoryRepository: WatchHistoryRepository,
    private val downloadDao: DownloadDao,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sourceId: String = savedStateHandle["sourceId"] ?: ""
    private val videoId: String = java.net.URLDecoder.decode(savedStateHandle["videoId"] ?: "", "UTF-8")
    private val episodeId: String = java.net.URLDecoder.decode(savedStateHandle["episodeId"] ?: "", "UTF-8")
    private val videoTitle: String = savedStateHandle["title"] ?: ""
    private val coverUrl: String? = savedStateHandle["coverUrl"]

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        loadVideoLinks()
        loadSavedProgress()
    }

    private fun loadVideoLinks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            android.util.Log.d("PlayerViewModel", "Loading video links: sourceId=$sourceId, videoId=$videoId, episodeId=$episodeId")

            try {
                // Check if episode is downloaded for offline playback
                val downloadId = "video_${sourceId}_${videoId}_${episodeId}"
                val downloadEntity = downloadDao.getById(downloadId)

                if (downloadEntity != null && downloadEntity.status == "completed") {
                    val localFile = File(downloadEntity.filePath)
                    if (localFile.exists()) {
                        val localLink = VideoLink(
                            url = "file://${downloadEntity.filePath}",
                            quality = "Downloaded",
                            extractorName = "offline",
                            isM3u8 = false,
                            isDash = false
                        )
                        android.util.Log.d("PlayerViewModel", "Using offline video: ${localFile.absolutePath}")

                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            links = listOf(localLink),
                            episodeTitle = videoTitle.ifBlank { "Episode" },
                            selectedLink = localLink,
                            isOffline = true
                        )
                        return@launch
                    }
                }

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
                    episodeTitle = videoTitle.ifBlank { episode.title ?: "Episode ${episode.number}" },
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

    private fun loadSavedProgress() {
        viewModelScope.launch {
            val saved = watchHistoryRepository.getProgress(videoId, sourceId)
            if (saved != null && !saved.isCompleted && saved.progressPosition > 0) {
                _uiState.value = _uiState.value.copy(
                    savedPosition = saved.progressPosition,
                    showResumeDialog = true
                )
            }
        }
    }

    fun dismissResumeDialog() {
        _uiState.value = _uiState.value.copy(showResumeDialog = false)
    }

    fun startFromBeginning() {
        _uiState.value = _uiState.value.copy(showResumeDialog = false, savedPosition = null)
    }

    fun saveVideoProgress(positionMs: Long, durationMs: Long) {
        if (durationMs <= 0) return
        val percentage = positionMs.toFloat() / durationMs
        val isCompleted = percentage > 0.90f
        viewModelScope.launch {
            watchHistoryRepository.upsert(
                WatchHistoryEntity(
                    id = "$sourceId:$videoId",
                    contentId = videoId,
                    sourceId = sourceId,
                    contentType = "video",
                    title = videoTitle.ifBlank { _uiState.value.episodeTitle ?: "Unknown" },
                    coverUrl = coverUrl,
                    episodeId = episodeId,
                    progressPosition = positionMs,
                    totalDuration = durationMs,
                    progressPercentage = percentage,
                    lastWatchedAt = System.currentTimeMillis(),
                    isCompleted = isCompleted
                )
            )
        }
    }
}

data class PlayerUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val links: List<VideoLink> = emptyList(),
    val selectedLink: VideoLink? = null,
    val episodeTitle: String? = null,
    val isOffline: Boolean = false,
    val savedPosition: Long? = null,
    val showResumeDialog: Boolean = false
)
