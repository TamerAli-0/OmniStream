# Phase 8: Foundation, Bug Fixes, and Progress Tracking - Context

**Gathered:** 2026-01-30
**Status:** Ready for planning

<domain>
## Phase Boundary

Stable database foundation (Room migration v1 to v2 adding watch_history, search_history, and downloads tables), fix three bugs (library favorites, search race condition, reading progress persistence), and add Continue Watching/Reading rows to the Home screen. No new capabilities beyond what's listed in requirements BUG-01, BUG-02, BUG-03, PLAYER-01.

</domain>

<decisions>
## Implementation Decisions

### Continue Watching/Reading rows
- Two separate rows: "Continue Watching" for video, "Continue Reading" for manga/manhwa
- Rows appear at the top of the Home screen, above all other content
- Each item shows: thumbnail, title, and progress indicator
- Video items: thin progress bar at bottom of thumbnail (Netflix/YouTube style)
- Manga/manhwa items: percentage text overlay showing chapters read of total (e.g., "30%")
- Maximum 10 items per row, scrollable horizontally
- Ordered by most recently watched/read first
- Completed content (100% watched/fully read) does not appear in Continue rows
- If no history exists, hide the rows entirely (no empty state prompt)
- Tap: opens content and plays/reads immediately
- Long press: dropdown menu with Delete, Continue, and Details options
- Delete removes progress history only (removes item from Continue row), does not affect favorites

### Library screen (BUG-01)
- Source of truth: Local Room DB for the Library screen display
- Favorites save to both Room DB (local) and cloud backend (dual write)
- If cloud save fails (offline), favorite still saves locally -- cloud syncs when connectivity returns (fire-and-forget)
- Fresh start: no migration of existing cloud favorites into Room DB
- Library screen reads from Room DB for speed and offline access

### Reading progress persistence (BUG-03)
- Auto-save manga/manhwa reading position periodically (every 10-15 seconds) while reading
- Saves exact page/scroll position within a chapter
- Resume opens at the exact page where user left off
- Percentage on Continue Reading row = chapters read out of total chapters

### Video progress persistence
- Save video position on pause and on exit (not periodic auto-save)
- Resume shows a prompt: "Resuming from X:XX" with option to start over
- Progress bar on Continue Watching row shows % of episode/movie watched

### Search race condition (BUG-02)
- Flow debounce(400).distinctUntilChanged().flatMapLatest{} pipeline
- Visual feedback during loading: Claude's discretion

### Claude's Discretion
- Search loading/feedback UX during debounce wait
- Whether detail pages show progress indicators on individual episodes/chapters
- Loading skeleton/shimmer design for Continue rows
- Exact progress bar colors and styling
- Error state handling

</decisions>

<specifics>
## Specific Ideas

- Continue rows should feel like Netflix's "Continue Watching" row -- thumbnail-forward, minimal text
- Long press dropdown inspired by streaming app patterns (Netflix, Disney+)
- Progress bar for video at bottom edge of thumbnail, not as a separate element

</specifics>

<deferred>
## Deferred Ideas

- Cloud sync for watch/read progress to website -- future phase
- Migration of existing cloud favorites to Room DB -- could be added when building full sync
- Cloud-to-local favorite sync (bidirectional) -- future phase

</deferred>

---

*Phase: 08-foundation-bug-fixes-progress*
*Context gathered: 2026-01-30*
