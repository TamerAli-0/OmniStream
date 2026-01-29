# Domain Pitfalls

**Project:** OmniStream v2.0 -- Play Store Ready
**Domain:** Android streaming/reading app with scraped content sources
**Researched:** 2026-01-29
**Focus:** Adding downloads, subtitles, PiP, search improvements, and Play Store polish to existing system

---

## Critical Pitfalls

Mistakes that cause app removal, account suspension, or forced rewrites.

---

### Pitfall 1: Play Store Removal for Copyright-Infringing Content Aggregation

**What goes wrong:** Google Play removes the app (or suspends the developer account) because the app aggregates copyrighted manga, anime, and movie content from scraped third-party sources without licensing agreements. This is the single highest risk for OmniStream.

**Why it happens:** Google Play explicitly prohibits "apps that encourage users to stream and download copyrighted works, including music and video, in violation of applicable copyright law." OmniStream scrapes from sites like FlickyStream, WatchFlix, Goojara, and others that host unlicensed content. Google has actively removed anime/manga aggregator apps in 2025 (Seekee was removed for exactly this pattern). Rights holders like Disney, Shueisha, and Funimation are filing DMCA takedowns aggressively.

**Consequences:**
- App removed from Play Store
- Developer account suspended (potentially permanent)
- Loss of all published apps under that account
- Account suspension can cascade to ban the developer from creating new accounts

**Warning signs:**
- App description mentions specific copyrighted franchises by name
- Sources are known piracy sites
- AES decryption chains exist specifically to bypass content protection
- No licensing agreements exist for any content

**Prevention:**
- Do NOT submit OmniStream to Google Play in its current form with scraped copyrighted sources. This is nearly guaranteed to result in removal.
- Option A: Remove all scraped sources and only use officially licensed APIs (MangaDex is the only source that may qualify, as it hosts community-translated works with some legitimacy)
- Option B: Distribute via sideloading (APK/GitHub releases) only, skip Play Store entirely
- Option C: Reframe the app as a "browser" or "aggregator framework" without bundled sources -- but this still carries risk if Google determines the primary purpose is accessing pirated content
- Option D: Obtain actual licensing agreements (impractical for an indie project)

**Phase impact:** This must be resolved BEFORE Play Store submission. No amount of polish fixes a policy violation.

**Confidence:** HIGH -- Based on Google's published Intellectual Property policy, the Seekee removal precedent, and the explicit prohibition on apps that facilitate streaming copyrighted content.

