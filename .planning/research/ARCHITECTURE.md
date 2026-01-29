# Architecture Patterns: OmniStream v2.0 Feature Integration

**Domain:** Android media streaming app (MVVM + Clean Architecture)
**Researched:** 2026-01-29
**Overall confidence:** HIGH (based on direct codebase analysis)

## Current Architecture Snapshot

```
com.omnistream/
  di/          AppModule (Hilt singleton providers)
  data/
    local/     AppDatabase, FavoriteEntity/Dao, UserPreferences (DataStore)
    remote/    ApiService, AuthDtos, SyncDtos
    repository/ AuthRepository, SyncRepository
  domain/
    model/     Video, Episode, VideoLink, Subtitle, Manga, Chapter, Page
  source/
    model/     VideoSource, MangaSource, Extractor interfaces
    anime/     AnimeKaiSource, GogoAnimeSource (paused)
    manga/     MangaDexSource, ManhuaPlusSource, AsuraComicSource
    movie/     FlickyStreamSource, WatchFlixSource, VidSrcSource, GoojaraSource
    SourceManager.kt
  core/
    network/   OmniHttpClient
    parser/    JsoupExtensions
    crypto/    CryptoUtils
    webview/   WebViewExtractor
  ui/
    navigation/ OmniNavigation (NavHost + bottom nav)
    home/       HomeScreen/ViewModel
    browse/     BrowseScreen/ViewModel
    search/     SearchScreen/ViewModel
    library/    LibraryScreen/ViewModel
    downloads/  DownloadsScreen (placeholder)
    detail/     MangaDetail, VideoDetail (Screen/ViewModel)
    reader/     ReaderScreen/ViewModel
    player/     PlayerScreen/ViewModel, components/, dialogs/
    settings/   SettingsScreen/ViewModel
    auth/       AccessGate, Login, Register (Screen/ViewModel)
```

**Key architectural facts:**
- Room DB v1 has only `FavoriteEntity` (single table, no migrations yet)
- Player already has subtitles, speed, resize, source selection, and PiP button wired up
- DownloadsScreen is a placeholder (empty state, no ViewModel)
- SearchViewModel has no history persistence -- it is purely in-memory
- No watch history or progress tracking exists anywhere
- Navigation uses string routes with URL-encoded arguments
- WorkManager is listed as a dependency but not used

---

## Feature 1: Downloads System

### Integration Points

**Touches existing:**
| File | Change Type | What Changes |
|------|-------------|--------------|
| `AppDatabase.kt` | MODIFY | Add DownloadEntity, DownloadDao; bump version to 2 with migration |
| `AppModule.kt` | MODIFY | Provide DownloadDao, DownloadRepository |
| `DownloadsScreen.kt` | REWRITE | Replace placeholder with real download list UI |
| `VideoDetailScreen.kt` | MODIFY | Add download button per episode |
| `MangaDetailScreen.kt` | MODIFY | Add download button per chapter |
| `PlayerScreen.kt` | MODIFY | Support playing from local file path instead of URL |
| `ReaderScreen.kt` | MODIFY | Support loading pages from local storage |
| `OmniNavigation.kt` | NO CHANGE | Downloads route already exists |

**New components needed:**

| Component | Layer | Purpose |
|-----------|-------|---------|
| `DownloadEntity.kt` | data/local | Room entity: id, contentId, sourceId, contentType, title, coverUrl, status (QUEUED/DOWNLOADING/PAUSED/COMPLETED/FAILED), progress (0-100), filePath, totalBytes, downloadedBytes, createdAt |
| `DownloadDao.kt` | data/local | CRUD + Flow queries for status-based filtering, progress updates |
| `DownloadRepository.kt` | data/repository | Coordinates between DAO, WorkManager, and file system |
| `DownloadWorker.kt` | data/worker | WorkManager worker -- fetches video/manga content, writes to app internal storage |
| `DownloadsViewModel.kt` | ui/downloads | Observes downloads via Flow, handles pause/resume/delete/retry |
| `DownloadNotificationManager.kt` | core/ | Foreground notification for active downloads with progress |

