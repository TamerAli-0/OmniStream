# Feature Landscape: OmniStream v2.0

**Domain:** Android manga reader + video streaming aggregator (Tachiyomi/CloudStream hybrid)
**Researched:** 2026-01-29
**Scope:** New features for existing app with multi-source aggregation, reader, player, auth, favorites, library, and search already built.

---

## Table Stakes

Features users of apps like Tachiyomi, CloudStream, and Kotatsu expect as baseline. Missing any of these makes the app feel incomplete relative to competitors.

### 1. Download System -- Manga Chapters

| Aspect | Expected Behavior | Complexity | Depends On |
|--------|-------------------|------------|------------|
| Single chapter download | Long-press or tap download icon on any chapter; downloads all pages as images to local storage | Medium | Existing chapter navigation, Room DB |
| Batch download | Select multiple chapters or "download all" / "download unread"; enqueue as batch | Medium | Single chapter download |
| Download queue with progress | Persistent notification showing current download, queue position, per-page progress | High | WorkManager / ForegroundService |
| Pause / resume downloads | Tap notification or queue screen to pause; resume later even after app kill | High | WorkManager persistence |
| Wi-Fi only option | Settings toggle to restrict downloads to Wi-Fi; respect constraint automatically | Low | WorkManager network constraints |
| Auto-download new chapters | When library updates detect new chapters, auto-download for favorited manga | Medium | Library sync, download queue |
| Delete after reading | Optional setting to auto-delete downloaded chapters once marked read | Low | Read-state tracking |
| Storage management | View total download size per manga; bulk delete; choose storage location (internal/SD) | Medium | File management |
| Download format | Store as directory of images (simplest) or CBZ archive (portable, Tachiyomi standard) | Medium | Compression library |

**How it works in reference apps:**
- **Tachiyomi/forks:** Downloads are per-chapter directories of images under a manga folder. Queue is managed with a foreground service. Progress shown in notification and a dedicated Downloads screen. Supports auto-download on library update. Filters in library for "downloaded" status.
- **Kotatsu:** Uses WorkManager with `FOREGROUND_SERVICE_TYPE_DATA_SYNC`. New download manager (2025) supports pause/resume, download history, skip-on-error, and per-source throttling to avoid IP bans. Stores downloads with Room DB tracking.

**Architecture recommendation:** Use WorkManager with `CoroutineWorker` and `setForeground()` for Android 14+ compliance. Track download queue in Room DB (not just in-memory). Each chapter download is one `OneTimeWorkRequest`; do NOT chain them (chaining fails entire chain on single failure). Use a coordinator pattern: Room table holds queue state, worker polls for next pending item.

### 2. Download System -- Video Episodes

| Aspect | Expected Behavior | Complexity | Depends On |
|--------|-------------------|------------|------------|
| Episode download | Download button on episode detail; downloads selected quality | High | Quality selection, source extraction |
| Quality selection for download | Choose quality before downloading (not just streaming quality) | Medium | Quality selection feature |
| Download with subtitles | Download selected subtitle track alongside video; option for multiple languages | High | Subtitle system |
| Offline playback | Play downloaded episodes with full player controls; no network required | Medium | Video player, local file source |
| Storage warnings | Warn when storage is low; show episode file sizes before download | Low | Android storage APIs |

**How it works in reference apps:**
- **CloudStream:** Downloads video + subtitles (English by default, configurable). Multi-select downloads added in recent versions. Stores in app-specific directory. Offline playback uses same player with local source.
- **Key challenge:** Video files are large (200MB-2GB per episode). Must show accurate progress, handle interruptions gracefully, and resume partial downloads. Unlike manga (small images), video download failure mid-file is costly.

**Architecture recommendation:** Same WorkManager infrastructure as manga, but with chunked download support and byte-range resume (HTTP Range headers). Store download metadata (URL, quality, subtitle tracks, byte progress) in Room. Use OkHttp for downloads with progress interceptor.

### 3. Subtitle Rendering

