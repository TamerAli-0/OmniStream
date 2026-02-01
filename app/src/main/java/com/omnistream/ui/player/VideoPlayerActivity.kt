package com.omnistream.ui.player

import android.app.PictureInPictureParams
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import android.graphics.Color
import android.graphics.Typeface
import androidx.media3.common.text.Cue
import com.omnistream.R

/**
 * Activity-based video player with subtitle support and custom styling.
 *
 * This Activity provides:
 * - External subtitle file loading (SRT/VTT)
 * - Custom subtitle styling (white text, semi-transparent background, outline)
 * - Picture-in-Picture support (enhanced in Plan 03)
 * - Quality selection infrastructure (enhanced in Plan 03)
 */
@UnstableApi
class VideoPlayerActivity : AppCompatActivity() {

    private lateinit var player: ExoPlayer
    private lateinit var playerView: PlayerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)

        // Initialize views
        playerView = findViewById(R.id.player_view)

        // Initialize ExoPlayer
        player = ExoPlayer.Builder(this).build()
        playerView.player = player

        // Configure subtitle styling
        configureSubtitleView()

        // Setup immersive mode
        setupImmersiveMode()
    }

    override fun onStart() {
        super.onStart()
        updatePictureInPictureParams()
    }

    /**
     * Configure Picture-in-Picture parameters.
     *
     * Sets dynamic aspect ratio from video format and enables auto-enter on Android 12+.
     * Pattern from RESEARCH.md lines 462-535.
     */
    private fun updatePictureInPictureParams() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val params = PictureInPictureParams.Builder().apply {
                // Get dynamic aspect ratio from video
                val videoFormat = player.videoFormat
                if (videoFormat != null) {
                    setAspectRatio(Rational(videoFormat.width, videoFormat.height))
                } else {
                    setAspectRatio(Rational(16, 9)) // Fallback
                }

                // Auto-enter on Android 12+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setAutoEnterEnabled(true)
                }
            }.build()

            setPictureInPictureParams(params)
        }
    }

    /**
     * Handle user leaving the app via home button.
     *
     * For Android < 12, manually enter PiP mode.
     * For Android 12+, auto-enter is handled by setAutoEnterEnabled.
     */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        // Only needed for Android < 12 (without setAutoEnterEnabled)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                enterPictureInPictureMode(PictureInPictureParams.Builder().build())
            }
        }
    }

    /**
     * Handle PiP mode changes.
     *
     * Hides player controls when entering PiP, shows them when exiting.
     */
    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        playerView.useController = !isInPictureInPictureMode
    }

    /**
     * Configure SubtitleView with custom styling.
     *
     * CRITICAL: Must call setApplyEmbeddedStyles(false) BEFORE setting custom styles
     * to ensure app-controlled styling takes precedence over subtitle file styles.
     *
     * Pattern from RESEARCH.md lines 252-269.
     */
    private fun configureSubtitleView() {
        playerView.subtitleView?.apply {
            // CRITICAL: Disable embedded styles FIRST
            setApplyEmbeddedStyles(false)
            setApplyEmbeddedFontSizes(false)

            // Apply custom caption style
            val customStyle = CaptionStyleCompat(
                Color.WHITE,                              // Foreground (white text)
                Color.argb(180, 0, 0, 0),                // Semi-transparent background
                Color.TRANSPARENT,                        // Window color
                CaptionStyleCompat.EDGE_TYPE_OUTLINE,    // Edge type (outline for readability)
                Color.BLACK,                              // Edge color
                Typeface.DEFAULT_BOLD                     // Typeface
            )
            setStyle(customStyle)

            // Set fixed text size (18sp absolute)
            setFixedTextSize(Cue.TEXT_SIZE_TYPE_ABSOLUTE, 18f)

            // Add bottom padding (10% of view height)
            setBottomPaddingFraction(0.1f)
        }
    }

    /**
     * Load video with optional external subtitle file.
     *
     * Uses Media3's SubtitleConfiguration API (modern approach, not deprecated MergingMediaSource).
     * Pattern from RESEARCH.md lines 439-457.
     *
     * @param videoUri URI of video file (http://, https://, file://, or content://)
     * @param subtitleUri Optional URI of subtitle file (SRT or VTT format)
     */
    fun loadVideoWithSubtitles(videoUri: Uri, subtitleUri: Uri? = null) {
        val builder = MediaItem.Builder().setUri(videoUri)

        // Add subtitle configuration if provided
        if (subtitleUri != null) {
            val mimeType = detectSubtitleMimeType(subtitleUri)

            val subtitle = MediaItem.SubtitleConfiguration.Builder(subtitleUri)
                .setMimeType(mimeType)
                .setLanguage("en")
                .setSelectionFlags(C.SELECTION_FLAG_DEFAULT)
                .setLabel("English")
                .build()

            builder.setSubtitleConfigurations(listOf(subtitle))
        }

        // Set media item and prepare player
        player.setMediaItem(builder.build())
        player.prepare()
        player.play()
    }

    /**
     * Detect subtitle MIME type from file extension.
     *
     * Supports:
     * - .srt -> application/x-subrip
     * - .vtt -> text/vtt
     * - Default to SRT if unknown
     *
     * @param uri Subtitle file URI
     * @return MIME type string
     */
    private fun detectSubtitleMimeType(uri: Uri): String {
        val uriString = uri.toString().lowercase()

        return when {
            uriString.endsWith(".vtt") -> MimeTypes.TEXT_VTT
            uriString.endsWith(".srt") -> MimeTypes.APPLICATION_SUBRIP
            else -> MimeTypes.APPLICATION_SUBRIP // Default to SRT
        }
    }

    /**
     * Setup immersive fullscreen mode for video playback.
     * Hides system bars and forces landscape orientation.
     */
    private fun setupImmersiveMode() {
        // Force landscape orientation
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

        // Keep screen on during playback
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Hide system bars
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            )
        }
    }

    override fun onPause() {
        super.onPause()
        // Don't pause video in PiP mode (video continues playing)
        if (!isInPictureInPictureMode) {
            player.pause()
        }
    }

    override fun onStop() {
        super.onStop()
        // Don't release player in PiP mode
        if (!isInPictureInPictureMode) {
            player.release()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Final cleanup - release player if not already released
        if (player.isPlaying || player.playbackState != androidx.media3.common.Player.STATE_IDLE) {
            player.release()
        }
    }
}
