# Phase 8: Foundation, Bug Fixes, and Progress Tracking - Research

**Researched:** 2026-01-30
**Domain:** Room database migration, Flow-based search debounce, reading/video progress persistence, Jetpack Compose UI
**Confidence:** HIGH

## Summary

This phase requires four interconnected changes to an existing Kotlin/Compose/Room/Hilt Android app: (1) Room database migration from v1 to v2 adding watch_history, search_history, and downloads tables, (2) switching Library screen from cloud-only SyncRepository to local Room FavoriteDao, (3) replacing the manual Job-based search debounce with a proper Flow pipeline, and (4) building a watch/read history system with Continue Watching/Reading rows on the Home screen.

The codebase already has Room, Hilt, StateFlow, Coroutines, and Media3 ExoPlayer in place. No new dependencies are needed. The work is primarily wiring new Room entities/DAOs, creating a WatchHistoryRepository, modifying existing ViewModels, and adding new Compose UI components.

**Primary recommendation:** Start with the Room migration (foundation), then fix the two bugs (Library + Search), then build the progress tracking system (WatchHistory entity/DAO/repo + reader/player save hooks + Home screen Continue rows).

## Standard Stack

### Core (Already in Project)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Room | 2.6.1 | Local database | Already in use for favorites; migration API is well-established |
| Kotlin Coroutines | 1.8.0 | Async operations | Already in use; Flow operators (debounce, flatMapLatest) are standard |
| Hilt | 2.50 | Dependency injection | Already wiring all ViewModels and repositories |
| Jetpack Compose | BOM 2024.02 | UI | Already in use for all screens |
| Media3 ExoPlayer | 1.2.1 | Video playback | Already in use; has position tracking built in |
| StateFlow | (coroutines) | Reactive state | Already the pattern for all ViewModels |

### Supporting (Already in Project)
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Coil | 2.5.0 | Image loading | Already used for thumbnails in Home/Library screens |
| Navigation Compose | 2.7.7 | Screen navigation | Already handles all routes including player/reader |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Room for history | DataStore | DataStore is for key-value; Room is correct for structured history with queries |
| Flow debounce | Manual Job cancel | Manual approach (current) has race conditions; Flow pipeline is idiomatic and correct |
| Periodic save timer | Save every frame | Every frame wastes I/O; 10-15s interval is standard for reading progress |

**Installation:** No new dependencies needed. Zero additions to build.gradle.kts.

## Architecture Patterns

### Current Project Structure (relevant parts)
```
data/
├── local/
│   ├── AppDatabase.kt          # Room DB, currently v1 with FavoriteEntity only
│   ├── FavoriteEntity.kt       # @Entity for favorites table
│   ├── FavoriteDao.kt          # DAO with Flow queries
│   └── UserPreferences.kt     # DataStore preferences
├── remote/
│   └── dto/SyncDtos.kt        # Cloud sync DTOs
└── repository/
    ├── SyncRepository.kt       # Cloud-only sync (current Library data source)
    └── AuthRepository.kt

ui/
├── home/
│   ├── HomeViewModel.kt       # Needs: inject WatchHistoryRepository, add continue rows
│   └── HomeScreen.kt          # Needs: render Continue Watching/Reading rows at top
├── library/
│   ├── LibraryViewModel.kt    # BUG: reads from SyncRepository (cloud), should read Room
│   └── LibraryScreen.kt       # Needs: use FavoriteEntity instead of LibraryEntryDto
├── search/
│   ├── SearchViewModel.kt     # BUG: manual Job debounce, 300ms, race-prone
│   └── SearchScreen.kt
├── reader/
│   ├── ReaderViewModel.kt     # Needs: auto-save reading progress every 10-15s
│   └── ReaderScreen.kt        # Has LazyColumn with listState tracking currentPage
└── player/
    ├── PlayerViewModel.kt     # Needs: save video position on pause/exit
    └── components/
        └── VideoPlayer.kt     # Has ExoPlayer with position tracking loop already
```