| Aspect | Expected Behavior | Complexity | Depends On |
|--------|-------------------|------------|------------|
| SRT support | Render SubRip subtitles with correct timing | Low | Media3 ExoPlayer (built-in) |
| VTT support | Render WebVTT subtitles with styling | Low | Media3 ExoPlayer (built-in) |
| ASS/SSA support | Render Advanced SubStation Alpha (critical for anime) | High | See note below |
| Subtitle source selection | Toggle between available subtitle tracks from source | Medium | Existing player UI |
| External subtitle loading | Load subtitle file from device storage | Medium | File picker + Media3 API |
| Online subtitle search | Search OpenSubtitles/SubDL for matching subtitles | High | API integration |
| Subtitle customization | Font size, color, background opacity, font family | Medium | SubtitleView configuration |
| Subtitle sync/offset | Manual +/- offset adjustment for out-of-sync subtitles | Medium | Custom player control |
| Subtitle language preference | Remember preferred language; auto-select matching track | Low | SharedPreferences/DataStore |

**Critical note on ASS/SSA:** Media3's ExoPlayer strips ASS styling (fonts, positioning, animations) when rendering. For anime content this is a significant gap -- anime fansubs rely heavily on ASS formatting. Options:
1. **Accept stripped rendering** (easiest, but anime users will complain)
2. **Integrate libass** via JNI for native ASS rendering (complex, but correct)
3. **Use libmpv as alternative player engine** (most complete, but heavyweight)

**Recommendation:** Start with Media3's built-in SRT/VTT support (covers 80% of use cases). Add subtitle customization and sync offset. Defer full ASS support to a later phase -- it is a deep rabbit hole. Flag ASS as a known limitation in v2.0.

**How it works in reference apps:**
- **CloudStream:** Three subtitle sources: auto from extension, local file, online (OpenSubtitles). Customizable font/size/color/style. Manual sync offset. English auto-downloaded for offline. SubDL integration added mid-2024.

### 4. Picture-in-Picture (PiP)

| Aspect | Expected Behavior | Complexity | Depends On |
|--------|-------------------|------------|------------|
| Enter PiP on home press | Video continues in floating window when user navigates away | Medium | Android 8.0+ PiP API |
| Auto-enter PiP (Android 12+) | Smooth transition with `setAutoEnterEnabled(true)` | Low | Android 12+ |
| sourceRectHint | Smooth animation from video bounds to PiP window | Low | PlayerView bounds calculation |
| PiP playback controls | Play/pause, skip forward/back as remote actions in PiP window | Medium | `PictureInPictureParams` remote actions |
| Hide non-video UI in PiP | Hide controls, episode info, etc. when in PiP mode | Low | `onPictureInPictureModeChanged` |
| Audio focus management | Handle audio focus properly (pause if another app takes focus) | Low | AudioManager API |
| Disable PiP for manga | PiP only available in video player, not reader | Low | Manifest activity config |

**How it works in reference apps:**
- **CloudStream:** PiP supported with basic play/pause controls. Enters on home button press during video playback.

**Architecture recommendation:** This is well-documented Android API territory. Declare `android:supportsPictureInPicture="true"` on video activity. Use `setAutoEnterEnabled(true)` for Android 12+. Set `sourceRectHint` to PlayerView bounds. Add remote actions for play/pause/seek. Override `onPictureInPictureModeChanged` to hide/show UI. Straightforward -- no custom architecture needed.

### 5. Quality Selection

| Aspect | Expected Behavior | Complexity | Depends On |
|--------|-------------------|------------|------------|
| Auto quality (adaptive) | Default: ExoPlayer selects quality based on bandwidth | Low | Media3 DefaultTrackSelector (already have) |
| Manual quality override | User picks from available qualities (360p/480p/720p/1080p) | Medium | TrackSelectionParameters API |
| Quality picker UI | Bottom sheet or dialog listing available tracks with resolution/bitrate | Medium | Player UI overlay |
| Remember quality preference | Persist user's preferred quality (auto/specific) across sessions | Low | DataStore |
| Quality per-network | Optional: different default for Wi-Fi vs mobile data | Low | ConnectivityManager check |
| Quality for downloads | Separate quality selector when initiating downloads | Low | Reuse picker UI |

