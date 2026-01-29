# Project Research Summary

**Project:** OmniStream v2.0
**Domain:** Android manga reader + video streaming aggregator (Tachiyomi/CloudStream hybrid)
**Researched:** 2026-01-29
**Confidence:** HIGH (all four research tracks completed with verified sources)

## Research Summary

OmniStream v2.0 adds downloads, subtitles, PiP, quality selection, progress persistence, and search improvements to an existing Kotlin/Compose app with Media3 ExoPlayer, Room, Hilt, and WorkManager already in the stack. The single most important finding across all research is that **zero new production dependencies are required** -- every planned feature is achievable with APIs already present in the current dependency tree (Media3 1.9.1, Room, WorkManager, OkHttp, Kotlin Flow). The second most important finding is that **the app's current architecture is fundamentally incompatible with Google Play Store distribution** due to scraped copyrighted content, hidden decryption functionality, and a passcode gate that blocks reviewer access. This is not a polish issue -- it is a business model decision that must be resolved before any Play Store work begins. For all non-Play-Store concerns, the existing MVVM + Clean Architecture codebase is well-structured to absorb these features. The architecture research identified a clean build order based on dependency analysis: database schema first (single migration adding three tables), then watch history, then downloads, then player polish.

---

## Stack Additions

**Key finding: No new production dependencies.** All v2.0 features use APIs already bundled in the existing stack.

| Feature | Library | Status |
|---------|---------|--------|
| Video downloads (HLS/DASH) | `media3-exoplayer-hls`, `media3-exoplayer-dash` | Already in dependency tree -- use `DownloadService`, `DownloadManager`, `HlsDownloader` APIs |
| Manga chapter downloads | OkHttp (existing) | Download images to `context.filesDir`, track state in Room |
| Subtitle rendering (SRT/VTT/TTML) | Media3 ExoPlayer (built-in) | Use `MediaItem.SubtitleConfiguration`, NOT deprecated `MergingMediaSource` |
| Picture-in-Picture | Android platform API (8.0+) | `PictureInPictureParams`, `setAutoEnterEnabled(true)` for Android 12+ |
| Quality selection | Media3 `DefaultTrackSelector` | Constraint-based via `TrackSelectionParameters.setMaxVideoSize()` |
| Progress persistence | Room (existing) | New entities: `WatchHistoryEntity`, `DownloadEntity`, `SearchHistoryEntity` |
| Search debounce + history | Kotlin Flow `debounce()` + Room | `flatMapLatest` for cancellation; Room for history persistence |

**Only additions needed:**
- Bump Media3 to 1.9.1
- Add `media3-session` if not already present (for PiP remote actions)
- Add LeakCanary as `debugImplementation` only
- Configure R8/ProGuard rules for release builds

**Critical "do NOT use" list:** Do not use Android's `DownloadManager` for video (no HLS awareness). Do not use Coil's disk cache as download storage (LRU evicts). Do not use `MergingMediaSource` for subtitles (throws `IllegalStateException` in Media3 1.4.0+). Do not use RxJava (project is Flow-based). Do not use SAF for primary storage (slow, complex).

---

## Feature Priorities

### Table Stakes (must ship in v2.0)

| Priority | Feature | Complexity | Rationale |
|----------|---------|------------|-----------|
| 1 | Continue Watching/Reading | Low | Highest ROI -- no existing progress tracking. Touches Player, Reader, Home. Foundation for everything else. |
| 2 | Search Debounce + History | Low | Quick win upgrade to existing search. Flow `debounce` + Room history table. |
| 3 | Quality Selection | Medium | Users expect manual quality control. Source-level selection already works; add track-level for adaptive HLS. |
| 4 | Manga Chapter Downloads | High | Core offline feature. WorkManager + Room queue. Tachiyomi users expect this. |
| 5 | PiP (completion) | Low-Medium | Already partially implemented. Needs Activity lifecycle wiring, auto-enter, remote actions. |
| 6 | Video Episode Downloads | High | Builds on manga download infra. Larger files = more complexity (resume, chunked). |
| 7 | Subtitle Enhancements | Low | Already functionally complete. Add styling preferences and sync offset as polish. |

### Differentiators (competitive advantage)