**Data flow:**

```
User taps "Download" on VideoDetailScreen
  -> VideoDetailViewModel calls DownloadRepository.enqueue(episode)
    -> DownloadRepository inserts DownloadEntity (status=QUEUED)
    -> DownloadRepository enqueues DownloadWorker via WorkManager
      -> DownloadWorker resolves video links via SourceManager
      -> DownloadWorker downloads HLS segments or progressive file via OkHttp
      -> DownloadWorker updates DownloadEntity progress periodically
      -> DownloadWorker writes to app-specific storage (context.filesDir/downloads/)
    -> DownloadsScreen observes DownloadDao.getAllDownloads() Flow

User plays downloaded content:
  -> PlayerScreen checks if local file exists for this episode
  -> If yes: uses local file:// URI instead of streaming URL
  -> If no: streams as normal
```

**Database migration strategy:**
```kotlin
// Migration 1->2: Add downloads table
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS downloads (
                id TEXT NOT NULL PRIMARY KEY,
                contentId TEXT NOT NULL,
                sourceId TEXT NOT NULL,
                contentType TEXT NOT NULL,
                title TEXT NOT NULL,
                coverUrl TEXT,
                episodeId TEXT,
                chapterId TEXT,
                status TEXT NOT NULL DEFAULT 'QUEUED',
                progress INTEGER NOT NULL DEFAULT 0,
                filePath TEXT,
                totalBytes INTEGER NOT NULL DEFAULT 0,
                downloadedBytes INTEGER NOT NULL DEFAULT 0,
                createdAt INTEGER NOT NULL DEFAULT 0
            )
        """)
    }
}
```

**Storage approach:**
- Video downloads: `context.filesDir/downloads/video/{sourceId}/{videoId}/{episodeId}.mp4`
- Manga downloads: `context.filesDir/downloads/manga/{sourceId}/{mangaId}/{chapterId}/page_{index}.jpg`
- Use app-internal storage (no WRITE_EXTERNAL_STORAGE permission needed)
- HLS downloads require Media3's `DownloadManager` or manual segment stitching

**Critical decision: HLS download strategy.**
Most video sources return `.m3u8` URLs. Two approaches:
1. **Media3 DownloadManager** -- Purpose-built for HLS/DASH offline. Handles segment downloading, license management, and provides `DownloadService`. This is the recommended approach because it handles the complexity of multi-segment HLS properly.
2. **Manual OkHttp download** -- Parse m3u8 playlist, download all .ts segments, concatenate. Fragile and reinvents what Media3 already provides.

**Recommendation:** Use Media3's `DownloadManager` + `DownloadService` for video. Use manual OkHttp for manga (just image files). This aligns with the existing ExoPlayer/Media3 stack.

---

## Feature 2: Subtitle Enhancements

### Integration Points

The subtitle infrastructure is **already built**. The current codebase has:
- `VideoLink.subtitles: List<Subtitle>` -- external subtitle URLs from sources
- `VideoPlayer.kt` -- creates `MergingMediaSource` with `SingleSampleMediaSource` for each subtitle
- `SubtitleSelectionSheet` -- UI for selecting subtitle tracks
- `PlayerControls` -- subtitle toggle button
- Track detection via `onTracksChanged` listener that populates `subtitleTracks`

**What already works:**
- External VTT/SRT/ASS subtitle loading via MergingMediaSource
- Embedded HLS subtitles detected automatically by ExoPlayer
- Subtitle on/off toggle via DefaultTrackSelector
- Track selection by index via TrackSelectionOverride

**What needs enhancement (if any):**

| Enhancement | Component | Effort |
|-------------|-----------|--------|
| Subtitle styling (font size, color, background) | `VideoPlayer.kt` subtitleView config, `UserPreferences.kt` for persistence | Low |
| Subtitle search/download from OpenSubtitles API | New `SubtitleSearchRepository.kt` | Medium |
| Subtitle sync offset (+/- seconds) | `PlayerViewModel.kt` + `VideoPlayer.kt` renderer offset | Medium |