### Pattern 1: Room Migration v1 to v2
**What:** Add three new tables (watch_history, search_history, downloads) in a single migration.
**When to use:** Anytime the database schema changes after the app has been released.
**Example:**
```kotlin
// Source: Room migration documentation (standard pattern)
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS watch_history (
                id TEXT NOT NULL PRIMARY KEY,
                content_id TEXT NOT NULL,
                source_id TEXT NOT NULL,
                content_type TEXT NOT NULL,
                title TEXT NOT NULL,
                cover_url TEXT,
                episode_id TEXT,
                chapter_id TEXT,
                progress_position REAL NOT NULL DEFAULT 0,
                total_duration REAL NOT NULL DEFAULT 0,
                progress_percentage REAL NOT NULL DEFAULT 0,
                page_index INTEGER NOT NULL DEFAULT 0,
                total_pages INTEGER NOT NULL DEFAULT 0,
                last_watched_at INTEGER NOT NULL DEFAULT 0,
                is_completed INTEGER NOT NULL DEFAULT 0
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS search_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                query TEXT NOT NULL,
                searched_at INTEGER NOT NULL DEFAULT 0
            )
        """)
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS downloads (
                id TEXT NOT NULL PRIMARY KEY,
                content_id TEXT NOT NULL,
                source_id TEXT NOT NULL,
                content_type TEXT NOT NULL,
                title TEXT NOT NULL,
                cover_url TEXT,
                episode_id TEXT,
                chapter_id TEXT,
                file_path TEXT NOT NULL,
                file_size INTEGER NOT NULL DEFAULT 0,
                status TEXT NOT NULL DEFAULT 'pending',
                progress REAL NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL DEFAULT 0
            )
        """)
    }
}
```

### Pattern 2: Flow Debounce Search Pipeline
**What:** Replace manual Job cancellation with `debounce().distinctUntilChanged().flatMapLatest{}`.
**When to use:** Any text-input-to-search scenario to prevent race conditions.
**Example:**
```kotlin
// Standard Flow debounce pipeline for search
private val _searchQuery = MutableStateFlow("")

init {
    viewModelScope.launch {
        _searchQuery
            .debounce(400)
            .distinctUntilChanged()
            .flatMapLatest { query ->
                if (query.isBlank()) {
                    flowOf(SearchUiState())
                } else {
                    flow {
                        emit(_uiState.value.copy(isLoading = true, error = null, query = query))
                        val results = performSearch(query)
                        emit(_uiState.value.copy(isLoading = false, results = results))
                    }
                }
            }
            .collect { state -> _uiState.value = state }
    }
}

fun onQueryChanged(query: String) {
    _searchQuery.value = query
}
```

### Pattern 3: Periodic Auto-Save with LaunchedEffect
**What:** Save reading progress every 10-15 seconds while reading.
**When to use:** Any periodic background save operation tied to a composable lifecycle.
**Example:**
```kotlin
// In ReaderViewModel
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
    watchHistoryRepository.upsertProgress(
        WatchHistoryEntity(
            id = "${sourceId}:${mangaId}:${currentChapterId}",
            contentId = mangaId,
            sourceId = sourceId,
            contentType = "manga",
            title = state.mangaTitle,
            chapterId = currentChapterId,
            pageIndex = state.currentPage,
            totalPages = state.pages.size,
            progressPercentage = (state.currentPage + 1).toFloat() / state.pages.size,
            lastWatchedAt = System.currentTimeMillis()
        )
    )
}
```

### Pattern 4: Dual Write (Room + Cloud Fire-and-Forget)
**What:** Save to Room first (synchronous), then fire-and-forget cloud sync.
**When to use:** Favorites that need to work offline.
**Example:**
```kotlin
suspend fun addFavorite(entity: FavoriteEntity) {
    // Local save (source of truth)
    favoriteDao.addFavorite(entity)
    // Cloud sync (fire-and-forget, don't block on failure)
    try {
        syncRepository.pushSyncData(/* ... */)
    } catch (e: Exception) {
        Log.w("FavRepo", "Cloud sync failed, will retry later", e)
    }
}
```