- **Unified Continue Row** -- One home section mixing manga + video progress (no competing app does this)
- **Smart Download Queue** -- Single queue managing both manga and video downloads with priority
- **Adaptive Onboarding** -- Smoother first-run than Tachiyomi/CloudStream (critical for Play Store if pursued)

### Defer to v3.0+

- Full ASS/SSA subtitle rendering (requires libass JNI or libmpv -- multi-week rabbit hole)
- Cross-content recommendations (requires MAL/AniList ID matching)
- Predictive auto-downloads (usage pattern analysis)
- Online subtitle search (OpenSubtitles/SubDL API integration)

---

## Architecture Integration

### Current State

The codebase is clean MVVM with Hilt DI, 67 Kotlin files. Room DB v1 has only `FavoriteEntity` (no migrations yet). Player already has subtitle selection, speed controls, resize, and a PiP button wired up. DownloadsScreen is a placeholder. SearchViewModel has no persistence. No watch history or progress tracking exists anywhere.

### Key Integration Points and Build Order

**Single database migration (v1 -> v2):** Add all three new tables (`downloads`, `watch_history`, `search_history`) in one migration. This avoids chaining 1->2->3->4 and reduces test surface. All tables are independent (no foreign keys between them).

**New components by layer:**

| Layer | New Components |
|-------|---------------|
| data/local | `DownloadEntity`, `WatchHistoryEntity`, `SearchHistoryEntity` + DAOs |
| data/repository | `DownloadRepository`, `WatchHistoryRepository` |
| data/worker | `DownloadWorker` (WorkManager `CoroutineWorker`) |
| core | `DownloadNotificationManager` |
| ui/downloads | `DownloadsViewModel` (rewrite placeholder screen) |

**ViewModels gaining new dependencies:**

| ViewModel | Will Also Inject |
|-----------|-----------------|
| `PlayerViewModel` | `WatchHistoryRepository` |
| `ReaderViewModel` | `WatchHistoryRepository` |
| `HomeViewModel` | `WatchHistoryRepository` |
| `VideoDetailViewModel` | `WatchHistoryRepository`, `DownloadRepository` |
| `MangaDetailViewModel` | `WatchHistoryRepository`, `DownloadRepository` |
| `SearchViewModel` | `SearchHistoryDao` |

**What stays untouched:** `SourceManager`, `OmniNavigation` (downloads route already registered), all domain models, all source implementations.

---

## Critical Risks

### 1. Play Store Incompatibility (CRITICAL -- Decision Gate)

The app scrapes copyrighted content from unlicensed sites, uses AES decryption chains to bypass content protection, and has a passcode gate that blocks Play Store reviewers. These form an interlocking set of four policy violations (IP infringement, offline piracy enablement, paywall restriction, deceptive behavior) that **cannot be fixed with polish**. The project must choose a distribution path before investing in Play Store work:

- **Path A (Play Store):** Strip all scraped sources except MangaDex, remove decryption chains, remove/rework access gate. This is essentially a different app.
- **Path B (Sideload):** Distribute via GitHub/APK. Build all features freely. Skip Play Store entirely.
- **Path C (Dual-track):** Clean Play Store version + full sideload version. Higher maintenance cost.

**Recommendation:** Choose Path B for v2.0. Ship all features without Play Store constraints. Revisit Play Store as a separate project if/when a legal content strategy exists.

### 2. Foreground Service Misconfiguration on Android 14-15+ (MODERATE)

Android 14 requires declaring foreground service types in manifest AND Play Console. Android 15 introduced a 6-hour timeout for `dataSync` services. Prevention: use WorkManager with `CoroutineWorker` and `setForeground()`, not raw foreground services. Handle `onTimeout()`.

### 3. PiP Breaks on Android 16 with Predictive Back (MODERATE)

The new predictive back gesture in Android 16 preempts `enterPictureInPictureMode()` by finishing the activity first. Prevention: use `setAutoEnterEnabled(true)` instead of back-press-triggered PiP. Set `enableOnBackInvokedCallback="false"` for the player activity. Test on Android 16 emulator.

### 4. WorkManager Download Queue Duplication (MODERATE)

Without `enqueueUniqueWork()`, each tap creates a new work request. Prevention: use unique work names per content item, observe `WorkInfo` via Flow for progress, implement exponential backoff for retries.

### 5. Search Race Conditions Despite Debounce (MODERATE)

