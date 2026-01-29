# Technology Stack: OmniStream v2.0 New Features

**Project:** OmniStream v2.0 milestone
**Researched:** 2026-01-29
**Scope:** Stack additions for downloads, subtitles, PiP, quality selection, progress persistence, search improvements, Play Store readiness
**Confidence:** HIGH (all recommendations verified against official docs and current releases)

---

## Existing Stack (Validated -- NOT Changed)

| Technology | Role |
|---|---|
| Kotlin + Jetpack Compose + Material 3 | UI framework |
| Hilt | Dependency injection |
| Room | Local database |
| DataStore | Preferences/settings |
| OkHttp + Jsoup | Network + scraping |
| Media3 ExoPlayer | Video playback (HLS, DASH) |
| Coil | Image loading |
| Coroutines + Flow | Async/reactive |
| WorkManager | Background tasks |

---

## Required Stack Additions

### 1. Media3 Download Infrastructure (Video Episode Downloads)

| Library | Version | Purpose | Why This |
|---|---|---|---|
| `androidx.media3:media3-exoplayer-dash` | 1.9.1 | DASH offline downloads | Already using ExoPlayer; Media3's built-in `DownloadService` + `DownloadManager` + `DashDownloader` is the canonical offline solution. No third-party needed. |
| `androidx.media3:media3-exoplayer-hls` | 1.9.1 | HLS offline downloads | `HlsDownloader` handles HLS segment downloads with track selection. Same library family. |

**Integration notes:**
- You likely already depend on these modules for playback. The download capability is in the SAME artifacts -- no new dependencies, just new APIs.
- Key classes: `DownloadService`, `DownloadManager`, `DownloadHelper`, `Cache` (SimpleCache), `CacheDataSource.Factory`
- Use `WorkManagerScheduler` (from existing WorkManager) to schedule download resumption after network loss.
- Store download state in Room via `DownloadIndex` or a custom table that mirrors it.
- `minSdk` raised to 23 in Media3 1.9.0 -- verify your project targets API 23+.

**What NOT to use:**
- Do NOT use Android's built-in `DownloadManager` (android.app.DownloadManager) for video. It lacks HLS/DASH segment awareness and track selection. Media3's download system understands adaptive streaming manifests.
- Do NOT use a third-party download library (e.g., Fetch, PRDownloader). Media3 already handles everything including cache, resume, and queue management for media files.

### 2. Manga/Chapter Image Downloads (Offline Reading)

| Library | Version | Purpose | Why This |
|---|---|---|---|
| (No new library) | -- | Chapter image downloads | Use existing OkHttp for downloading + save to app-specific storage. Coil's disk cache is for caching, not for permanent offline storage. |

**Implementation approach:**
- Use OkHttp directly to download chapter images to `context.filesDir` or `context.getExternalFilesDir()` organized as `/downloads/manga/{title}/{chapter}/page_{n}.jpg`.
- Track download state (queued, downloading, complete, failed) in a new Room entity.
- Use WorkManager `CoroutineWorker` with `setForeground()` for download notifications.
- For progress tracking: wrap OkHttp's `ResponseBody` with a `ForwardingSource` to count bytes and emit progress via `setProgress()`.
- Resume support: use HTTP `Range` header for partial downloads (server must support `Accept-Ranges`).
- For offline reading: load images from local files via Coil using `File` or `Uri` image requests.

**What NOT to use:**
- Do NOT rely on Coil's disk cache as a download mechanism. The disk cache is an LRU cache that evicts entries. Downloaded content must be saved to persistent app storage separately.
- Do NOT use `DocumentFile`/SAF for primary storage -- it is slow and complex. Use app-specific storage (`filesDir` / `getExternalFilesDir`). Only offer SAF as an optional export feature.

### 3. Subtitle Support

| Library | Version | Purpose | Why This |
|---|---|---|---|
| (No new library) | -- | SRT, VTT, embedded subtitle rendering | Media3 ExoPlayer has built-in subtitle support for SRT, WebVTT, TTML, and embedded tracks. No additional dependency required. |