**Verdict:** Subtitles are functionally complete. Any work here is polish, not core integration. The architecture does not need changes for basic subtitle support.

---

## Feature 3: Picture-in-Picture (PiP)

### Integration Points

PiP is **already partially implemented**:
- `PlayerScreen.kt` has `enterPipMode()` function using `PictureInPictureParams`
- `PlayerControls.kt` has PiP button wired to `onPipClick`
- Handles API 26+ (Android O) check

**What is missing for a complete PiP implementation:**

| Missing Piece | Component | What to Add |
|---------------|-----------|-------------|
| Auto-enter PiP on home button | `MainActivity.kt` | Override `onUserLeaveHint()` to enter PiP if player is active |
| PiP playback controls (play/pause from PiP window) | `MainActivity.kt` | Register `RemoteAction` broadcast receivers |
| Detect PiP mode to hide/show controls | `PlayerScreen.kt` | Observe `isInPictureInPictureMode` via `OnPictureInPictureModeChangedProvider` |
| Manifest declaration | `AndroidManifest.xml` | Add `android:supportsPictureInPicture="true"` to activity |
| Compose lifecycle handling | `PlayerScreen.kt` | Suppress Compose UI recomposition during PiP transition |

**Key architectural consideration:** PiP requires `Activity`-level awareness. The current architecture uses a single `MainActivity`. The `PlayerScreen` composable already has access to the activity via `LocalContext.current as? Activity`. The main challenge is bridging `Activity.onPictureInPictureModeChanged` callbacks into the Compose layer.

**Pattern to follow:**
```kotlin
// In MainActivity
override fun onUserLeaveHint() {
    super.onUserLeaveHint()
    // Check if player is active (via shared ViewModel or flag)
    if (isPlayerActive) enterPipMode()
}

// In PlayerScreen - observe PiP state
val lifecycle = LocalLifecycleOwner.current.lifecycle
DisposableEffect(lifecycle) {
    val observer = LifecycleEventObserver { _, event ->
        if (event == Lifecycle.Event.ON_PAUSE) {
            // Entered PiP or backgrounded
        }
    }
    lifecycle.addObserver(observer)
    onDispose { lifecycle.removeObserver(observer) }
}
```

**Effort:** Low-Medium. The foundation exists; it needs Activity-level lifecycle wiring.

---

## Feature 4: Quality Selection

### Integration Points

Quality selection is **already implemented** through the source selection sheet:
- `SourceSelectionSheet` in `PlayerDialogs.kt` shows all `VideoLink` objects
- Each `VideoLink` has a `quality` field ("1080p", "720p", "480p", "Auto")
- `QualityBadge` composable renders quality labels with color coding
- User selects a link, `PlayerViewModel.selectLink()` updates state, `VideoPlayer` reloads

**Current limitation:** The "quality" concept is conflated with "source". Each source/extractor returns separate `VideoLink` objects for different qualities. There is no single-stream quality switching (like adaptive HLS where you pick resolution within one stream).

**Two levels of quality selection exist:**
1. **Source-level (existing):** Switch between different `VideoLink` objects (e.g., VidCloud 1080p vs MegaCloud 720p). This works today.
2. **Track-level (not implemented):** For adaptive HLS streams, ExoPlayer auto-selects quality based on bandwidth. To let users override this:

| Component | Change |
|-----------|--------|
| `PlayerViewModel.kt` | Add function to set max video resolution via `DefaultTrackSelector.Parameters` |
| `PlayerDialogs.kt` | Add `QualitySelectionSheet` that lists available video track qualities from `player.currentTracks` |
| `VideoPlayer.kt` | Expose video track info through `onTracksChanged` listener |
| `VideoPlayerState` | Add `videoTracks: List<VideoTrack>`, `selectedVideoTrackIndex: Int` |