Multi-source search returns results out of order. Simple debounce does not fix this. Prevention: use `flatMapLatest` (auto-cancels previous collection), wrap OkHttp calls in `suspendCancellableCoroutine` with `invokeOnCancellation { call.cancel() }`.

---

## Recommended Build Order

### Phase 1: Database Foundation + Watch History

**Rationale:** Touches the most files, establishes the migration pattern, and is the highest-ROI feature (zero existing progress tracking). Every subsequent feature piggybacks on this migration.

**Delivers:** Continue Watching/Reading row on HomeScreen, progress persistence in Player and Reader, auto-complete detection at 95%.

**Scope:**
- Room migration 1->2 (all three tables: `downloads`, `watch_history`, `search_history`)
- `WatchHistoryEntity`, `WatchHistoryDao`, `WatchHistoryRepository`
- `PlayerViewModel` saves position every 10s + on exit
- `ReaderViewModel` saves chapter + page on exit
- HomeScreen "Continue" row (unified manga + video, sorted by recency)
- Progress bars on episode/chapter lists in detail screens

**Avoids:** Pitfall of saving progress on every frame (debounce to 10-15s). Pitfall of using DataStore for structured data (use Room).

**Research needed:** None -- standard Room + MVVM patterns.

### Phase 2: Download System

**Rationale:** Most complex feature. Requires the database layer from Phase 1. Separate manga and video workers (different download characteristics) but unified queue UI.

**Delivers:** Manga chapter downloads, video episode downloads, download queue screen, offline playback, pause/resume/retry.

**Scope:**
- `DownloadEntity`, `DownloadDao`, `DownloadRepository`
- `MangaDownloadWorker` (OkHttp, parallel page downloads)
- `VideoDownloadWorker` (Media3 `DownloadManager` for HLS/DASH)
- `DownloadNotificationManager` (foreground notification with progress)
- Rewrite `DownloadsScreen` placeholder
- Download buttons on `VideoDetailScreen` and `MangaDetailScreen`
- Offline playback in Player (local file URI) and Reader (local images)
- Wi-Fi only toggle via WorkManager network constraints

**Avoids:** Pitfalls 5 (foreground service misconfiguration), 6 (scoped storage), 9 (queue duplication). Use `enqueueUniqueWork()`. Use app-internal storage. Handle Android 15 service timeouts.

**Research needed:** YES -- Media3 `DownloadService` integration details, HLS segment download behavior, and interaction with existing `OmniHttpClient`.

### Phase 3: Search + Player Polish

**Rationale:** Lowest risk, lowest complexity. Search history is a small Room addition. PiP and quality selection are incremental improvements to already-working features.

**Delivers:** Search debounce, search history with clear/delete, PiP completion (auto-enter, remote actions, Android 16 compatibility), track-level quality selection for adaptive HLS, subtitle styling preferences + sync offset.

**Scope:**
- `SearchHistoryEntity`, `SearchHistoryDao` integration into `SearchViewModel`
- SearchScreen UI: recent searches when empty, clear/delete individual items
- Flow pipeline: `debounce(400).distinctUntilChanged().flatMapLatest {}`
- PiP: `onUserLeaveHint()` in MainActivity, `setAutoEnterEnabled(true)`, `RemoteAction` buttons, `sourceRectHint`
- Quality: `QualitySelectionSheet` listing tracks from `player.currentTracks`, persist preference in DataStore
- Subtitle: `CaptionStyleCompat` configuration, sync offset control

**Avoids:** Pitfalls 7 (Android 16 PiP), 8 (subtitle MIME mismatches), 10 (search race conditions), 12 (search history privacy).

**Research needed:** PiP on Android 16 should be tested early. Otherwise standard patterns.

### Phase 4: Play Store Readiness (CONDITIONAL)

**Rationale:** Only pursue if distribution strategy decision selects Path A or C. Otherwise skip entirely.

**Delivers:** R8/ProGuard configuration, release signing, App Bundle, baseline profiles, accessibility audit, onboarding flow, privacy policy, data safety section.

**Scope (if pursued):**
- R8 with ProGuard rules for OkHttp, Jsoup, Room, Media3, Coil
- Release signing config (keystore in `local.properties`, never committed)
- Access gate rework or removal for reviewer access
- Source stripping (remove all scraped sources except MangaDex)
- Onboarding wizard
- Accessibility: `contentDescription`, 48dp tap targets, contrast ratio 4.5:1
- Edge-to-edge display, predictive back gesture support