**Implementation approach:**
- **Sidecar subtitles (SRT/VTT files from scraped sources):** Use `MediaItem.SubtitleConfiguration.Builder` to attach external subtitle URIs to `MediaItem`. Set MIME type (`MimeTypes.TEXT_VTT` or `MimeTypes.APPLICATION_SUBRIP`) and language.
- **Embedded subtitles (in-stream):** ExoPlayer auto-detects embedded text tracks. Expose them via `TrackSelectionParameters` for user selection.
- **New subtitle pipeline** (default since Media3 1.4.0): Improved clipping, overlapping subtitle handling, and large file performance. Do NOT use the deprecated `MergingMediaSource` approach -- use `SubtitleConfiguration` on `MediaItem.Builder` instead or you will hit `IllegalStateException: Legacy decoding is disabled`.
- Render via `PlayerView`'s built-in `SubtitleView`. Customizable via `CaptionStyleCompat`.

**What NOT to use:**
- Do NOT add a third-party subtitle parser. ExoPlayer handles SRT, VTT, TTML, SSA/ASS natively.
- Do NOT use `MergingMediaSource` for sidecar subtitles -- it triggers legacy decoding errors in Media3 1.4.0+.

### 4. Picture-in-Picture (PiP)

| Library | Version | Purpose | Why This |
|---|---|---|---|
| (No new library) | -- | PiP for video playback | PiP is a platform API (Android 8.0+, API 26). Jetpack Compose has first-class PiP support with `rememberIsInPipMode()`. |

**Implementation approach:**
- Add `android:supportsPictureInPicture="true"` and `android:configChanges="orientation|screenLayout|screenSize|smallestScreenSize"` to the video player Activity in `AndroidManifest.xml`.
- Use `PictureInPictureParams.Builder` with:
  - `setAspectRatio()` from video dimensions (must be between 1:2.39 and 2.39:1 or app crashes).
  - `setAutoEnterEnabled(true)` for Android 12+ auto-enter on home press.
  - `setSourceRectHint()` from Compose `onGloballyPositioned` for smooth transition animation.
- Use `rememberIsInPipMode()` composable to conditionally hide UI controls when in PiP.
- Add `RemoteAction` buttons (play/pause, skip) via `BroadcastReceiver` + `DisposableEffect`.
- If using `MediaSession` (already in Media3), default transport controls appear automatically in PiP.

**What NOT to use:**
- Do NOT use the old View-based `onPictureInPictureModeChanged()` callback pattern. Use the Compose-aware `rememberIsInPipMode()` approach.

### 5. Video Quality Selection

| Library | Version | Purpose | Why This |
|---|---|---|---|
| (No new library) | -- | Manual quality picker | Media3's `DefaultTrackSelector` + `TrackSelectionParameters` handle this entirely. |

**Implementation approach:**
- Access available tracks via `player.currentTracks` to enumerate video qualities (resolution, bitrate).
- Build a Compose bottom sheet / dialog displaying quality options (Auto, 1080p, 720p, 480p, 360p).
- Apply selection via `player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().setMaxVideoSize(width, height).build()`.
- For "Auto" mode: clear constraints so `DefaultTrackSelector` uses adaptive ABR.
- Persist user's preferred quality in DataStore. Apply on player initialization.

**What NOT to use:**
- Do NOT replace `DefaultTrackSelector` with a custom implementation. Constraint-based selection via `TrackSelectionParameters` is the supported approach and carries across playlist items automatically.

### 6. Continue Watching / Reading Progress

| Library | Version | Purpose | Why This |
|---|---|---|---|
| (No new library) | -- | Persist playback/reading position | Room (existing) for local persistence. DataStore (existing) for lightweight current-session state. |

**Implementation approach:**
- **Room entity:** `WatchProgress(contentId, sourceUrl, episodeId, positionMs, durationMs, percentage, lastWatched)` and `ReadProgress(contentId, chapterId, pageIndex, totalPages, percentage, lastRead)`.
- **Save triggers:** For video, save on `onPause`/`onStop` and periodically (every 10-30s) during playback. For manga, save on page turn.
- **Resume:** On player init, seek to `positionMs` if `percentage < 95%`. For manga, scroll to `pageIndex`.
- **Continue Watching UI:** Query Room for items sorted by `lastWatched DESC` with `percentage BETWEEN 5 AND 95`.
- **Sync with backend:** Expose progress via Node.js backend API for cross-device sync (already has auth infrastructure).

