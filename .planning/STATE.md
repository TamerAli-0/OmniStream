# State: OmniStream

## Project Reference

See: .planning/PROJECT.md (updated 2026-01-29)

**Core value:** Users can discover, stream, and read content from multiple sources in one app with a seamless, crash-free experience.
**Current focus:** v2.0 -- Bug fixes, downloads, player upgrades, search improvements, notifications (sideload distribution)

## Current Position

Phase: 8 -- Foundation, Bug Fixes, and Progress Tracking
Plan: 04 of 4 (Continue Watching/Reading Rows)
Status: Phase complete
Progress: [##########] 4/4 plans in phase 8
Last activity: 2026-01-31 -- Completed 08-04-PLAN.md (Continue Watching/Reading rows on Home screen)

## Performance Metrics

Plans completed: 4
Requirements delivered: 4/19
Phases completed: 1/5

## Accumulated Context

### Decisions
- WebView approach for AnimeKai (encryption too complex)
- Vidzee AES decryption cracked and implemented in CryptoUtils.kt
- WatchFlix uses simple 3-layer HTML parsing (no encryption)
- TMDB keys moved to BuildConfig for security
- Room database chosen for favorites (replaces cloud-only approach)
- Distribution: Sideload only for v2.0 (Play Store incompatible with scraping architecture)
- Single Room migration v1->v2 adds all three new tables (downloads, watch_history, search_history)
- Zero new production dependencies needed for v2.0 features
- Library shows flat favorites list (no categories -- FavoriteEntity has no category field)
- Library Flow auto-updates from Room (no manual Refresh needed)
- Search debounce 400ms with Flow pipeline (up from 300ms manual Job approach)
- performSearch uses coroutineScope{} for structured concurrency in flatMapLatest
- WatchHistoryEntity uses "sourceId:contentId" composite string PK (matches FavoriteEntity pattern)
- Unified watch_history table for video and manga (contentType discriminator, not separate tables)
- Continue rows placed at top of Home LazyColumn, before all source sections
- Video continue cards: 16:9 aspect ratio with 3dp LinearProgressIndicator at thumbnail bottom
- Manga continue cards: 0.7 portrait aspect ratio with percentage badge overlay
- Long-press dropdown menu pattern: combinedClickable + DropdownMenu for context actions

### Known Issues
- ~~Library screen only shows cloud sync data, not local Room favorites (BUG-01, Phase 8)~~ FIXED 08-02
- ~~No reading progress persistence (BUG-03, Phase 8)~~ ADDRESSED 08-03/08-04
- GogoAnime details page crashes (deferred to Future, not in v2.0 scope)
- ~~Search race condition with rapid typing (BUG-02, Phase 8)~~ FIXED 08-02
- No search timeout handling (SEARCH-02, Phase 10)
- Downloads screen is placeholder (DL-01 through DL-04, Phase 9)

### Technical Notes
- Backend: https://omnistream-api-q2rh.onrender.com (Render free tier)
- MongoDB Atlas: cluster0.gyrtw6w.mongodb.net
- Test device: Infinix Note 30
- Codebase: 67 Kotlin files, MVVM + Clean Architecture, Hilt DI
- Room DB v2 with MIGRATION_1_2: adds watch_history, search_history, downloads tables
- Media3 ExoPlayer already has subtitle selection, speed controls, resize, PiP button wired up
- Player features partially implemented -- Phase 11/12 complete what exists
- Pattern: Room Flow collection in ViewModel init for reactive UI
- Pattern: MutableStateFlow + debounce + distinctUntilChanged + flatMapLatest for search
- Pattern: combinedClickable + DropdownMenu for long-press context menus
- WatchHistoryRepository injectable via Hilt, wraps WatchHistoryDao
- All new DAOs (WatchHistory, SearchHistory, Download) provided by Hilt via AppModule
- HomeViewModel injects WatchHistoryRepository, collects continueWatching/continueReading Flows
- HomeScreen has ContinueWatchingRow/ContinueReadingRow composables with progress indicators

### Todos
- Execute Phase 9 (Downloads)

### Blockers
- None

## Session Continuity

Last session ended: 2026-01-31
Next step: Execute Phase 9 (Downloads) or Phase 10 (Search improvements)
Key context for next session: Phase 8 complete. Room v2 migration, bug fixes, progress tracking, and continue rows all done. HomeViewModel has WatchHistoryRepository injected. HomeScreen shows continue watching/reading rows at top. Ready for next phase.

---
*Last updated: 2026-01-31*