**How it works in reference apps:**
- **CloudStream:** Quality selection via source picker in player. Users can choose between available video qualities from the source. Auto quality follows adaptive streaming when HLS/DASH manifest provides multiple bitrates.

**Architecture recommendation:** Use Media3's `DefaultTrackSelector` with `TrackSelectionParameters`. For manual override, call `player.trackSelectionParameters = player.trackSelectionParameters.buildUpon().setMaxVideoSize(width, height).build()`. Expose available tracks via `player.currentTracks` and map to user-friendly labels. Store preference in DataStore.

### 6. Continue Watching / Reading Resume

| Aspect | Expected Behavior | Complexity | Depends On |
|--------|-------------------|------------|------------|
| Video position persistence | Save playback position when user exits; resume on return | Low | Room DB |
| Manga page persistence | Save current page in chapter when user exits reader | Low | Room DB |
| Continue watching row | Home screen row showing recently watched with progress bar | Medium | Room query + UI |
| Continue reading row | Home screen row showing recently read manga with chapter progress | Medium | Room query + UI |
| Cross-device sync | Sync progress via cloud (if cloud sync exists) | High | Existing cloud sync system |
| Auto-mark completed | Mark episode/chapter as completed when >90% watched/read | Low | Progress threshold check |
| Next episode/chapter auto-advance | After completing, offer or auto-load next item | Medium | Source navigation logic |

**How it works in reference apps:**
- **Tachiyomi/Kotatsu:** Tracks last-read chapter and page per manga. Library shows unread count. Resume opens exact page. Chapter marked read when last page viewed.
- **CloudStream:** Saves playback position per episode. "Continue watching" section on home. Resumes from saved position. Watch Next integration on Android TV.
- **Key UX expectation:** Resume must be instantaneous. No "do you want to resume?" dialog -- just resume. If completed (>90%), start from beginning of NEXT item instead.

**Architecture recommendation:** Create a `WatchHistory` / `ReadHistory` Room entity with fields: `contentId`, `episodeOrChapterId`, `progress` (seconds or page number), `totalDuration` (or total pages), `lastAccessed` timestamp, `completed` boolean. Update on every pause/exit. Query for "continue" rows ordered by `lastAccessed DESC WHERE NOT completed`. This is low-complexity, high-impact.

### 7. Enhanced Search

| Aspect | Expected Behavior | Complexity | Depends On |
|--------|-------------------|------------|------------|
| Debounced search-as-you-type | 300-500ms debounce before firing search query | Low | Kotlin Flow `debounce` |
| Search history | Persist recent searches; show as suggestions | Low | Room DB or DataStore |
| Clear history | Option to clear individual items or all history | Low | Room delete |
| Search suggestions | Show suggestions from history + trending/popular | Medium | Combine local history with source API |
| Multi-source search | Search across all enabled sources simultaneously | Medium | Existing unified search (enhance) |
| Filter results | Filter by type (manga/anime/movie), source, year, genre | Medium | UI filter chips + query params |
| Recent/trending display | Show popular content when search bar is empty (discovery) | Medium | Source API integration |

**How it works in reference apps:**
- **Tachiyomi:** Search with source-specific filters (genre, status, sort). Global search across all sources with per-source result cards.
- **Kotatsu:** Tag-based search. Random discovery button. Recommendations v2 with improved algorithm.
- **CloudStream:** Search across providers. Filter by type.

**Architecture recommendation:** Use Kotlin Flow pipeline: `searchQuery.debounce(400).distinctUntilChanged().filter { it.length >= 2 }.flatMapLatest { query -> searchRepository.search(query) }`. Store history in Room with timestamp. Show history + suggestions when field focused but empty. Cancel in-flight searches on new query via `flatMapLatest`.

---

## Differentiators

Features that go beyond what Tachiyomi/CloudStream offer individually. These create competitive advantage for OmniStream as a unified app.

### 1. Unified Continue Row (Manga + Video)

| Aspect | Description | Complexity |
|--------|-------------|------------|
| Single "Continue" section | One unified home row mixing manga chapters and video episodes, sorted by recency | Medium |
| Why it differentiates | Tachiyomi only shows manga; CloudStream only shows video. OmniStream shows both, which is the whole value prop. |
| Implementation | Single `ContinueItem` sealed class wrapping either video or manga progress; single RecyclerView/LazyRow |