**What NOT to use:**
- Do NOT store progress in DataStore alone. DataStore is not designed for frequently-updated structured data with queries. Room is correct for this.

### 7. Search Debounce + History

| Library | Version | Purpose | Why This |
|---|---|---|---|
| (No new library) | -- | Debounced search, search history | Kotlin Flow's `debounce()` operator + Room for history persistence. All existing stack. |

**Implementation approach:**
- **Debounce:** In ViewModel, chain `searchQuery.debounce(400).filter { it.length >= 2 }.distinctUntilChanged().flatMapLatest { query -> searchRepository.search(query) }`.
- **Search history Room entity:** `SearchHistory(id, query, timestamp, resultCount)`. Query with `ORDER BY timestamp DESC LIMIT 20`.
- **Optional FTS4:** If searching local favorites/downloads, add `@Fts4(contentEntity = ...)` annotation to a shadow entity for fast full-text matching with `MATCH`. This is built into Room (2.1.0+), no new dependency.
- **Suggestions UI:** Combine recent history + live results in a single Compose `LazyColumn` with section headers.

**What NOT to use:**
- Do NOT add a dedicated search library (e.g., Algolia, MeiliSearch). The app searches scraped sources via HTTP -- debounce is the only improvement needed on the client side.
- Do NOT use `Handler.postDelayed` for debounce. Flow's `debounce()` is the idiomatic Kotlin approach and integrates cleanly with the existing coroutines stack.

### 8. Play Store Readiness

| Tool/Config | Purpose | Notes |
|---|---|---|
| R8 (built-in AGP) | Code shrinking, obfuscation | Already in AGP. Enable with `isMinifyEnabled = true` in release build type. Use `proguard-android-optimize.txt` as base. |
| `signingConfigs` in `build.gradle.kts` | Release signing | Use Android Keystore. Store keystore password in `local.properties` (gitignored) or environment variables. NEVER commit keystore passwords. |
| Android App Bundle (`.aab`) | Play Store upload format | Default since AGP 4.0+. Produces optimized per-device APKs. |
| `baseline-profiles` | Startup performance | Optional but recommended. Uses Macrobenchmark library to generate AOT compilation profiles. |
| LeakCanary (debug only) | Memory leak detection | `debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")` -- catches leaks before release. |

**R8/ProGuard rules needed for existing stack:**

```proguard
# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# Jsoup
-keep class org.jsoup.** { *; }

# Room (auto-generated, but verify)
-keep class * extends androidx.room.RoomDatabase

# Hilt
# Handled automatically by Hilt's Gradle plugin

# Media3 ExoPlayer
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Coil
-keep class coil3.** { *; }

# Kotlin Serialization (if used)
-keepattributes *Annotation*
-keep class kotlinx.serialization.** { *; }
```

**Critical Play Store policy considerations:**
- Content scraping apps are high-risk for policy violations. The app aggregates content from third-party sources without explicit licensing.
- Mitigation strategies: Present as a "browser/reader" not a "downloader." Do NOT cache or redistribute copyrighted content on your servers. Implement DMCA takedown compliance. Consider a content-source plugin architecture so the core app is source-agnostic.
- Implement Play Store required disclosures: privacy policy, data safety section, content rating questionnaire.
- Test with `bundletool` locally before uploading to Play Console.

**What NOT to add:**
- Do NOT add ProGuard (standalone). R8 is the replacement and is built into AGP.
- Do NOT add Firebase Crashlytics for initial release unless you want analytics. Start with Play Console's built-in ANR/crash reporting (Android Vitals).

---

## Version Summary: All Dependencies

```kotlin
// build.gradle.kts (app module)

val media3Version = "1.9.1"

// === EXISTING (ensure updated to current) ===
implementation("androidx.media3:media3-exoplayer:$media3Version")
implementation("androidx.media3:media3-exoplayer-hls:$media3Version")
implementation("androidx.media3:media3-exoplayer-dash:$media3Version")
implementation("androidx.media3:media3-ui:$media3Version")
implementation("androidx.media3:media3-session:$media3Version")   // For PiP MediaSession controls

// === NEW (only if not already present) ===
// None required -- all features are covered by existing dependencies

// === DEV/DEBUG ONLY ===
debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
```