**Avoids:** Pitfalls 1-4 (the interlocking Play Store policy violations).

**Research needed:** YES -- Play Store review process specifics, App Access Declaration requirements, content policy edge cases for aggregator apps.

### Phase Ordering Rationale

1. **Watch history first** because it has zero dependencies, touches the most screens, and the database migration it creates is consumed by every other feature.
2. **Downloads second** because it is the most complex and benefits from the stable database layer. Manga and video workers are independent but share queue infrastructure.
3. **Search + polish third** because these are low-risk incremental improvements that can be developed in parallel.
4. **Play Store last (conditional)** because it requires a strategic decision that is independent of feature work, and because the feature set should be stable before pursuing certification.

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 2 (Downloads):** Media3 `DownloadService` lifecycle, HLS segment download behavior, Android 15 foreground service timeout handling, interaction with existing `OmniHttpClient`
- **Phase 4 (Play Store):** Policy compliance specifics, App Access Declaration, reviewer bypass patterns

Phases with standard patterns (skip research-phase):
- **Phase 1 (Watch History):** Standard Room + MVVM, well-documented
- **Phase 3 (Search + Polish):** All APIs are well-documented platform features

---

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | All recommendations verified against official Android/Media3 docs. Zero new deps is a strong finding. |
| Features | HIGH | Feature expectations verified against Tachiyomi, CloudStream, Kotatsu reference apps. Priority ordering is well-supported. |
| Architecture | HIGH | Based on direct analysis of all 67 Kotlin files in the codebase. Integration points are concrete, not speculative. |
| Pitfalls | HIGH | Critical pitfalls backed by Google Play policy docs and 2025 removal precedents. Technical pitfalls backed by official Android docs and open GitHub issues. |

**Overall confidence:** HIGH

### Gaps to Address

- **Distribution strategy:** The Play Store vs. sideload decision is a business call, not a technical one. Research can only flag the risk -- the team must decide.
- **ASS/SSA subtitles:** Deferred to v3.0 but anime users will notice. Need to communicate this limitation clearly.
- **Cross-device sync:** The existing Node.js backend has auth infrastructure but sync protocol is not researched. Defer to after v2.0 core features ship.
- **Decryption per quality level:** Quality selection may break existing Vidzee/Cloudnestra decryption chains. Needs code-level verification during Phase 3 planning.

---

## Sources

### Primary (HIGH confidence)
- [Media3 Downloads Guide](https://developer.android.com/media/media3/exoplayer/downloading-media)
- [Media3 Track Selection](https://developer.android.com/media/media3/exoplayer/track-selection)
- [Media3 Supported Formats](https://developer.android.com/media/media3/exoplayer/supported-formats)
- [Compose PiP Setup](https://developer.android.com/develop/ui/compose/system/pip-setup)
- [WorkManager Long-Running Workers](https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running)
- [Google Play Intellectual Property Policy](https://support.google.com/googleplay/android-developer/answer/9888072)
- [Google Play Deceptive Behavior Policy](https://support.google.com/googleplay/android-developer/answer/9888077)
- [Android 15 Foreground Service Changes](https://developer.android.com/about/versions/15/behavior-changes-15)
- Direct codebase analysis of all 67 Kotlin files in OmniStream project

### Secondary (MEDIUM confidence)
- [Kotatsu Architecture (DeepWiki)](https://deepwiki.com/KotatsuApp/Kotatsu/5-developer-guide)
- [CloudStream Subtitles Wiki](https://cloudstream.miraheze.org/wiki/Subtitles)
- [Seekee App Removal (CBR)](https://www.cbr.com/anime-piracy-seekee-legal-streaming-bury/)
- [Android 16 PiP Breaking Change](https://medium.com/@khamd0786/migrating-to-android-16-why-your-picture-in-picture-might-break-and-how-to-fix-it-9d9b8bc79567)

### Tertiary (LOW confidence)
- Play Store specific review behavior (based on developer forum reports, not official documentation)
- Decryption chain behavior per quality level (inferred from architecture, needs code verification)

---
*Research completed: 2026-01-29*
*Ready for roadmap: yes*