**Pattern for HLS quality override:**
```kotlin
trackSelector.setParameters(
    trackSelector.buildUponParameters()
        .setMaxVideoSize(1920, 1080) // Cap at 1080p
        .setForceHighestSupportedBitrate(false)
)
```

**Recommendation:** Source-level switching already works. Add track-level quality selection only if users request finer control over adaptive streams. This is a nice-to-have, not blocking.

---

## Feature 5: Continue Watching / Watch History

### Integration Points

**This is the highest-impact new feature architecturally.** Nothing exists for tracking watch progress.

**New components needed:**

| Component | Layer | Purpose |
|-----------|-------|---------|
| `WatchHistoryEntity.kt` | data/local | Room entity: id (sourceId:contentId), sourceId, contentId, contentType, title, coverUrl, episodeId/chapterId, progress (ms for video, page for manga), duration, lastWatchedAt, isCompleted |
| `WatchHistoryDao.kt` | data/local | Queries: getRecentlyWatched (ordered by lastWatchedAt), getProgress(sourceId, contentId, episodeId), upsert, markCompleted, clearHistory |
| `WatchHistoryRepository.kt` | data/repository | Abstraction over DAO, potentially syncs with backend |

**Touches existing:**

| File | Change Type | What Changes |
|------|-------------|--------------|
| `AppDatabase.kt` | MODIFY | Add WatchHistoryEntity; bump version (2 or 3 depending on build order with downloads) |
| `AppModule.kt` | MODIFY | Provide WatchHistoryDao, WatchHistoryRepository |
| `PlayerViewModel.kt` | MODIFY | Save position periodically (every 10s) and on exit |
| `PlayerScreen.kt` | MODIFY | On enter, restore last position from history |
| `ReaderViewModel.kt` | MODIFY | Save current chapter + page on exit |
| `ReaderScreen.kt` | MODIFY | On enter, restore last chapter + page |
| `VideoDetailViewModel.kt` | MODIFY | Show watch progress per episode (progress bar on episode list items) |
| `MangaDetailViewModel.kt` | MODIFY | Show read progress per chapter |
| `HomeViewModel.kt` | MODIFY | Add "Continue Watching" section at the top of home |
| `HomeScreen.kt` | MODIFY | Render continue watching row |

**Data flow for video progress saving:**

```
PlayerScreen mounts
  -> PlayerViewModel checks WatchHistoryDao.getProgress(sourceId, videoId, episodeId)
  -> If found: seek to saved position on player ready

During playback (every 10 seconds):
  -> PlayerViewModel calls WatchHistoryRepository.updateProgress(
       sourceId, videoId, episodeId, currentPositionMs, durationMs
     )

On PlayerScreen exit (DisposableEffect onDispose):
  -> Save final position

On playback complete (95% threshold):
  -> Mark episode as completed
  -> Auto-advance to next episode (future feature)
```

**Database schema:**
```kotlin
@Entity(
    tableName = "watch_history",
    indices = [Index(value = ["sourceId", "contentId", "episodeId"], unique = true)]
)
data class WatchHistoryEntity(
    @PrimaryKey
    val id: String, // "sourceId:contentId:episodeId"
    val sourceId: String,
    val contentId: String,
    val contentType: String, // "video" or "manga"
    val title: String,
    val coverUrl: String? = null,
    val episodeId: String? = null,    // null for movies
    val chapterId: String? = null,    // for manga
    val episodeTitle: String? = null,
    val progressMs: Long = 0,         // video: milliseconds, manga: unused
    val progressPage: Int = 0,        // manga: page number
    val durationMs: Long = 0,
    val totalPages: Int = 0,
    val lastWatchedAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
)
```

**Home screen integration:**
- `HomeViewModel` queries `WatchHistoryDao.getRecentlyWatched(limit = 20)` as a Flow
- Renders as the first horizontal row in HomeScreen before source sections
- Each card shows cover image, title, episode info, and a progress bar
- Tapping resumes directly (navigates to player/reader with the saved episodeId/chapterId)