**Key insight: Zero new production dependencies are needed.** Every v2.0 feature is achievable with the existing stack. The only additions are:
- Bumping Media3 to 1.9.1 (from whatever current version)
- Adding `media3-session` if not already present (for PiP remote actions)
- LeakCanary for debug builds (Play Store readiness)
- R8 configuration (build config, not a dependency)

---

## Alternatives Considered and Rejected

| Feature | Alternative | Why Rejected |
|---|---|---|
| Video downloads | Android DownloadManager | No HLS/DASH segment awareness, no track selection, no cache integration |
| Video downloads | Fetch library | Redundant -- Media3 has full download infrastructure with queue, resume, notification |
| Image downloads | Coil disk cache | LRU cache evicts content. Downloads must persist permanently in app storage. |
| Subtitles | Third-party parser (SubtitleCollapser, etc.) | ExoPlayer handles SRT, VTT, TTML, SSA natively since Media3 1.4.0 |
| Search debounce | RxJava | Project uses Coroutines/Flow. Adding RxJava would create a parallel reactive stack for no benefit. |
| PiP | Third-party PiP library | Platform API is straightforward. Compose support is built-in. |
| Quality selection | Custom TrackSelector | DefaultTrackSelector with constraint overrides is the supported pattern |
| Crash reporting | Firebase Crashlytics | Play Console Android Vitals is sufficient initially. Add Crashlytics later if needed. |
| Storage | SAF (Storage Access Framework) | Slow, complex UX. Use app-specific storage. Offer SAF only as optional export. |
| Full-text search | Algolia / MeiliSearch | Overkill. Room FTS4 handles local search. Remote search is just HTTP to scraped sources. |

---

## Architecture Impact Summary

| Feature | New Room Entities | New Workers | New Services | Manifest Changes |
|---|---|---|---|---|
| Video downloads | DownloadState | DownloadWorker | DownloadService (Media3) | foregroundServiceType="dataSync" |
| Chapter downloads | ChapterDownload, PageDownload | ChapterDownloadWorker | -- (uses WorkManager foreground) | foregroundServiceType="dataSync" |
| Subtitles | -- | -- | -- | -- |
| PiP | -- | -- | -- | supportsPictureInPicture, configChanges |
| Quality selection | -- (use DataStore) | -- | -- | -- |
| Progress tracking | WatchProgress, ReadProgress | -- | -- | -- |
| Search history | SearchHistory, (optional FTS shadow) | -- | -- | -- |
| Play Store | -- | -- | -- | Various metadata |

---

## Sources

- [Media3 Downloads Guide](https://developer.android.com/media/media3/exoplayer/downloading-media) -- Official Android documentation for offline media downloads
- [Media3 Releases](https://developer.android.com/jetpack/androidx/releases/media3) -- Version 1.9.1 released January 26, 2026
- [Media3 Track Selection](https://developer.android.com/media/media3/exoplayer/track-selection) -- Official track selection API documentation
- [Media3 Supported Formats](https://developer.android.com/media/media3/exoplayer/supported-formats) -- Subtitle format support (SRT, VTT, TTML)
- [Compose PiP Setup](https://developer.android.com/develop/ui/compose/system/pip-setup) -- Official Jetpack Compose PiP guide
- [Compose PiP Remote Actions](https://developer.android.com/develop/ui/compose/system/pip-remote-actions) -- Adding controls to PiP window
- [WorkManager Long-Running Workers](https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running) -- Foreground service + notification patterns
- [WorkManager Progress Observation](https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/observe) -- setProgress API for download progress
- [Room FTS4](https://developer.android.com/training/data-storage/room/defining-data) -- Full-text search with Room entities
- [R8 Keep Rules (Nov 2025)](https://android-developers.googleblog.com/2025/11/configure-and-troubleshoot-r8-keep-rules.html) -- Official Android blog on R8 configuration
- [Coil 3 Documentation](https://coil-kt.github.io/coil/) -- Latest Coil 3.3.0 docs
- [Kotlin Flow Debounce Operators (Nov 2025)](https://medium.com/droidstack/debounce-throttle-sample-in-kotlin-flow-when-to-use-which-86b3781038d9) -- Flow operator patterns for search
