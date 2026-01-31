# Roadmap: OmniStream v2.0

## Overview

OmniStream v2.0 fixes existing bugs, adds offline downloads, upgrades the player and search experience, and introduces episode notifications -- all for sideload distribution (no Play Store constraints). The build order prioritizes the database foundation first (consumed by every subsequent feature), then the complex download system, then incremental search and player improvements.

**Distribution:** Sideload only (APK/GitHub). Play Store deferred per research recommendation (Path B).

**Phases:** 8-12 (continuing from v1.0 phases 1-7)
**Depth:** Standard
**Requirements:** 19 across 5 categories

---

## Phase 8: Foundation, Bug Fixes, and Progress Tracking

**Goal:** Users have a stable, bug-free base app that remembers where they left off in any content.

**Dependencies:** None (first phase of v2.0)

**Requirements:** BUG-01, BUG-02, BUG-03, PLAYER-01

**Plans:** 4 plans

Plans:
- [ ] 08-01-PLAN.md -- Room migration v1->v2 + WatchHistory entities/DAOs/Repository + Hilt wiring
- [ ] 08-02-PLAN.md -- Fix Library to read from Room (BUG-01) + Fix search race condition (BUG-02)
- [ ] 08-03-PLAN.md -- Manga reading progress auto-save (BUG-03) + Video progress save/resume
- [ ] 08-04-PLAN.md -- Continue Watching/Reading rows on Home screen (PLAYER-01)

**Success Criteria:**
1. User opens Library screen and sees locally favorited items from Room database (not just cloud sync)
2. User types rapidly in search bar and results load cleanly without duplicates or stale data
3. User exits a manga chapter mid-read, reopens it later, and resumes from the exact page
4. User sees a "Continue Watching/Reading" row on Home screen showing their most recent content with progress indicators

**Research Flags:** None -- standard Room + MVVM patterns.

**Scope Notes:**
- Room migration v1 to v2 adding `watch_history`, `search_history`, and `downloads` tables (all three in one migration even though downloads are used in Phase 9)
- `WatchHistoryEntity`, `WatchHistoryDao`, `WatchHistoryRepository`
- Flow `debounce(400).distinctUntilChanged().flatMapLatest {}` pipeline for search
- Progress save every 10-15s + on exit (not every frame)

---

## Phase 9: Download System

**Goal:** Users can download any content for offline access with full queue management.

**Dependencies:** Phase 8 (requires Room migration with `downloads` table and repository patterns)

**Requirements:** DL-01, DL-02, DL-03, DL-04

**Success Criteria:**
1. User taps download on a single episode or manga chapter and it downloads with visible progress
2. User selects multiple episodes/chapters and batch downloads them all at once
3. User opens the Downloads screen and can pause, resume, cancel, or delete any download in the queue
4. User enables airplane mode and can play downloaded videos or read downloaded manga chapters without internet

**Research Flags:** Media3 `DownloadService` lifecycle, HLS segment download behavior, Android 15 foreground service timeout handling.

**Scope Notes:**
- Separate `MangaDownloadWorker` (OkHttp parallel page downloads) and `VideoDownloadWorker` (Media3 `DownloadManager`)
- `enqueueUniqueWork()` to prevent duplicate downloads
- WorkManager `CoroutineWorker` with `setForeground()` (not raw foreground services)
- App-internal storage (no SAF complexity)
- Wi-Fi only toggle via WorkManager network constraints

---

## Phase 10: Search Improvements

**Goal:** Users have a polished, reliable search experience with history and filtering.

**Dependencies:** Phase 8 (requires `search_history` table from Room migration)

**Requirements:** SEARCH-01, SEARCH-02, SEARCH-03

**Success Criteria:**
1. User taps the search bar and sees their recent search terms as suggestions, with ability to clear individual items or all history
2. User searches when a source is slow or down and sees a graceful timeout message instead of a hang or crash
3. User can filter search results by source type (manga/anime/movies), genre, or release year

**Research Flags:** None -- standard patterns, debounce already handled in Phase 8.

**Scope Notes:**
- `SearchHistoryEntity`, `SearchHistoryDao` integrated into `SearchViewModel`
- Per-source timeout with `withTimeoutOrNull()` so partial results still display
- Filter UI on search results screen (chips or bottom sheet)