### 2. Cross-Content Recommendations

| Aspect | Description | Complexity |
|--------|-------------|------------|
| "Watching the anime? Read the manga" | When user watches an anime, suggest the source manga and vice versa | High |
| Why it differentiates | No single app does this today. Requires content ID matching (MAL/AniList IDs). |
| Implementation | Match content across sources via tracking service IDs (MyAnimeList, AniList). Show cross-type cards. |

### 3. Smart Download Queue (Manga + Video Unified)

| Aspect | Description | Complexity |
|--------|-------------|------------|
| One download manager for all content | Single queue showing manga chapter downloads and video episode downloads with unified progress | Medium |
| Priority system | User can reorder queue; manga downloads (small) can interleave with video downloads (large) | Medium |
| Why it differentiates | Separate apps have separate download systems. Unified queue is better UX. |

### 4. Adaptive Onboarding

| Aspect | Description | Complexity |
|--------|-------------|------------|
| Content-type onboarding | Ask user on first launch: "What do you enjoy?" (manga/anime/movies/all) and customize home screen | Medium |
| Source setup wizard | Guided setup for enabling content sources rather than a settings dump | Medium |
| Why it differentiates | Tachiyomi and CloudStream both have steep learning curves. Play Store apps need smoother onboarding. |

### 5. Offline-First Reading/Watching

| Aspect | Description | Complexity |
|--------|-------------|------------|
| Predictive downloads | Auto-download next 2-3 chapters/episodes based on reading/watching pattern | High |
| Smart storage management | Auto-clean watched/read content; keep configurable buffer of upcoming content | Medium |
| Why it differentiates | Goes beyond manual download; anticipates user needs |

---

## Anti-Features

Things to deliberately NOT build. Common mistakes in this domain.

### 1. DO NOT: Build a Custom Video Player From Scratch

**Why teams try it:** Want pixel-perfect control over every UI element.
**Why it fails:** Video playback is enormously complex (codec support, DRM, adaptive streaming, subtitle rendering, hardware acceleration). Media3/ExoPlayer handles this. Wrapping it with custom UI is fine; replacing the engine is not.
**Do instead:** Use Media3 ExoPlayer with custom `PlayerView` overlays for controls.

### 2. DO NOT: Implement Full ASS/SSA Subtitle Rendering in v2.0

**Why teams try it:** Anime fans demand it immediately.
**Why it fails:** ASS rendering requires either JNI integration with libass (C library, complex build, crash-prone) or embedding libmpv (large binary, different player lifecycle). Either approach is a multi-week effort with ongoing maintenance burden.
**Do instead:** Ship SRT/VTT support first. Document ASS as a known limitation. Plan ASS for v3.0 with proper libass or libmpv integration.

### 3. DO NOT: Download Everything as a Single Monolithic File Manager

**Why teams try it:** Seems simpler to have one giant DownloadManager class.
**Why it fails:** Manga downloads (100KB images x 20 pages) and video downloads (500MB+ files) have fundamentally different characteristics. Manga needs parallel page downloads within a chapter; video needs chunked byte-range downloads with resume. Conflating them creates unmaintainable code.
**Do instead:** Shared download queue UI and Room schema, but separate `MangaDownloadWorker` and `VideoDownloadWorker` implementations.

### 4. DO NOT: Sync Every Interaction to Cloud in Real-Time

**Why teams try it:** Want perfect cross-device sync.
**Why it fails:** Syncing read position on every page turn or every 5 seconds of video creates massive API load, battery drain, and conflict resolution nightmares. Users on mobile data will hate it.
**Do instead:** Sync on natural boundaries: chapter completed, episode paused/exited, app backgrounded. Batch sync with conflict resolution (last-write-wins with timestamp).

### 5. DO NOT: Build a Torrent/P2P Download System