---

## Feature 6: Search History

### Integration Points

**Touches existing:**

| File | Change Type | What Changes |
|------|-------------|--------------|
| `SearchViewModel.kt` | MODIFY | Save queries on search, load recent searches, clear history |
| `SearchScreen.kt` | MODIFY | Show recent searches when query is empty, add clear button |
| `AppDatabase.kt` | MODIFY | Add SearchHistoryEntity (or use DataStore for simplicity) |
| `AppModule.kt` | MODIFY | Provide SearchHistoryDao if using Room |

**Design decision: Room vs DataStore**

| Approach | Pros | Cons |
|----------|------|------|
| Room entity | Consistent with other features, queryable, unlimited history | Heavier for a simple string list |
| DataStore | Already used for preferences, simpler for small data | Limited query capability, not ideal for lists |

**Recommendation:** Use Room. The database migration is already happening for downloads and watch history, so adding a simple `search_history` table is zero incremental cost. It also enables features like "most searched" or "search suggestions" later.

**New components:**

| Component | Layer | Purpose |
|-----------|-------|---------|
| `SearchHistoryEntity.kt` | data/local | Room entity: id (auto-generated), query (unique), searchedAt |
| `SearchHistoryDao.kt` | data/local | getRecent(limit=10), insert (upsert by query text), deleteByQuery, clearAll |

**Integration into SearchViewModel:**
```kotlin
// On search execution:
fun search(query: String) {
    if (query.isBlank()) return
    searchHistoryDao.insert(SearchHistoryEntity(query = query))
    // ... existing search logic
}

// New: expose recent searches
val recentSearches: Flow<List<String>> = searchHistoryDao.getRecent(10)
    .map { entities -> entities.map { it.query } }
```

**SearchScreen UI changes:**
- When query field is empty and focused: show recent search chips/list
- Each item has an "X" to delete that single entry
- "Clear all" button in header
- Tapping a recent search populates the query and triggers search

---

## Database Migration Plan

All three new tables (downloads, watch_history, search_history) should be added in a single migration to avoid chaining multiple migrations:

```
Version 1 (current): favorites
Version 2 (target):  favorites + downloads + watch_history + search_history
```

**Single migration 1->2:**
```kotlin
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Downloads table
        db.execSQL("CREATE TABLE IF NOT EXISTS downloads (...)")
        // Watch history table
        db.execSQL("CREATE TABLE IF NOT EXISTS watch_history (...)")
        // Search history table
        db.execSQL("CREATE TABLE IF NOT EXISTS search_history (...)")
    }
}
```

This is cleaner than 1->2->3->4 and reduces migration test surface. All three tables are independent (no foreign keys between them).

---

## Suggested Build Order

Based on dependency analysis:

```
Phase 1: Database + Watch History (foundation)
  |- New Room entities + migration (all 3 tables at once)
  |- WatchHistoryRepository
  |- PlayerViewModel progress saving/restoring
  |- ReaderViewModel progress saving/restoring
  |- "Continue Watching" row on HomeScreen
  Dependencies: None (pure addition)

Phase 2: Downloads System
  |- DownloadRepository + WorkManager integration
  |- DownloadWorker (video via Media3 DownloadManager, manga via OkHttp)
  |- DownloadsViewModel + DownloadsScreen rewrite
  |- Download buttons on detail screens
  |- Offline playback support in Player/Reader
  Dependencies: Room migration from Phase 1 (tables already created)

Phase 3: Search History + Player Polish
  |- SearchHistoryDao integration into SearchViewModel
  |- SearchScreen UI for recent searches
  |- PiP completion (Activity lifecycle wiring)
  |- HLS track-level quality selection (if desired)
  |- Subtitle styling preferences
  Dependencies: Room migration from Phase 1 (table already created)
```