### Anti-Patterns to Avoid
- **Reading from cloud for Library display:** The current LibraryViewModel reads from SyncRepository (cloud API). This fails offline. Read from Room FavoriteDao instead.
- **Manual Job.cancel() for debounce:** The current SearchViewModel uses `searchJob?.cancel()` + `delay(300)`. This is error-prone. Use Flow debounce operator instead.
- **Saving video position every frame:** The VideoPlayer already has a 500ms position update loop. Don't save to Room every 500ms. Save on pause and on exit only.
- **Using `@PrimaryKey(autoGenerate = true)` for watch_history:** Use a composite key like `sourceId:contentId:episodeId` to enable upsert (INSERT OR REPLACE) for progress updates.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Search debounce | Manual Job cancel + delay | `Flow.debounce(400).distinctUntilChanged().flatMapLatest{}` | Flow operators handle cancellation, backpressure, and edge cases automatically |
| Database migration | Drop and recreate DB | Room `Migration(1, 2)` with raw SQL | Preserves user's existing favorites data |
| Progress percentage | Custom math per content type | Single `progressPercentage` Float field in WatchHistoryEntity | Unifies video (position/duration) and manga (page/totalPages) into one query for Continue rows |
| Upsert logic | Check exists + insert or update | `@Insert(onConflict = OnConflictStrategy.REPLACE)` | Room handles this atomically |
| Periodic save timer | Handler/TimerTask | `viewModelScope.launch { while(isActive) { delay(); save() } }` | Coroutine cancellation is automatic when ViewModel is cleared |

**Key insight:** Every problem in this phase has a standard Kotlin/Android solution already in the project's dependency tree. No new libraries, no custom frameworks.

## Common Pitfalls

### Pitfall 1: Room Migration Forgetting Existing Data
**What goes wrong:** Using `fallbackToDestructiveMigration()` instead of a proper Migration object wipes the favorites table.
**Why it happens:** It's the easy path when schema changes.
**How to avoid:** Always provide a `Migration(1, 2)` object to `Room.databaseBuilder().addMigrations()`. Test migration with existing data.
**Warning signs:** Users lose their favorites after app update.

### Pitfall 2: Search Flow Not Emitting Loading State
**What goes wrong:** The `flatMapLatest` cancels the previous flow, but if loading state is emitted inside the flow, the cancellation can eat the loading=false emission.
**Why it happens:** `flatMapLatest` cancels the inner flow when a new value arrives.
**How to avoid:** Emit loading state before the flow, and always handle the blank-query case to reset state immediately.
**Warning signs:** Spinner stays visible forever after rapid typing.

### Pitfall 3: WatchHistory Primary Key Collision
**What goes wrong:** Using just `contentId` as primary key means switching episodes/chapters overwrites the previous entry.
**Why it happens:** Video and manga have different granularity (episode vs chapter).
**How to avoid:** Use composite key: `sourceId:contentId` for the Continue row (most recent per content), but store `episodeId`/`chapterId` as fields for resume point. The Continue row queries by `contentId` and gets the latest entry.
**Warning signs:** Progress shows wrong episode or chapter.

### Pitfall 4: ExoPlayer Position Not Available on Exit
**What goes wrong:** `exoPlayer.currentPosition` returns 0 after `release()` is called.
**Why it happens:** DisposableEffect's `onDispose` may run after player is released.
**How to avoid:** Save position BEFORE releasing the player. Use `onCleared()` in ViewModel or a `DisposableEffect` that saves before disposal.
**Warning signs:** Video progress always shows 0%.

### Pitfall 5: Library Screen Still Using LibraryEntryDto
**What goes wrong:** LibraryScreen currently renders `LibraryEntryDto` (cloud DTO). Switching to Room requires mapping `FavoriteEntity` to the UI model.
**Why it happens:** The screen was built for cloud data shape.
**How to avoid:** Either adapt the screen to use FavoriteEntity directly, or create a mapping function. FavoriteEntity has fewer fields (no `progress`, `unreadCount`, `lastChapter`), so the Library grid card will simplify.
**Warning signs:** Compiler errors when switching data source.

### Pitfall 6: LazyColumn Scroll State Not Preserved for Reader Resume
**What goes wrong:** `rememberLazyListState()` resets when navigating away and back.
**Why it happens:** Compose state is lost when the composable leaves composition.
**How to avoid:** On resume, read saved page from WatchHistory and scroll to it via `listState.scrollToItem(savedPageIndex)` in a LaunchedEffect.
**Warning signs:** Reader always starts at page 1 despite saved progress.

## Code Examples

