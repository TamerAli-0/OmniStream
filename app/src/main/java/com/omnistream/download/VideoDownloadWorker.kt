package com.omnistream.download

import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.omnistream.data.local.DownloadDao
import com.omnistream.core.network.OmniHttpClient
import com.omnistream.domain.model.Episode
import com.omnistream.source.SourceManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File

@HiltWorker
class VideoDownloadWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadDao: DownloadDao,
    private val httpClient: OmniHttpClient,
    private val notificationHelper: DownloadNotificationHelper,
    private val sourceManager: SourceManager
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "VideoDownloadWorker"
        private const val BUFFER_SIZE = 8192
        private const val NOTIFICATION_THROTTLE_MS = 1000L
    }

    override suspend fun doWork(): Result {
        val downloadId = inputData.getString("download_id") ?: return Result.failure()
        val sourceId = inputData.getString("source_id") ?: return Result.failure()
        val contentId = inputData.getString("content_id") ?: return Result.failure()
        val episodeId = inputData.getString("episode_id") ?: return Result.failure()
        val title = inputData.getString("title") ?: "Download"
        val filePath = inputData.getString("file_path") ?: return Result.failure()
        var videoUrl = inputData.getString("video_url") ?: ""
        var referer = inputData.getString("referer") ?: ""

        return try {
            // If no video URL provided, resolve it from the source
            if (videoUrl.isBlank()) {
                val resolved = resolveVideoUrl(sourceId, contentId, episodeId)
                if (resolved == null) {
                    Log.e(TAG, "Could not resolve video URL for $episodeId")
                    downloadDao.updateProgress(downloadId, 0f, "failed")
                    return Result.failure()
                }
                videoUrl = resolved.first
                if (referer.isBlank()) referer = resolved.second
            }

            // Promote to foreground with progress notification
            setForeground(createForegroundInfo(downloadId, title, 0f))

            // Mark as downloading
            downloadDao.updateProgress(downloadId, 0f, "downloading")

            // Create output file and parent directories
            val outputFile = File(filePath)
            outputFile.parentFile?.mkdirs()

            // Resume support: check existing file size for HTTP Range header
            val existingSize = if (outputFile.exists()) outputFile.length() else 0L
            val headers = mutableMapOf<String, String>()
            if (existingSize > 0) {
                headers["Range"] = "bytes=$existingSize-"
                Log.d(TAG, "Resuming download from byte $existingSize")
            }

            // Download the video file
            val response = httpClient.getRaw(videoUrl, headers = headers, referer = referer.ifBlank { null })
            response.use { resp ->
                if (!resp.isSuccessful && resp.code != 206) {
                    Log.e(TAG, "Failed to download video: HTTP ${resp.code}")
                    downloadDao.updateProgress(downloadId, 0f, "failed")
                    return Result.failure()
                }

                val contentLength = resp.body?.contentLength() ?: -1L
                val totalSize = if (contentLength > 0) contentLength + existingSize else -1L

                val inputStream = resp.body?.byteStream()
                    ?: throw Exception("Empty response body")

                // Open in append mode if resuming, otherwise write mode
                val outputStream = if (existingSize > 0) {
                    outputFile.outputStream().apply {
                        // Seek to end by opening in append mode
                        close()
                    }
                    java.io.FileOutputStream(outputFile, true)
                } else {
                    outputFile.outputStream()
                }

                inputStream.use { input ->
                    outputStream.use { output ->
                        val buffer = ByteArray(BUFFER_SIZE)
                        var bytesRead: Int
                        var totalBytesWritten = existingSize
                        var lastNotificationUpdate = 0L

                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            // Check cancellation
                            if (isStopped) {
                                val progress = if (totalSize > 0) {
                                    totalBytesWritten.toFloat() / totalSize
                                } else 0f
                                downloadDao.updateProgress(downloadId, progress, "paused")
                                Log.d(TAG, "Download paused at $totalBytesWritten bytes")
                                return Result.failure()
                            }

                            output.write(buffer, 0, bytesRead)
                            totalBytesWritten += bytesRead

                            val progress = if (totalSize > 0) {
                                totalBytesWritten.toFloat() / totalSize
                            } else 0f

                            // Rate-limit progress updates to every 1 second
                            val now = System.currentTimeMillis()
                            if (now - lastNotificationUpdate > NOTIFICATION_THROTTLE_MS ||
                                (totalSize > 0 && totalBytesWritten >= totalSize)
                            ) {
                                setProgress(workDataOf("progress" to progress))
                                downloadDao.updateProgress(downloadId, progress, "downloading")
                                setForeground(createForegroundInfo(downloadId, title, progress))
                                lastNotificationUpdate = now
                            }
                        }
                    }
                }
            }

            // Update entity with final progress and file size
            val finalSize = outputFile.length()
            val entity = downloadDao.getById(downloadId)
            if (entity != null) {
                downloadDao.upsert(entity.copy(
                    progress = 1f,
                    status = "completed",
                    fileSize = finalSize
                ))
            } else {
                downloadDao.updateProgress(downloadId, 1f, "completed")
            }

            Log.d(TAG, "Download complete: $filePath ($finalSize bytes)")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Download failed for $downloadId", e)
            downloadDao.updateProgress(downloadId, 0f, "failed")
            Result.retry()
        }
    }

    /**
     * Resolve the video URL by fetching links from the source.
     * Returns pair of (videoUrl, referer) or null if not found.
     */
    private suspend fun resolveVideoUrl(
        sourceId: String,
        contentId: String,
        episodeId: String
    ): Pair<String, String>? {
        val source = sourceManager.getVideoSource(sourceId) ?: return null

        // Build episode URL same way as PlayerViewModel
        val episodeUrl = when (sourceId) {
            "gogoanime" -> "${source.baseUrl}/$episodeId/"
            "vidsrc" -> episodeId
            else -> "${source.baseUrl}/episode/$episodeId"
        }

        val seasonEpisodeMatch = Regex("""_s(\d+)_e(\d+)""").find(episodeId)
        val season = seasonEpisodeMatch?.groupValues?.get(1)?.toIntOrNull()
        val episodeNum = seasonEpisodeMatch?.groupValues?.get(2)?.toIntOrNull()
            ?: extractEpisodeNumber(episodeId)

        val episode = Episode(
            id = episodeId,
            videoId = contentId,
            sourceId = sourceId,
            url = episodeUrl,
            number = episodeNum,
            season = season
        )

        val links = source.getLinks(episode)
        if (links.isEmpty()) return null

        // Prefer direct MP4 links over HLS/DASH
        val bestLink = links.firstOrNull { !it.isM3u8 && !it.isDash }
            ?: links.firstOrNull()
            ?: return null

        return Pair(bestLink.url, bestLink.referer ?: source.baseUrl)
    }

    private fun extractEpisodeNumber(episodeId: String): Int {
        return Regex("""(\d+)""").findAll(episodeId).lastOrNull()
            ?.groupValues?.get(1)?.toIntOrNull() ?: 1
    }

    private fun createForegroundInfo(downloadId: String, title: String, progress: Float): ForegroundInfo {
        val notification = notificationHelper.buildProgressNotification(title, progress, downloadId)
        val notificationId = downloadId.hashCode()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(notificationId, notification)
        }
    }
}