**Rationale for this order:**
1. **Watch history first** because it touches the most files (Player, Reader, Home, Detail screens) and establishes the database migration pattern. Every subsequent feature piggybacks on this migration.
2. **Downloads second** because it is the most complex feature (WorkManager, file I/O, Media3 DownloadManager, notification management) and can be developed independently once the database layer exists.
3. **Search history + polish last** because these are the lowest risk and lowest complexity. Search history is a small Room addition. PiP and quality selection are incremental improvements to already-working features.

---

## Component Boundary Summary

### What stays the same
- `SourceManager` -- no changes needed. Downloads and history consume source data but do not modify source behavior.
- `OmniNavigation` -- Downloads route already registered. No new routes needed (continue watching navigates to existing player/reader routes).
- Domain models (`Video`, `Episode`, `Manga`, `Chapter`, `VideoLink`) -- no changes. New entities are data-layer only.
- All source implementations -- untouched.

### What gets new dependencies
| ViewModel | Currently Injects | Will Also Inject |
|-----------|-------------------|------------------|
| `PlayerViewModel` | SourceManager | WatchHistoryRepository |
| `ReaderViewModel` | SourceManager | WatchHistoryRepository |
| `HomeViewModel` | SourceManager | WatchHistoryRepository |
| `VideoDetailViewModel` | SourceManager, FavoriteDao | WatchHistoryRepository, DownloadRepository |
| `MangaDetailViewModel` | SourceManager, FavoriteDao | WatchHistoryRepository, DownloadRepository |
| `SearchViewModel` | SourceManager | SearchHistoryDao |
| (new) `DownloadsViewModel` | -- | DownloadRepository |

### Hilt module additions to AppModule.kt
```kotlin
@Provides
fun provideDownloadDao(database: AppDatabase): DownloadDao = database.downloadDao()

@Provides
fun provideWatchHistoryDao(database: AppDatabase): WatchHistoryDao = database.watchHistoryDao()

@Provides
fun provideSearchHistoryDao(database: AppDatabase): SearchHistoryDao = database.searchHistoryDao()

@Provides
@Singleton
fun provideDownloadRepository(
    downloadDao: DownloadDao,
    sourceManager: SourceManager,
    @ApplicationContext context: Context
): DownloadRepository = DownloadRepository(downloadDao, sourceManager, context)

@Provides
@Singleton
fun provideWatchHistoryRepository(
    watchHistoryDao: WatchHistoryDao
): WatchHistoryRepository = WatchHistoryRepository(watchHistoryDao)
```

---

## Anti-Patterns to Avoid

### 1. Saving progress on every frame
**Bad:** Update Room on every position change (500ms polling loop)
**Good:** Debounce to every 10-15 seconds and on lifecycle events (onPause, onDispose). Room writes are not free.

### 2. Downloading in ViewModel scope
**Bad:** Launching download coroutines in ViewModel (dies when user navigates away)
**Good:** Use WorkManager (survives process death, handles retries, respects battery/network constraints).

### 3. Storing downloaded files in external storage
**Bad:** Requesting WRITE_EXTERNAL_STORAGE, saving to Downloads folder
**Good:** Use `context.filesDir` (no permission needed, cleaned up on uninstall, not visible to file managers which is appropriate for cached streaming content).

### 4. Separate database migrations per feature
**Bad:** Migration 1->2 for downloads, 2->3 for history, 3->4 for search
**Good:** Single migration 1->2 adding all tables. Fewer migration paths to test, no ordering dependency between features.

### 5. Using DataStore for structured lists
**Bad:** Serializing search history as JSON in DataStore
**Good:** Use Room. DataStore is for key-value preferences, not queryable collections.

---

## Sources

- Direct codebase analysis of all 67 Kotlin files in OmniStream project (HIGH confidence)
- Architecture patterns from existing code conventions (MVVM, Hilt, StateFlow, Room)
- Media3 ExoPlayer offline download capabilities based on existing Media3 usage in `VideoPlayer.kt` (HIGH confidence for API patterns)
- WorkManager integration patterns based on Android Jetpack conventions (HIGH confidence)