### WatchHistoryEntity
```kotlin
@Entity(tableName = "watch_history")
data class WatchHistoryEntity(
    @PrimaryKey
    val id: String,                    // "sourceId:contentId" for uniqueness per content
    val contentId: String,
    val sourceId: String,
    val contentType: String,           // "video" or "manga"
    val title: String,
    val coverUrl: String? = null,
    @ColumnInfo(name = "episode_id")
    val episodeId: String? = null,     // For video: which episode
    @ColumnInfo(name = "chapter_id")
    val chapterId: String? = null,     // For manga: which chapter
    @ColumnInfo(name = "progress_position")
    val progressPosition: Long = 0L,   // Video: milliseconds, Manga: page index
    @ColumnInfo(name = "total_duration")
    val totalDuration: Long = 0L,      // Video: total ms, Manga: total pages
    @ColumnInfo(name = "progress_percentage")
    val progressPercentage: Float = 0f, // 0.0 to 1.0
    @ColumnInfo(name = "last_watched_at")
    val lastWatchedAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false
)
```

### WatchHistoryDao
```kotlin
@Dao
interface WatchHistoryDao {
    @Query("""
        SELECT * FROM watch_history
        WHERE content_type = :contentType AND is_completed = 0
        ORDER BY last_watched_at DESC
        LIMIT :limit
    """)
    fun getContinueItems(contentType: String, limit: Int = 10): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history WHERE content_id = :contentId AND source_id = :sourceId")
    suspend fun getProgress(contentId: String, sourceId: String): WatchHistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: WatchHistoryEntity)

    @Query("DELETE FROM watch_history WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearAll()
}
```

### SearchViewModel Fix (Flow Pipeline)
```kotlin
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val sourceManager: SourceManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _searchQuery
                .debounce(400)
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    if (query.isBlank()) {
                        flowOf(SearchUiState())
                    } else {
                        flow {
                            emit(_uiState.value.copy(isLoading = true, error = null, query = query))
                            try {
                                val results = performSearch(query)
                                emit(SearchUiState(
                                    isLoading = false,
                                    results = results,
                                    query = query,
                                    error = if (results.isEmpty()) "No results found" else null
                                ))
                            } catch (e: Exception) {
                                emit(_uiState.value.copy(
                                    isLoading = false,
                                    error = e.message ?: "Search failed"
                                ))
                            }
                        }
                    }
                }
                .collect { state -> _uiState.value = state }
        }
    }

    fun onQueryChanged(query: String) {
        _searchQuery.value = query
    }

    // performSearch() stays the same as current implementation
}
```

### LibraryViewModel Fix (Room Instead of Cloud)
```kotlin
@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val favoriteDao: FavoriteDao,
    private val syncRepository: SyncRepository // Keep for dual write
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState

    init {
        // Observe Room favorites reactively
        viewModelScope.launch {
            favoriteDao.getAllFavorites().collect { favorites ->
                _uiState.value = LibraryUiState(
                    isLoading = false,
                    favorites = favorites
                )
            }
        }
    }
}
```

### HomeViewModel Continue Rows
```kotlin
// In HomeViewModel, add WatchHistoryRepository injection
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val sourceManager: SourceManager,
    private val watchHistoryRepository: WatchHistoryRepository
) : ViewModel() {
    // Add to HomeUiState:
    // val continueWatching: List<WatchHistoryEntity> = emptyList()
    // val continueReading: List<WatchHistoryEntity> = emptyList()

    init {
        // Collect continue watching/reading as Flows
        viewModelScope.launch {
            watchHistoryRepository.getContinueWatching().collect { items ->
                _uiState.value = _uiState.value.copy(continueWatching = items)
            }
        }
        viewModelScope.launch {
            watchHistoryRepository.getContinueReading().collect { items ->
                _uiState.value = _uiState.value.copy(continueReading = items)
            }
        }
        loadHomeContent()
    }
}
```

### Continue Row Compose Component (Video Progress Bar)
```kotlin
@Composable
fun ContinueWatchingCard(
    item: WatchHistoryEntity,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(160.dp)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress
            ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box {
            AsyncImage(
                model = item.coverUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            )
            // Progress bar at bottom of thumbnail
            LinearProgressIndicator(
                progress = { item.progressPercentage },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .align(Alignment.BottomCenter),
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.Black.copy(alpha = 0.5f)
            )
        }
        Text(
            text = item.title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(8.dp),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
```