**Why teams try it:** Some sources distribute via torrents; seems powerful.
**Why it fails:** Play Store will reject the app. Torrent libraries are heavyweight. Legal exposure increases dramatically.
**Do instead:** HTTP-only downloads. If a source requires torrent, skip that source.

### 6. DO NOT: Over-Engineer Search Suggestions with ML

**Why teams try it:** Want "smart" autocomplete like Google.
**Why it fails:** Search corpus is relatively small (manga/anime titles). Simple prefix matching on history + source API suggestions is sufficient and instant. On-device ML models add APK size and battery cost for negligible improvement.
**Do instead:** Simple history-based suggestions + source-provided suggestions. Prefix matching is fine.

### 7. DO NOT: Custom Gesture System for PiP

**Why teams try it:** Want swipe-to-PiP or custom animations.
**Why it fails:** Android's PiP API handles transitions. Custom gesture detection conflicts with the system's own gesture navigation (especially on Android 12+ with gesture nav). Users expect system-standard PiP behavior.
**Do instead:** Use `setAutoEnterEnabled(true)` (Android 12+) and standard `enterPictureInPictureMode()` for older versions.

---

## Feature Dependencies

```
                    +------------------+
                    | Room DB (exists) |
                    +--------+---------+
                             |
              +--------------+--------------+
              |              |              |
     +--------v---+  +------v------+  +----v--------+
     | Download    |  | Continue    |  | Search      |
     | Queue DB    |  | History DB  |  | History DB  |
     +--------+----+  +------+------+  +----+--------+
              |              |              |
    +---------+------+       |              |
    |                |       |              |
+---v----+    +------v-+  +-v---------+  +-v----------+
| Manga  |    | Video  |  | Continue  |  | Enhanced   |
| DL     |    | DL     |  | Watching/ |  | Search     |
| Worker |    | Worker |  | Reading   |  | w/Debounce |
+--------+    +---+----+  +-----------+  +------------+
                  |
          +-------+-------+
          |               |
    +-----v-----+   +----v------+
    | Subtitle   |   | Quality   |
    | Rendering  |   | Selection |
    +------------+   +-----------+
                          |
                    +-----v-----+
                    |    PiP    |
                    +-----------+

Legend:
  A --> B means B depends on A or benefits from A being built first
```

**Key dependency chains:**
1. Download Queue DB must exist before either download worker
2. Video Download Worker needs Quality Selection (to know what quality to download)
3. Subtitle download requires Subtitle Rendering (to know which formats to save)
4. PiP is fully independent but benefits from Quality Selection being stable
5. Continue Watching/Reading is independent of downloads but shares Room DB
6. Enhanced Search is fully independent of all other features

---

## MVP Feature Recommendation (v2.0)

### Must Ship (Phase 1-2)

These features are table stakes that users of Tachiyomi/CloudStream expect:

1. **Continue Watching/Reading** -- Highest ROI. Low complexity, massive UX improvement. Build first.
2. **Enhanced Search with Debounce** -- Low complexity upgrade to existing search. Quick win.
3. **Quality Selection** -- Medium complexity. Users expect manual quality control. Uses existing Media3 APIs.
4. **Manga Chapter Downloads** -- High value. WorkManager + Room queue. Core offline feature.

### Ship Next (Phase 3)

5. **Picture-in-Picture** -- Medium complexity but well-documented API. Expected for video apps.
6. **Video Episode Downloads** -- Builds on manga download infrastructure. More complex due to file size.
7. **Subtitle Rendering (SRT/VTT)** -- Media3 built-in support. Subtitle customization UI.

### Defer to v3.0

8. **ASS/SSA full rendering** -- Requires libass/libmpv integration. Too complex for v2.0.
9. **Cross-content recommendations** -- Requires tracking service API integration and content matching.
10. **Predictive/auto downloads** -- Needs usage pattern analysis. Nice-to-have.
11. **Online subtitle search (OpenSubtitles/SubDL)** -- API integration + search UI. Deferrable.

### Polish Layer (Parallel to All Phases)

12. **Onboarding flow** -- Build incrementally as features land.
13. **Accessibility** -- Content descriptions, TalkBack, tap targets. Must be ongoing, not a phase.
14. **Crash-free stability** -- LeakCanary, strict mode, Firebase Crashlytics. From day one.