---

## Phase 11: Player Core Upgrades

**Goal:** Users have a full-featured video player with subtitles, picture-in-picture, and quality control.

**Dependencies:** Phase 8 (player infrastructure), Phase 9 (offline playback paths)

**Requirements:** PLAYER-02, PLAYER-03, PLAYER-04

**Success Criteria:**
1. User loads an SRT or VTT subtitle file (embedded or external) and sees synchronized captions during playback
2. User navigates away from the player and video continues in a floating PiP window with play/pause controls
3. User taps a quality selector during playback and switches between available resolutions (e.g., 720p, 1080p) without restarting the video

**Research Flags:** PiP on Android 16 (predictive back gesture conflict). Quality selection may interact with Vidzee/Cloudnestra decryption chains -- verify during planning.

**Scope Notes:**
- `MediaItem.SubtitleConfiguration` for subtitle loading (NOT deprecated `MergingMediaSource`)
- `PictureInPictureParams` with `setAutoEnterEnabled(true)` for Android 12+
- `enableOnBackInvokedCallback="false"` for player activity (Android 16 fix)
- `DefaultTrackSelector` with `TrackSelectionParameters.setMaxVideoSize()` for quality
- Persist quality preference in DataStore

---

## Phase 12: Player Polish and Notifications

**Goal:** Users have gesture controls, playback customization, skip functionality, and episode update notifications.

**Dependencies:** Phase 11 (player core must be stable), Phase 8 (watch history for subscription tracking)

**Requirements:** PLAYER-05, PLAYER-06, PLAYER-07, PLAYER-08, NOTIF-01

**Success Criteria:**
1. User swipes horizontally to seek and vertically to adjust volume (right side) or brightness (left side) during video playback
2. User switches between Fit, Stretch, and Zoom display modes to match their screen preference
3. User adjusts playback speed from 0.5x to 2x and the change takes effect immediately
4. User watches a show with AniSkip data and intro/outro segments are automatically skipped (or skip button appears)
5. User subscribes to a show and receives a notification when a new episode becomes available

**Research Flags:** AniSkip API integration details. Notification scheduling strategy (periodic WorkManager check vs. push).

**Scope Notes:**
- Gesture overlay with `pointerInput` modifiers in Compose
- `player.setVideoScalingMode()` or aspect ratio calculation for display modes
- `player.setPlaybackSpeed()` with speed selector UI
- AniSkip API (`api.aniskip.com`) for skip timestamps per episode
- WorkManager periodic task to check subscribed shows for new episodes
- Notification channel with proper importance level

---

## Progress

| Phase | Name | Requirements | Status |
|-------|------|--------------|--------|
| 8 | Foundation + Bug Fixes + Progress | BUG-01, BUG-02, BUG-03, PLAYER-01 | ✓ Complete |
| 9 | Download System | DL-01, DL-02, DL-03, DL-04 | Not Started |
| 10 | Search Improvements | SEARCH-01, SEARCH-02, SEARCH-03 | Not Started |
| 11 | Player Core Upgrades | PLAYER-02, PLAYER-03, PLAYER-04 | Not Started |
| 12 | Player Polish + Notifications | PLAYER-05, PLAYER-06, PLAYER-07, PLAYER-08, NOTIF-01 | Not Started |

## Coverage

**Total requirements:** 19
**Mapped:** 19/19

| Category | Requirements | Phase(s) |
|----------|-------------|----------|
| Bug Fixes | BUG-01, BUG-02, BUG-03 | 8 |
| Downloads | DL-01, DL-02, DL-03, DL-04 | 9 |
| Search | SEARCH-01, SEARCH-02, SEARCH-03 | 10 |
| Player | PLAYER-01 | 8 |
| Player | PLAYER-02, PLAYER-03, PLAYER-04 | 11 |
| Player | PLAYER-05, PLAYER-06, PLAYER-07, PLAYER-08 | 12 |
| Notifications | NOTIF-01 | 12 |

No orphaned requirements. No duplicates.

---
*Created: 2026-01-29*
*Milestone: v2.0*