### Video Position Save on Pause/Exit
```kotlin
// In PlayerViewModel or PlayerScreen DisposableEffect
fun saveVideoProgress(
    exoPlayer: ExoPlayer,
    watchHistoryRepository: WatchHistoryRepository,
    sourceId: String,
    videoId: String,
    episodeId: String,
    title: String,
    coverUrl: String?
) {
    val position = exoPlayer.currentPosition
    val duration = exoPlayer.duration.takeIf { it != C.TIME_UNSET } ?: 0L
    if (duration <= 0) return

    val percentage = position.toFloat() / duration
    val isCompleted = percentage > 0.90f // 90% = completed

    viewModelScope.launch {
        watchHistoryRepository.upsert(
            WatchHistoryEntity(
                id = "$sourceId:$videoId",
                contentId = videoId,
                sourceId = sourceId,
                contentType = "video",
                title = title,
                coverUrl = coverUrl,
                episodeId = episodeId,
                progressPosition = position,
                totalDuration = duration,
                progressPercentage = percentage,
                lastWatchedAt = System.currentTimeMillis(),
                isCompleted = isCompleted
            )
        )
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| Manual Job cancel for debounce | `Flow.debounce().flatMapLatest{}` | Kotlin Coroutines 1.6+ | Eliminates race conditions in search |
| Cloud-only library | Local Room DB + cloud sync | This phase | Offline access, speed improvement |
| No progress tracking | WatchHistoryEntity in Room | This phase | Continue Watching/Reading feature |

**No deprecated APIs involved.** All Room, Flow, and Compose APIs used are current and stable.

## Open Questions

1. **WatchHistory ID granularity for video**
   - What we know: Video has episodes, manga has chapters. The Continue row should show the content (not each episode).
   - What's unclear: Should we track per-episode progress or only per-content? If a user watches Episode 3, should the Continue row resume to Episode 3?
   - Recommendation: Use `sourceId:contentId` as the primary key (one entry per content). Store `episodeId`/`chapterId` as fields. When user taps Continue, navigate to that specific episode/chapter. This keeps the Continue row clean (one entry per show/manga).

2. **Manga progress percentage calculation**
   - What we know: Context says "percentage text overlay showing chapters read of total"
   - What's unclear: How to know total chapter count when the reader only loads one chapter at a time
   - Recommendation: ReaderViewModel already loads `chapterList` for navigation. Store `currentChapterIndex / chapterList.size` as the overall progress. The per-page progress is for resume; the chapter-level progress is for the Continue row.

3. **Video resume prompt UI**
   - What we know: "Resume shows prompt: Resuming from X:XX with option to start over"
   - What's unclear: Exact UI for the prompt
   - Recommendation: Use an AlertDialog or Snackbar with "Resume from X:XX" and "Start Over" buttons, shown when PlayerScreen loads and finds existing progress.

## Sources

### Primary (HIGH confidence)
- Codebase analysis: All files in `app/src/main/java/com/omnistream/` read directly
- Room migration pattern: Standard Android documentation, well-established since Room 1.0
- Flow debounce/flatMapLatest: Standard Kotlin Coroutines Flow API, stable since kotlinx-coroutines 1.6

### Secondary (MEDIUM confidence)
- ExoPlayer position tracking: Based on codebase's existing `VideoPlayer.kt` which already tracks `currentPosition` every 500ms
- Continue Watching UI pattern: Based on Netflix/YouTube standard patterns described in CONTEXT.md decisions

### Tertiary (LOW confidence)
- None. All patterns are verified against the existing codebase and standard Android/Kotlin APIs.

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All dependencies already in build.gradle.kts, no changes needed
- Architecture: HIGH - Patterns follow existing codebase conventions (Room Entity/DAO/Repository, Hilt injection, StateFlow ViewModels)
- Pitfalls: HIGH - Identified from direct codebase analysis of current bugs and missing features
- Code examples: HIGH - Based on existing codebase patterns and standard Room/Flow APIs

**Research date:** 2026-01-30
**Valid until:** 2026-03-30 (stable APIs, no fast-moving dependencies)