**Sources:**
- [Google Play Intellectual Property Policy](https://support.google.com/googleplay/android-developer/answer/9888072)
- [Seekee app removal for anime piracy](https://www.cbr.com/anime-piracy-seekee-legal-streaming-bury/)
- [Google Fighting Piracy](https://fightingpiracy.withgoogle.com/google-policies-by-product/)

---

### Pitfall 2: Download System Enables Offline Piracy -- Escalates Policy Violation

**What goes wrong:** Adding a download system to an app that already streams copyrighted content transforms it from a "streaming aggregator" (bad) to a "piracy distribution tool" (worse). Downloading copyrighted content for offline use is explicitly called out in Google Play policy.

**Why it happens:** The download feature is a natural user request, but it compounds the copyright issue. Google's policy states apps must not "encourage users to stream and download copyrighted works." The download feature provides direct evidence of facilitating copyright infringement.

**Consequences:**
- Stronger case for app removal (not just streaming, but enabling persistent copies)
- Higher likelihood of developer account suspension vs. just app removal
- Downloaded content on user devices creates evidence trail

**Warning signs:**
- Download feature saves full video/manga files to device storage
- No DRM or content protection on downloaded files
- Downloads persist after app uninstall (if saved to shared storage)

**Prevention:**
- If targeting Play Store: do not implement downloads for scraped copyrighted sources
- If distributing via sideloading: implement downloads freely, but understand the legal exposure
- Consider downloads only for MangaDex content (community translations with more ambiguous copyright status)
- If downloads are implemented: use app-internal storage (deleted on uninstall), not shared MediaStore

**Phase impact:** Download system design must account for distribution strategy. If Play Store is the target, downloads become a liability rather than a feature.

**Confidence:** HIGH -- Direct reading of Google Play policy on downloading copyrighted works.

---

### Pitfall 3: Access Gate (VIP/Standard Passcode) Blocks Play Store Review

**What goes wrong:** Google Play reviewers cannot access the app's full functionality because it is behind a passcode gate. The app gets rejected for "Paywall Restriction" or "Broken Functionality" -- the reviewer sees a locked screen and flags it.

**Why it happens:** Google requires reviewers to access all app content. The VIP/Standard passcode system with SHA-256 hashing blocks reviewers unless credentials are provided. Multiple developers in 2025 reported apps removed for "Paywall Restriction" even with proper login credentials submitted, because the review system is partially automated and the gate mechanism is non-standard.

**Consequences:**
- App rejected during review
- Repeated rejections impact developer account standing
- May require architectural changes to the access system

**Warning signs:**
- App shows a passcode screen on first launch
- No way for reviewers to bypass the gate
- App Access Declaration in Play Console is not filled out

**Prevention:**
- Fill out the App Access Declaration in Play Console with working credentials
- Implement a reviewer bypass: a specific gesture or code that grants full access for review purposes
- Consider making the app free-to-use with the gate removed for Play Store version
- If keeping the gate: provide a "demo mode" that shows full functionality
- Document clearly in Play Console: "Enter passcode: [X] for full access"

**Phase impact:** Must be addressed in Play Store polish phase. The access gate system may need to be reworked or removed entirely for the Play Store build.

**Confidence:** HIGH -- Based on multiple 2025 developer reports of passcode/paywall rejection and Google's published App Access Declaration requirements.

**Sources:**
- [Paywall Restriction app removal thread](https://support.google.com/googleplay/android-developer/thread/346992873)
- [Deceptive Behavior Policy](https://support.google.com/googleplay/android-developer/answer/9888077)

---

### Pitfall 4: Deceptive Behavior Flagging for Hidden Scraping/Decryption

**What goes wrong:** Google's automated review detects that the app makes network requests to known piracy domains, performs AES decryption of content streams, or has functionality not disclosed in the app description. The app is flagged for "Deceptive Behavior" or "Hidden Functionality."

**Why it happens:** Google's review process includes static analysis of the APK and dynamic analysis of network behavior. The Vidzee/Cloudnestra decryption chains, scraping of third-party sites, and WebView-based source extraction (AnimeKai) are all forms of hidden functionality that are not disclosed to users or reviewers.

**Consequences:**
- App rejected for Deceptive Behavior policy violation
- Developer account flagged for potential suspension
- Harder to get future apps approved

**Warning signs:**
- Network traffic to domains known for pirated content
- AES decryption code with hardcoded keys in the APK
- WebView loading third-party sites in the background
- App description does not mention scraping or third-party content sources

**Prevention:**
- If targeting Play Store: the app's true functionality must match its description
- Do not hide the nature of content sources -- but disclosing them also triggers copyright policy
- This creates a catch-22: disclosing scraping triggers IP policy, hiding it triggers deceptive behavior policy
- Reinforces Pitfall 1 conclusion: the current architecture is fundamentally incompatible with Play Store

**Phase impact:** This is not fixable with polish. It is an architectural/business model issue.

**Confidence:** HIGH -- Google's Deceptive Behavior policy explicitly covers hidden functionality and undisclosed network behavior.

---

## Moderate Pitfalls

Mistakes that cause delays, rework, or degraded user experience.

---

### Pitfall 5: Foreground Service Type Misconfiguration for Downloads

**What goes wrong:** The download service uses the wrong foreground service type, hits the 6-hour timeout on Android 15+, or fails Play Console's foreground service declaration requirement. Downloads silently stop or the app is rejected.

**Why it happens:** Android 14+ requires declaring specific foreground service types in the manifest AND in Play Console with video demonstration. Android 15 introduced a 6-hour timeout for `dataSync` foreground services. The `dataSync` type is being deprecated in favor of `DownloadManager` or user-initiated data transfer jobs. Many developers use `dataSync` for downloads and hit the timeout.

**Consequences:**
- Downloads silently stop after 6 hours on Android 15+
- App rejected during Play Console review for missing foreground service declaration
- Crash if `onTimeout()` is not handled (system throws internal exception)

**Warning signs:**
- Using `FOREGROUND_SERVICE_DATA_SYNC` for download operations
- No `onTimeout()` handler in the service
- Play Console foreground service declaration not submitted
- No video demonstration of foreground service usage prepared

**Prevention:**
- Use WorkManager with `CoroutineWorker` for downloads instead of raw foreground services
- If foreground service is needed, use the correct type and handle `onTimeout()`
- Prepare Play Console declaration with video demo before submission
- For large downloads, implement chunked downloading with resume capability
- Use `DownloadManager` system service for HTTP downloads (handles retries, connectivity changes, notifications automatically)

**Phase impact:** Download system architecture phase. Must decide WorkManager vs. foreground service vs. DownloadManager early.

**Confidence:** HIGH -- Based on official Android 15 behavior changes documentation and Play Console requirements.

**Sources:**
- [Foreground service requirements](https://support.google.com/googleplay/android-developer/answer/13392821)
- [Android 15 dataSync timeout](https://developer.android.com/about/versions/15/behavior-changes-15)
- [DownloadService issue with Android 15](https://github.com/androidx/media/issues/2614)

---

### Pitfall 6: Scoped Storage Breaks Download File Access

**What goes wrong:** Downloaded files are inaccessible after app restart, files saved to app-internal storage are deleted on uninstall (frustrating users), or the app requests `MANAGE_EXTERNAL_STORAGE` which triggers Play Store rejection.

**Why it happens:** Android's scoped storage (enforced since Android 11) restricts file access. Apps cannot access files created by other apps in Downloads. `requestLegacyExternalStorage` no longer works. MediaStore has quirks: different collections have different access rules, `IS_PENDING` flag must be used for large downloads, and MIME types must be correct.

**Consequences:**
- Users lose downloaded content on app uninstall (if using app-internal storage)
- "File not found" errors when trying to play downloaded content
- Play Store rejection if requesting `MANAGE_EXTERNAL_STORAGE` without justification
- Downloaded manga images scattered across wrong MediaStore collections

**Warning signs:**
- Using `File` API directly instead of MediaStore or SAF
- Not setting `IS_PENDING` flag during large downloads
- Requesting `MANAGE_EXTERNAL_STORAGE` permission
- Downloaded files placed in wrong MediaStore collection (images in Downloads, videos in Images, etc.)

**Prevention:**
- For manga: save to app-internal storage (`getExternalFilesDir()`) -- accepts deletion on uninstall as tradeoff for simplicity
- For video: use MediaStore.Video or MediaStore.Downloads with correct MIME types
- Set `IS_PENDING = 1` during download, flip to `0` on completion
- Use `ContentResolver.insert()` and `openOutputStream()` pattern, not direct file paths
- Never request `MANAGE_EXTERNAL_STORAGE` -- use scoped alternatives
- Test file access after app restart and device reboot

**Phase impact:** Download system design phase. Storage strategy must be decided upfront.

**Confidence:** HIGH -- Based on official Android scoped storage documentation and known developer issues.

**Sources:**
- [Scoped Storage Downloads pitfalls](https://commonsware.com/blog/2020/01/11/scoped-storage-stories-diabolical-details-downloads.html)
- [MANAGE_EXTERNAL_STORAGE Play Store requirements](https://support.google.com/googleplay/android-developer/answer/10467955)

---

### Pitfall 7: PiP Mode Breaks on Android 16 with Predictive Back Gesture

**What goes wrong:** Picture-in-Picture mode that works on Android 14-15 silently breaks on Android 16 (Baklava). When user presses back, instead of entering PiP, the activity is destroyed.

**Why it happens:** Android 16 introduces Predictive Back Navigation, which aggressively assumes the user intends to finish the activity on back swipe. This preempts `enterPictureInPictureMode()` because the system finishes the activity before the PiP trigger executes.

**Consequences:**
- PiP stops working on Android 16 devices
- Users lose video playback when navigating away
- Difficult to debug because it works on all older Android versions

**Warning signs:**
- PiP works in testing on Android 14/15 but fails on Android 16 emulator
- `onBackPressed()` or back gesture handling triggers PiP entry
- Activity has `android:enableOnBackInvokedCallback="true"`

**Prevention:**
- Set `android:enableOnBackInvokedCallback="false"` for the video player activity specifically
- Use `setAutoEnterEnabled(true)` on `PictureInPictureParams` (Android 12+) so PiP triggers automatically when user navigates away, not on back press
- Always provide `sourceRectHint` for smooth transition animations
- Test on Android 16 emulator specifically
- Do NOT rely on `onBackPressed()` for PiP entry -- use `onUserLeaveHint()` or `setAutoEnterEnabled`

**Phase impact:** PiP implementation phase. Must test on Android 16 from the start.

**Confidence:** HIGH -- Documented by Android developers who encountered this exact issue migrating to Android 16.

**Sources:**
- [Android 16 PiP breaking change](https://medium.com/@khamd0786/migrating-to-android-16-why-your-picture-in-picture-might-break-and-how-to-fix-it-9d9b8bc79567)
- [Official PiP documentation](https://developer.android.com/develop/ui/views/picture-in-picture)

---

### Pitfall 8: Subtitle MIME Type Mismatches and Dynamic Loading Failures

**What goes wrong:** Subtitles either do not display, display the wrong format, or cannot be changed once playback has started. ExoPlayer (Media3) subtitle support has several non-obvious requirements.

**Why it happens:** ExoPlayer requires exact MIME type matching for sideloaded subtitles. Using `APPLICATION_SUBRIP` for a `.vtt` file (or vice versa) silently fails. Replacing subtitles dynamically after playback starts does not work reliably -- the player continues using the old subtitle track. VobSub subtitle support is known to be unreliable (rendering roughly half of subtitles).

**Consequences:**
- Subtitles appear to not work at all (silent failure on MIME mismatch)
- Users cannot switch subtitle languages mid-playback
- Certain subtitle formats render incompletely

**Warning signs:**
- Using deprecated `SingleSampleMediaSource` API instead of `MediaItem.SubtitleConfiguration`
- Not specifying MIME type when adding subtitle tracks
- Attempting to swap subtitles without rebuilding the MediaItem
- Following pre-Media3 ExoPlayer tutorials

**Prevention:**
- Use `MediaItem.SubtitleConfiguration.Builder` with explicit MIME type for every subtitle track
- Detect subtitle format from file extension AND content header, not just extension
- To change subtitles mid-playback: rebuild the `MediaItem` with the new subtitle configuration and seek to the previous position
- Use `DefaultMediaSourceFactory` (required for subtitle sideloading)
- Stick to SRT and WebVTT formats (best supported); avoid VobSub
- Test with actual subtitle files from the scraped sources, not just test fixtures

**Phase impact:** Subtitle implementation phase. Format detection and MIME mapping must be correct from the start.

**Confidence:** HIGH -- Based on open issues in the androidx/media repository and official ExoPlayer documentation.

**Sources:**
- [ExoPlayer subtitle replacing issue](https://github.com/androidx/media/issues/1696)
- [VobSub rendering issue](https://github.com/androidx/media/issues/2935)
- [ExoPlayer supported formats](https://developer.android.com/media/media3/exoplayer/supported-formats)

---

### Pitfall 9: WorkManager Download Queue Duplication and Silent Failures

**What goes wrong:** Downloads are enqueued multiple times (user taps download, nothing visible happens, taps again), or downloads fail silently without user notification, or periodic cleanup work interferes with active downloads.

**Why it happens:** WorkManager requires explicit use of `enqueueUniqueWork()` to prevent duplicates. Without it, each download tap creates a new work request. WorkManager also has a minimum 15-minute interval for periodic work, making it unsuitable for real-time download progress polling. Internal threading with Room (WorkManager uses Room internally) can conflict with the app's own Room usage if not configured carefully.

**Consequences:**
- Duplicate downloads consuming bandwidth and storage
- Users confused by lack of download progress feedback
- Download queue becomes corrupted or stale
- Battery drain from redundant work

**Warning signs:**
- Using `enqueue()` instead of `enqueueUniqueWork()` for downloads
- Polling download progress with periodic WorkManager tasks
- No download status UI (progress, error, retry)
- Not handling network state changes during downloads

**Prevention:**
- Always use `enqueueUniqueWork()` with a unique name per content item (e.g., `"download_${contentId}"`)
- Use `WorkInfo` observed via `Flow` or `LiveData` to show real-time progress
- Implement exponential backoff for retry (`BackoffPolicy.EXPONENTIAL`)
- Set network constraint: `Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED)`
- Provide clear download status UI: queued, downloading (with %), paused, failed (with retry), completed
- For batch downloads: chain work requests or use a single worker that processes a queue from Room

**Phase impact:** Download system architecture phase.

**Confidence:** HIGH -- Based on official WorkManager documentation and common developer pitfalls.

**Sources:**
- [WorkManager deep dive](https://proandroiddev.com/android-workmanager-a-complete-technical-deep-dive-f037c768d87b)
- [WorkManager pitfalls](https://dev.to/kolanse/android-workmanager-implementation-tips-and-pitfalls-to-avoid-4lc9)

---

### Pitfall 10: Search Race Conditions Persist After "Fix"

**What goes wrong:** Search debouncing is added but race conditions still occur because multiple coroutine jobs from different sources return out of order, or cancellation does not propagate to network calls already in flight.

**Why it happens:** OmniStream searches across multiple sources simultaneously. Simple debouncing (delay before firing) does not solve the problem of responses arriving out of order. Source A might respond after Source B even though Source B was triggered by a newer query. Without proper coroutine cancellation, old network requests complete and overwrite newer results.

**Consequences:**
- Search results flicker between queries
- Results from a previous search overwrite current search
- UI shows stale results
- Memory leaks from uncancelled coroutines

**Warning signs:**
- Debounce added but results still flicker on rapid typing
- Using `launch` without cancelling previous job
- No `Job` reference stored for cancellation
- Network calls not using cooperative cancellation

**Prevention:**
- Use `collectLatest` on the search query flow (automatically cancels previous collection)
- Store a `Job` reference for each search and cancel it before starting a new one
- Use `MutableStateFlow` for the search query with `debounce().distinctUntilChanged().flatMapLatest {}`
- Tag each search request with a sequence number; discard responses with outdated sequence numbers
- Ensure OkHttp calls are wrapped in `suspendCancellableCoroutine` with `invokeOnCancellation { call.cancel() }`

**Phase impact:** Bug fix phase. This is a known issue from v1.0 that must be fixed correctly, not just patched.

**Confidence:** HIGH -- This is a well-documented pattern in Kotlin coroutines and the issue is already acknowledged in the project's known issues.

---

## Minor Pitfalls

Mistakes that cause polish issues or minor annoyance.

---

### Pitfall 11: PiP Transition Jank Without sourceRectHint

**What goes wrong:** PiP mode works but the transition animation is ugly -- the window disappears briefly and reappears, or a content overlay flashes during the transition.

**Why it happens:** Since Android 12, the system uses `sourceRectHint` to create smooth PiP enter/exit animations. Without it, the system falls back to a less polished animation. Many tutorials skip this detail.

**Prevention:**
- Always set `sourceRectHint` to the video view's bounds in `PictureInPictureParams.Builder`
- Update `sourceRectHint` when the video view layout changes
- Hide non-video UI elements before entering PiP (controls, title bar, etc.)

**Phase impact:** PiP implementation phase -- minor polish item.

**Confidence:** HIGH -- Official Android documentation.

---

### Pitfall 12: Search History Leaking Sensitive Queries

**What goes wrong:** Search history stores and displays all user queries, including potentially embarrassing or sensitive searches, with no way to clear individual items or disable history.

**Prevention:**
- Provide "clear all history" and "delete individual item" options
- Consider an incognito/private search toggle
- Store search history in Room with timestamps for expiry (auto-clear old entries)
- Never sync search history to the cloud backend

**Phase impact:** Search improvements phase.

**Confidence:** MEDIUM -- General UX best practice rather than technical pitfall.

---

### Pitfall 13: Video Quality Selection Breaks Decryption Chain

**What goes wrong:** Adding quality selection (360p, 720p, 1080p) requires different stream URLs from scraped sources, but each quality level may use a different decryption key or encryption scheme. The existing Vidzee decryption chain assumes a single stream.

**Why it happens:** Scraped video sources often serve different quality levels from different CDN endpoints with different protection. The existing decryption logic in CryptoUtils.kt may be hardcoded to a single stream resolution path.

**Prevention:**
- Audit the Vidzee/Cloudnestra decryption chains for all quality variants before implementing quality selection
- Test each quality level independently with the decryption pipeline
- Design the quality selector to fall back to the working quality if a selected quality fails decryption
- Cache the quality-to-URL mapping per source so switching quality does not require re-scraping

**Phase impact:** Player upgrade phase. Must verify decryption compatibility before exposing quality UI.

**Confidence:** MEDIUM -- Inferred from the project's existing architecture; specific decryption behavior would need code-level verification.

---

## Phase-Specific Warnings

| Phase Topic | Likely Pitfall | Severity | Mitigation |
|---|---|---|---|
| Play Store Submission | Copyright/IP removal (Pitfall 1) | CRITICAL | Resolve distribution strategy before any other work |
| Play Store Submission | Access gate blocks review (Pitfall 3) | CRITICAL | Provide reviewer credentials or remove gate |
| Play Store Submission | Deceptive behavior flagging (Pitfall 4) | CRITICAL | Cannot be mitigated without architectural changes |
| Download System | Enables offline piracy (Pitfall 2) | CRITICAL | Only implement if not targeting Play Store |
| Download System | Foreground service misconfiguration (Pitfall 5) | MODERATE | Use WorkManager, declare service types |
| Download System | Scoped storage issues (Pitfall 6) | MODERATE | Use MediaStore with IS_PENDING pattern |
| Download System | Queue duplication (Pitfall 9) | MODERATE | Use enqueueUniqueWork() |
| PiP Mode | Android 16 breaking change (Pitfall 7) | MODERATE | Use setAutoEnterEnabled, test on Android 16 |
| PiP Mode | Transition jank (Pitfall 11) | MINOR | Set sourceRectHint |
| Subtitle Support | MIME type mismatches (Pitfall 8) | MODERATE | Explicit MIME mapping, use Media3 API |
| Search Improvements | Race conditions persist (Pitfall 10) | MODERATE | Use flatMapLatest, cancel previous jobs |
| Search Improvements | History privacy (Pitfall 12) | MINOR | Provide clear/delete options |
| Player Upgrades | Quality breaks decryption (Pitfall 13) | MODERATE | Audit decryption per quality level |

---

## Strategic Assessment

The dominant finding from this research is that **OmniStream's current architecture is fundamentally incompatible with Google Play Store distribution.** Pitfalls 1, 2, 3, and 4 form an interlocking set of policy violations that cannot be resolved with polish or workarounds:

1. The app scrapes copyrighted content (IP violation)
2. Downloads would enable offline piracy (escalated IP violation)
3. The access gate blocks review (Paywall Restriction)
4. The scraping/decryption is hidden functionality (Deceptive Behavior)

**The project must make a strategic decision before investing in Play Store polish:**

- **Path A: Play Store distribution** -- Strip all scraped sources except MangaDex, remove or heavily modify the access gate, remove decryption chains, pivot to a legal content aggregator. This is essentially a different app.
- **Path B: Sideload distribution** -- Skip Play Store entirely, distribute via GitHub releases or APK hosting. Implement all features freely. Accept that discoverability is limited to word-of-mouth.
- **Path C: Dual-track** -- A clean Play Store version with only legal sources and a separate sideload version with full functionality. Increases maintenance burden but addresses both goals.

The remaining pitfalls (5-13) are standard Android development challenges that are well-understood and preventable with the strategies documented above.

---

## Sources

### Play Store Policy
- [Google Play Developer Policy Center](https://play.google/developer-content-policy/)
- [Intellectual Property Policy](https://support.google.com/googleplay/android-developer/answer/9888072)
- [Deceptive Behavior Policy](https://support.google.com/googleplay/android-developer/answer/9888077)
- [Foreground Service Requirements](https://support.google.com/googleplay/android-developer/answer/13392821)
- [MANAGE_EXTERNAL_STORAGE Policy](https://support.google.com/googleplay/android-developer/answer/10467955)
- [Paywall Restriction Thread](https://support.google.com/googleplay/android-developer/thread/346992873)

### Android Development
- [Android 15 Foreground Service Changes](https://developer.android.com/about/versions/15/behavior-changes-15)
- [PiP Official Documentation](https://developer.android.com/develop/ui/views/picture-in-picture)
- [ExoPlayer Supported Formats](https://developer.android.com/media/media3/exoplayer/supported-formats)
- [Scoped Storage Best Practices](https://developer.android.com/training/data-storage/use-cases)
- [WorkManager Documentation](https://developer.android.com/topic/libraries/architecture/workmanager)

### Community Reports
- [Android 16 PiP Breaking Change](https://medium.com/@khamd0786/migrating-to-android-16-why-your-picture-in-picture-might-break-and-how-to-fix-it-9d9b8bc79567)
- [ExoPlayer Subtitle Replacement Issue](https://github.com/androidx/media/issues/1696)
- [Seekee App Removal](https://www.cbr.com/anime-piracy-seekee-legal-streaming-bury/)
- [WorkManager Pitfalls](https://dev.to/kolanse/android-workmanager-implementation-tips-and-pitfalls-to-avoid-4lc9)