---

## Play Store Polish Expectations

For Play Store readiness, the following non-feature work is required alongside feature development:

| Area | Expectation | Implementation |
|------|-------------|----------------|
| Crash-free rate | >99.5% crash-free sessions | LeakCanary, StrictMode, Crashlytics |
| ANR rate | <0.5% ANR rate | No main-thread I/O; coroutines everywhere |
| Startup time | <500ms cold start | Lazy initialization, App Startup library |
| Onboarding | <3 screens, skippable | ViewPager2 or HorizontalPager |
| Accessibility | TalkBack navigable, 48dp tap targets, contrast ratio 4.5:1 | `contentDescription`, Material3 components |
| Dark/light theme | Both must work; follow system default | Material3 dynamic theming (already have themes) |
| Back navigation | Predictive back gesture (Android 14+) | Enable in manifest, handle properly |
| Edge-to-edge | Content draws behind system bars | `WindowCompat.setDecorFitsSystemWindows(window, false)` |
| Offline behavior | Graceful degradation; show cached/downloaded content | Connectivity checks, offline-first UI states |
| Retention analytics | Day 1, Day 7, Day 30 retention tracking | Firebase Analytics from day one |

---

## Confidence Assessment

| Area | Confidence | Reasoning |
|------|------------|-----------|
| Download system patterns | HIGH | WorkManager approach verified via official Android docs, Kotatsu architecture confirmed via DeepWiki |
| Subtitle rendering capabilities | HIGH | Media3 supported formats verified via official Android developer docs; ASS limitation confirmed across multiple GitHub issues |
| PiP implementation | HIGH | Official Android developer documentation is comprehensive and current |
| Quality selection | HIGH | Media3 TrackSelector API well-documented; verified via official docs |
| Continue watching patterns | MEDIUM | Standard pattern but implementation details vary; Android TV Watch Next API verified, mobile pattern is common sense |
| Search debounce | HIGH | Kotlin Flow `debounce` operator is official API; pattern well-established |
| Play Store polish criteria | MEDIUM | Based on general Android best practices; specific Play Store thresholds may vary |
| Reference app behavior | MEDIUM | Based on WebSearch findings and documentation; not direct code review of Tachiyomi/CloudStream/Kotatsu |

## Sources

- [Android PiP Documentation](https://developer.android.com/develop/ui/views/picture-in-picture)
- [Android PiP Design Guide](https://developer.android.com/design/ui/mobile/guides/home-screen/picture-in-picture)
- [Media3 Track Selection](https://developer.android.com/media/media3/exoplayer/track-selection)
- [Media3 Supported Formats](https://developer.android.com/media/media3/exoplayer/supported-formats)
- [WorkManager Getting Started](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started)
- [WorkManager Long-Running Workers](https://developer.android.com/develop/background-work/background-tasks/persistent/how-to/long-running)
- [Android Watch Next Codelab](https://developer.android.com/codelabs/watchnext-for-movie-tv-episodes)
- [Media3 Background Playback](https://developer.android.com/media/media3/session/background-playback)
- [Android Accessibility Principles](https://developer.android.com/guide/topics/ui/accessibility/principles)
- [Kotatsu Background Services (DeepWiki)](https://deepwiki.com/KotatsuApp/Kotatsu/5-developer-guide)
- [CloudStream Subtitles Wiki](https://cloudstream.miraheze.org/wiki/Subtitles)
- [KotatsuApp GitHub](https://github.com/KotatsuApp/Kotatsu)
- [Kotlin Flow debounce API](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/debounce.html)
- [Jellyfin ASS Subtitle Discussion](https://github.com/jellyfin/jellyfin-android/issues/1833)
- [ExoPlayer Adaptive Streaming Guide (Dec 2025)](https://medium.com/@ashiiq666/exoplayer-adaptive-streaming-a-complete-implementation-guide-jetpack-compose-c84bb42bfd0c)
- [WorkManager Download Pattern (ProAndroidDev)](https://proandroiddev.com/step-by-step-guide-to-download-files-with-workmanager-b0231b03efd1)
