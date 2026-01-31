# OmniStream Development Log

**Last Updated:** January 31, 2026
**Session:** Phase 8 Complete + Bug Fixes

---

## Jan 31, 2026 - Phase 8 Execution + Post-Execution Bug Fixes

### Phase 8: Foundation, Bug Fixes, and Progress Tracking (COMPLETE)
Executed 4 plans across 2 waves via GSD system.

**What was delivered:**
- Room migration v1 to v2 (adds watch_history, search_history, downloads tables)
- 7 new files: WatchHistoryEntity, SearchHistoryEntity, DownloadEntity + DAOs + WatchHistoryRepository
- Library screen now reads from local Room FavoriteDao (not cloud-only) — BUG-01 FIXED
- Search uses Flow debounce(400).distinctUntilChanged().flatMapLatest{} — BUG-02 FIXED
- Manga reading progress auto-saves every 12s, resumes exact page — BUG-03 FIXED
- Video progress saves on pause/exit with "Resume from X:XX?" dialog — PLAYER-01 FIXED
- Continue Watching/Reading rows on Home screen (Netflix-style) — PLAYER-01

### Post-Execution Bug Fixes (from testing on device)

1. **Continue Watching card showed no image + wrong title** — Player nav route didn't pass title/coverUrl, so watch history saved empty values. Fixed: VideoDetailScreen now passes title+posterUrl to player route.

2. **Player showed "Episode 1234731" instead of movie name** — episodeTitle fell back to extracting number from TMDB ID. Fixed: PlayerViewModel now uses videoTitle (from nav args) as primary display name.

3. **Continue Reading card showed no manga cover** — Same root cause as #1 for reader route. Fixed: MangaDetailScreen now passes title+coverUrl to reader route.

4. **No chapter sort on manga detail page** — With 800+ chapters, scrolling to find current chapter was painful. Fixed: Added ascending/descending sort toggle button next to "Chapters (N)" header.

5. **No "Continue Reading" button on manga detail page** — Had to manually find the chapter. Fixed: Added "Continue Reading - Ch. X" button that appears when saved progress exists.

### Files Modified
- `ui/detail/MangaDetailViewModel.kt` — Added WatchHistoryRepository, loadReadingProgress(), toggleChapterSort(), chaptersAscending state
- `ui/detail/MangaDetailScreen.kt` — Continue Reading button, chapter sort toggle, title+coverUrl in reader nav
- `ui/detail/VideoDetailScreen.kt` — Pass title+posterUrl to player route (both play button and episode list)
- `ui/navigation/OmniNavigation.kt` — Added title+coverUrl args to both reader and player routes
- `ui/player/PlayerViewModel.kt` — Use videoTitle for display, not "Episode {number}"

6. **Manga reader didn't resume at saved page** — `loadCurrentChapterPages` reset `currentPage` to 0 before saved progress check ran. Also `LazyColumn` never scrolled to restored page. Fixed: Added `restoredPage` state, `LaunchedEffect` scrolls to it, `DisposableEffect` saves on exit.

### Build Status: SUCCESSFUL

---

## Jan 29, 2026 (Late Night) - Favorites Persistence, Reader Navigation, API Key Security

### Room Database for Favorites (NEW)
- Created `FavoriteEntity.kt` — Room entity with id, contentId, sourceId, contentType, title, coverUrl, addedAt
- Created `FavoriteDao.kt` — DAO with reactive `isFavorite()` Flow, addFavorite/removeFavorite
- Created `AppDatabase.kt` — Room database class
- Wired into Hilt DI via `AppModule.kt` (provideAppDatabase + provideFavoriteDao)
- **MangaDetailViewModel** — Now observes favorite status reactively, toggleFavorite persists to Room
- **VideoDetailViewModel** — Same treatment, favorites now persist across sessions
- **MangaDetailScreen** + **VideoDetailScreen** — Favorite button now calls viewModel.toggleFavorite()

### Reader Chapter Navigation (FIXED)
- **ReaderViewModel** — Now loads full chapter list from source on init
  - Sorts chapters by number, finds current chapter index
  - `goToPreviousChapter()` and `goToNextChapter()` implemented
  - `hasPreviousChapter`/`hasNextChapter` now computed from actual chapter list
- **ReaderScreen** — Previous/Next chapter buttons now wired up
  - Page counter now updates on scroll via LaunchedEffect + snapshotFlow on listState

### TMDB API Key Security (FIXED)
- Added `TMDB_API_KEY_PRIMARY` and `TMDB_API_KEY_SECONDARY` to BuildConfig via build.gradle.kts
- Keys can be overridden via local.properties (defaults to current values for dev)
- Updated `FlickyStreamSource.kt` — uses `BuildConfig.TMDB_API_KEY_PRIMARY`
- Updated `WatchFlixSource.kt` — uses `BuildConfig.TMDB_API_KEY_PRIMARY`
- Updated `VidSrcSource.kt` — uses `BuildConfig.TMDB_API_KEY_SECONDARY`
- Keys no longer hardcoded in source files

### Files Created
- `data/local/FavoriteEntity.kt`
- `data/local/FavoriteDao.kt`
- `data/local/AppDatabase.kt`

### Files Modified
- `di/AppModule.kt` — Room database + DAO providers
- `ui/detail/MangaDetailViewModel.kt` — FavoriteDao injection, reactive favorite status, toggleFavorite with Room
- `ui/detail/VideoDetailViewModel.kt` — Same as above
- `ui/detail/MangaDetailScreen.kt` — Favorite button wired to viewModel
- `ui/detail/VideoDetailScreen.kt` — Same
- `ui/reader/ReaderViewModel.kt` — Full chapter list loading, prev/next navigation
- `ui/reader/ReaderScreen.kt` — Buttons wired, page counter tracks scroll
- `source/movie/FlickyStreamSource.kt` — BuildConfig for TMDB key
- `source/movie/WatchFlixSource.kt` — BuildConfig for TMDB key
- `source/movie/VidSrcSource.kt` — BuildConfig for TMDB key
- `app/build.gradle.kts` — TMDB API key BuildConfig fields

### Known Issues Remaining
- Library screen still fetches from cloud API only (no local favorites shown)
- No reading progress persistence
- GogoAnime details page crashes (paused)

### Build Status: SUCCESSFUL

---

## Jan 29, 2026 (Night) - Settings, Themes, Bug Fixes, GitHub Cleanup

### Settings Screen (NEW)
- Built full settings screen replacing "Coming Soon" placeholder
- **Account section**: Avatar with initial, username, email, VIP/Standard badge
- **Logout**: Confirmation dialog, clears auth, navigates to login
- **SettingsViewModel**: Handles user data, theme prefs, and logout

### Color Theme System (NEW)
- **8 color schemes**: Purple (default), Ocean, Emerald, Sunset, Rose, Midnight, Crimson, Gold
- Each has both dark and light variants
- **Dark mode toggle**: Dark, Light, or System (follows device setting)
- Theme preference saved to DataStore, persists across restarts
- Color picker with circular swatches + checkmark on selected
- Updated `MainViewModel` to expose theme flows
- Updated `MainActivity` to observe and apply theme dynamically

### GitHub Repository Cleanup
- Made 7 personal repos **private**: happybirthday-asia, happybirthday3, HappyBirthday-website, HappyBirthday, Website-gf, heart, First-Anniversary-of-Love
- All portfolio, fork, and school projects remain public

### Bug Fixes (App Audit)
Ran comprehensive 3-part audit of entire codebase. Fixed critical issues:

1. **Login/Register success never reset** — After logout, returning to login screen would auto-navigate away. Added `onLoginSuccessConsumed()` and `onRegisterSuccessConsumed()` methods.

2. **Library items not clickable** — Grid and list cards had empty `onClick` handlers. Now navigates to correct detail screen (manga or video) using `contentType`, `sourceId`, and `contentId`.

3. **Downloads screen fake data** — Entire screen was hardcoded mock data misleading users. Replaced with honest "Coming Soon" placeholder.

4. **Silent chapter/episode failures** — MangaDetailViewModel and VideoDetailViewModel caught chapter/episode loading errors silently. Now exposes `chaptersError`/`episodesError` in UI state.

5. **No retry on errors** — Added retry buttons to error states in LibraryScreen, MangaDetailScreen, and VideoDetailScreen. Added `retryLoad()` to both detail ViewModels.

6. **Null assertion crash** — LibraryScreen used `error!!` which could crash. Changed to safe `error ?: "Something went wrong"`.

### Files Created
- `ui/settings/SettingsViewModel.kt`

### Files Modified
- `ui/settings/SettingsScreen.kt` — Full rebuild with account, themes, dark mode, about, logout
- `ui/theme/Theme.kt` — 8 color schemes x 2 modes, AppColorScheme enum, DarkModeOption enum
- `ui/MainViewModel.kt` — Added colorScheme + darkMode state flows
- `MainActivity.kt` — Observes theme prefs, passes to OmniStreamTheme
- `data/local/UserPreferences.kt` — Added colorScheme + darkMode keys and setters
- `ui/navigation/OmniNavigation.kt` — Added onLogout callback to settings route
- `ui/auth/LoginViewModel.kt` — Added onLoginSuccessConsumed()
- `ui/auth/LoginScreen.kt` — Reset success flag after consuming
- `ui/auth/RegisterViewModel.kt` — Added onRegisterSuccessConsumed()
- `ui/auth/RegisterScreen.kt` — Reset success flag after consuming
- `ui/library/LibraryScreen.kt` — Fixed navigation, retry button, null safety
- `ui/downloads/DownloadsScreen.kt` — Replaced fake data with Coming Soon
- `ui/detail/MangaDetailViewModel.kt` — chaptersError field, retryLoad()
- `ui/detail/MangaDetailScreen.kt` — Retry button on error
- `ui/detail/VideoDetailViewModel.kt` — episodesError field, retryLoad()
- `ui/detail/VideoDetailScreen.kt` — Retry button on error

### Known Issues (from audit, not yet fixed)
- Favorite button is a TODO (doesn't persist)
- Reader previous/next chapter not implemented
- Reader page counter doesn't update on scroll
- No search timeout handling (could hang)
- TMDB API keys hardcoded in source files (should use BuildConfig)
- Search race condition with rapid queries

---

## Jan 29, 2026 (Evening) - Backend Deployed to Render + MongoDB Atlas

### Deployment

**Backend Live URL:** https://omnistream-api-q2rh.onrender.com
**GitHub Repo:** https://github.com/TamerAli-0/omnistream-api (`.env` NOT exposed, confirmed safe)
**Hosting:** Render (Free tier)
**Database:** MongoDB Atlas (Cluster0, project: OmniStream)

### Setup Completed
1. Pushed `omnistream-api` to GitHub (`TamerAli-0/omnistream-api`)
2. Connected Render to the GitHub repo for auto-deploy
3. MongoDB Atlas cluster created (`Cluster0` on `cluster0.gyrtw6w.mongodb.net`)
4. Database user: `moded977_db_user` (atlasAdmin role, SCRAM auth)
5. IP Whitelist: `0.0.0.0/0` (allow all) — required because Render free tier uses dynamic IPs
6. Updated Android app `ApiService.kt` BASE_URL from `http://10.0.2.2:3000/api` to `https://omnistream-api-q2rh.onrender.com/api`

### Security Notes
- `.env` is in `.gitignore` — credentials are NOT on GitHub
- Render environment variables hold `MONGODB_URI` and `JWT_SECRET`
- MongoDB still requires username + password even with open IP whitelist
- Free tier cold starts may take ~30-50 seconds after inactivity
- For production: upgrade Render for static IPs, restrict Atlas IP whitelist

### Auth System Status: FULLY BUILT
Everything from the previous session is complete and connected to the live backend:

| Component | Status |
|-----------|--------|
| AccessGateScreen (passcode) | Done |
| LoginScreen | Done |
| RegisterScreen | Done |
| All ViewModels | Done |
| UserPreferences (DataStore) | Done |
| AuthRepository | Done |
| ApiService (pointing to Render) | Done |
| Navigation (gate → login → home) | Done |
| Backend API (register/login/me/sync) | Done & Live |
| Rate limiting + brute force protection | Done |
| JWT auth (30-day tokens) | Done |
| bcrypt password hashing (12 rounds) | Done |

### Two-Tier Access System (How It Works)

**Flow:** App Launch → Access Gate (passcode) → Login/Register → Home

**Tier 1 - VIP (Tamer's personal code):**
- Bypasses all future paywalls permanently
- SHA-256 hashed passcode stored in `AuthRepository.kt`

**Tier 2 - Standard (paid users):**
- Requires payment to Tamer, then receives the standard passcode
- Will enforce paywall for premium content in future updates

**After passcode:** Users create an account (email + username + password) saved to MongoDB. This account syncs across devices and will sync with the future website.

### What's Next
- Test the full auth flow on a real device (gate → register → login → home)
- Build the OmniStream website (same backend, same accounts, cross-device sync)
- Implement paywall enforcement for Standard tier users
- Add logout and account settings

---

## Jan 29, 2026 - Two-Tier Access System + Backend + Sync

### What Was Built

**1. Two-Tier Access Gate**
- App now requires an access password on first launch
- **VIP tier** — full free access, bypasses future paywall
- **Standard tier** — normal access, paywall enforcement planned for later
- Both passwords unlock the app; tier is stored locally and on the server

**2. Backend API (Node.js + Express + MongoDB Atlas)**
- Location: `C:\Users\black\Desktop\omnistream-api`
- User registration and login with bcrypt password hashing
- JWT-based authentication (30-day tokens)
- Cross-device sync endpoints (library + history)
- Express-validator input validation
- Endpoints: POST /register, POST /login, GET /me, GET /sync, PUT /sync

**3. Android Auth Flow**
- `AccessGateScreen` — password entry, validates VIP or Standard
- `LoginScreen` — email/password login
- `RegisterScreen` — email/username/password/confirm registration
- All screens use Hilt ViewModels and DataStore persistence
- Token persists across app restarts (skip login on relaunch)

**4. Navigation Rewiring**
- Dynamic start destination based on unlock + auth state
- Flow: Launch -> AccessGate (if locked) -> Login (if no token) -> Home
- Auth routes excluded from bottom nav

**5. Library Sync Integration**
- LibraryScreen now backed by LibraryViewModel fetching real sync data
- Categories populated from server: Favorites, Reading, Plan to Read, Completed, On Hold
- Refresh button to re-fetch sync data

### Files Created

**Backend (`omnistream-api/`):**
- `package.json`, `.env`, `.gitignore`, `server.js`
- `models/User.js`, `models/SyncData.js`
- `middleware/auth.js`
- `routes/auth.js`, `routes/sync.js`

**Android Data Layer:**
- `data/local/UserPreferences.kt`
- `data/remote/ApiService.kt`
- `data/remote/dto/AuthDtos.kt`, `data/remote/dto/SyncDtos.kt`
- `data/repository/AuthRepository.kt`, `data/repository/SyncRepository.kt`

**Android UI:**
- `ui/auth/AccessGateScreen.kt`, `ui/auth/AccessGateViewModel.kt`
- `ui/auth/LoginScreen.kt`, `ui/auth/LoginViewModel.kt`
- `ui/auth/RegisterScreen.kt`, `ui/auth/RegisterViewModel.kt`
- `ui/MainViewModel.kt`
- `ui/library/LibraryViewModel.kt`

**Files Modified:**
- `di/AppModule.kt` — added providers for UserPreferences, ApiService, AuthRepository, SyncRepository
- `MainActivity.kt` — added MainViewModel for dynamic start destination
- `ui/navigation/OmniNavigation.kt` — added access_gate, login, register routes
- `ui/library/LibraryScreen.kt` — replaced mock data with ViewModel + API sync

### Paid Version Plans
- Standard tier users will see a paywall for premium content in a future update
- VIP tier users bypass all paywalls permanently
- Tier is stored both locally (DataStore) and server-side (MongoDB)

### OmniStream Web Plans
- The backend API is designed to support a future web client
- Same auth endpoints and sync data format will work for web
- CORS is enabled on the API for cross-origin web requests

### How to Run the Backend
```bash
cd C:\Users\black\Desktop\omnistream-api
npm install
# Update .env with your MongoDB Atlas URI and JWT secret
npm start
```

### How to Test
```bash
# Register
curl -X POST http://localhost:3000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","username":"testuser","password":"123456","tier":"vip"}'

# Login
curl -X POST http://localhost:3000/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@test.com","password":"123456"}'

# Get profile (use token from login response)
curl http://localhost:3000/api/auth/me \
  -H "Authorization: Bearer YOUR_TOKEN"

# Sync
curl http://localhost:3000/api/sync \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

**Previous Session:** WatchFlix source (vidsrc-embed/cloudnestra chain) - DECRYPT MISSION

---

## 🛠️ Scraping Setup (Desktop)

We have a **scraping tutorial setup** on the desktop for reverse engineering video sources:

### Tools Available
- **Modded Firefox** - DevTools with extended capabilities
  - Network tab for capturing all requests
  - Can be further modded to intercept encrypted/decrypted data
- **Screenshots folder:** `C:/Users/black/Pictures/Screenshots/`

### Firefox Mods to Consider
1. **Disable CSP** - Content Security Policy can block our interception
2. **Log decrypted data** - Hook into JS crypto functions
3. **Export HAR** - Full network capture for offline analysis
4. **Console overrides** - Inject code to log encryption keys

### Current Analysis Method
1. Open target site in modded Firefox
2. Network tab → Filter by specific domains (e.g., `quibblezoomfable.com/pl`)
3. Capture the request chain
4. Screenshot headers, URLs, and response bodies
5. Identify encryption/encoding patterns

---

## Current Goal
Get 3 working sources:
1. **Manga:** MangaDex ✅
2. **Anime:** AnimeKai (WebView) ✅ / GogoAnime 🔧 PAUSED
3. **Movies:** FlickyStream ✅ + WatchFlix ✅ **NEW!**

---

## 🔴 Jan 29, 2026 - Cloudnestra Decryption Challenge

### The Problem
The vidsrc-embed → cloudnestra chain uses **JavaScript-based URL generation**.

**Network Tab Analysis (Screenshots 2026-01-29 024908-025053):**

| Request | Domain | File | Status | Notes |
|---------|--------|------|--------|-------|
| POST | cloudnestra.com | rum | 404 | Tracking/verification |
| POST | vidsrc-embed.ru | rum | 204 | Tracking/verification |
| GET | **tmstr3.quibblezoomfable.com** | master.m3u8 | **200** | ✅ THE VIDEO! |
| GET | tmstr3.quibblezoomfable.com | index.m3u8 | 200 | Playlist |
| GET | tmstr3.cloudnestra.com | rt_ping.php | 200 | Ping |

### The M3U8 URL Pattern
```
https://tmstr3.quibblezoomfable.com/pl/H4sIAAAAAAAAAwXB0ZKCIBA0F.../master.m3u8
```

The path `/pl/{encoded}` contains **gzip+base64 encoded data** (starts with `H4sI`).

### Headers Required
```
Origin: https://cloudnestra.com
Referer: https://cloudnestra.com/
Host: tmstr3.quibblezoomfable.com
```

### The Challenge
The encoded path is generated by JavaScript (`pjs_main_drv_cast.261225.js`).

**Without JS execution, we can't generate the path ourselves.**

### What We Need to Find
1. **API endpoint** that returns the m3u8 URL directly (bypassing JS)
2. **The encoding algorithm** used to generate the path
3. **Alternative embed source** that doesn't use JS-based encoding

### Current Workarounds Tried
- vidsrc-embed.ru/embed/{type}/{id}
- 2embed.cc
- multiembed.mov
- vidsrc.pro
- superembed.stream
- autoembed.cc
- All require JS or have similar encoding

### Next Steps
1. Use modded Firefox to intercept the JS decryption
2. Find what data is encoded in the path (probably video hash + auth)
3. Look for server-side API that generates the path

---

## 🎉 Jan 29, 2026 - WatchFlix Source Added!

### Site Info
**URL:** https://watchflix.to
**Provider chain:** vidsrc-embed.ru → cloudnestra.com → CDN

### Key Discovery: NO ENCRYPTION NEEDED!
Unlike FlickyStream (which required AES decryption), WatchFlix uses a simple 3-layer HTML parsing chain.

### Extraction Chain (FULLY WORKING)

```
Step 1: GET https://vidsrc-embed.ru/embed/{movie|tv}?tmdb={id}[&season={s}&episode={e}]
        Referer: https://watchflix.to/
        → Parse HTML, extract hash from iframe src (//cloudnestra.com/rcp/{hash1})

Step 2: GET https://cloudnestra.com/rcp/{hash1}
        Referer: https://vidsrc-embed.ru/
        → Parse HTML, find '/prorcp/{hash2}' in loadIframe() function

Step 3: GET https://cloudnestra.com/prorcp/{hash2}
        Referer: https://cloudnestra.com/
        → Parse HTML, extract "file:" URL from Playerjs initialization

Step 4: Replace CDN placeholders in m3u8 URL
        {v1}, {v2}, {v3}, {v4}, {v5} → quibblezoomfable.com

Step 5: Play m3u8
        Origin: https://cloudnestra.com
        Referer: https://cloudnestra.com/
```

### URL Patterns
- **Movies:** `vidsrc-embed.ru/embed/movie?tmdb={tmdb_id}`
- **TV Shows:** `vidsrc-embed.ru/embed/tv?tmdb={tmdb_id}&season={s}&episode={e}`

### CDN Domain
- M3u8 URLs contain placeholders like `https://tmstr2.{v1}/pl/{base64}/master.m3u8`
- Replace `{v1}` through `{v5}` with `quibblezoomfable.com`

### Files Created/Modified
- **NEW:** `source/movie/WatchFlixSource.kt` - Full implementation
- **Modified:** `source/SourceManager.kt` - Registered WatchFlixSource, paused GogoAnimeSource

### GogoAnime Status: PAUSED
GogoAnime work is saved but commented out. Will revisit later after WatchFlix testing.
AnimeKai (WebView approach) remains active as the primary anime source.

---

## 🔧 Jan 28, 2026 - GogoAnime Source (Replacing AnimeKai)

### Why Switch?
AnimeKai encryption was too complex (multi-layer obfuscation, dynamic keys, external decryption services). Switching to GogoAnime which has simpler HTML structure.

**URL:** https://gogoanime.by/

### Current Status
| Feature | Status |
|---------|--------|
| Homepage | ✅ Working |
| Search | ⚠️ Not tested |
| Anime Details | ❌ Crashes app |
| Episode List | ❌ Not tested |
| Video Playback | ❌ Not tested |

### Bug Fixed (Homepage)
**Issue:** URLs end with trailing slash like `/arne-no-jikenbo-episode-4-english-subbed/`
**Problem:** `substringAfterLast("/")` returned empty string
**Fix:** Added `href.trimEnd('/')` before extracting anime ID

### Homepage HTML Structure (VERIFIED WORKING)
```html
div.excstf (parent container)
  article.bs
    div.bsx
      a.tip [href, oldtitle="Full Title"]
        div.limit
          div.typez (TV Show/Anime/Movie)
          img.ts-post-image [src]
        div.ttt
          div.tt (short title + h2 full title)
```

### TODO - Need Firefox DevTools
- [ ] Anime details page structure (crashes when clicked)
- [ ] Episode list selector
- [ ] Video server/link extraction

### Current Selectors (MAY BE WRONG - needs verification)
```kotlin
// Details page
title: "div.infolimit h2"
poster: "div.thumb img.ts-post-image"
info: "div.spe span"

// Episodes
container: "div.episodes-container div.episode-item[data-episode-number]"

// Video servers
servers: "div.servers li.player-type-link"
plain URL: data-plain-url attribute
encrypted URL: data-encrypted-url1/2/3 attributes
```

### Files Modified
- `GogoAnimeSource.kt` - Updated all selectors for gogoanime.by structure
- `SourceManager.kt` - Registered GogoAnime, kept AnimeKai as backup

---

## AnimeKai - PAUSED (Too Complex)

**URL:** https://anikai.to/

**Why abandoned:**
- Multi-layer encryption impossible to crack without external services
- `window.__$` token for AJAX requests
- Encrypted AJAX responses
- Obfuscated JS (14,000+ function calls)
- CloudStream uses external decryption APIs we don't have access to

**WebView approach built but unreliable.** May revisit if we find decryption keys.

---

---

## 🎉 Jan 28, 2026 - COMPLETE DECRYPTION CHAIN CRACKED!

### The Full Working Solution

**Step 1: Fetch the decryption key**
```
GET https://core.vidzee.wtf/api-key
Response: qqzBsLmdbM4IWfdZDRD+j5AzvhoU3GOZ714++QrcgqIK36cw6R6GwnwXulB6qHP4
```

**Step 2: Decrypt the API key (AES-256-GCM)**
```
Secret: "b3f2a9d4c6e1f8a7b"
Algorithm: AES-256-GCM with SHA-256 key derivation
Result: "ifyouscrapeyouaregay" 😂
```

**Step 3: Call the video API**
```
GET https://player.vidzee.wtf/api/server?id={tmdb_id}&sr=2
Headers:
  Origin: https://player.vidzee.wtf
  Referer: https://player.vidzee.wtf/?id={tmdb_id}
```

**Step 4: Decrypt the video link (AES-256-CBC)**
```
Input: response.url[0].link (encrypted base64 string)
Key: "ifyouscrapeyouaregay" (padded to 32 bytes with null chars)
Format: base64(base64(IV):base64(ciphertext))
Algorithm: AES-256-CBC with PKCS7 padding
Result: https://streams.smashystream.top/proxy/m3u8/...
```

**Step 5: Play the video**
```
Headers for playback:
  Origin: https://player.vidzee.wtf
  Referer: https://player.vidzee.wtf/
```

### Code Implementation

Updated files:
- `CryptoUtils.kt` - Added `vidzeeDecryptApiKey()` and `vidzeeDecryptLink()` functions
- `FlickyStreamSource.kt` - Added `getVidzeeKey()` and updated `getLinks()` to use proper decryption
- `PlayerScreen.kt` - Fixed player to use HlsMediaSource with custom headers (Referer/Origin)

### Player Fix (Jan 28, 2026)

**Problem:** ExoPlayer was treating HLS as progressive download, causing `UnrecognizedInputFormatException`

**Solution:** Updated PlayerScreen.kt to:
1. Use `HlsMediaSource.Factory()` for m3u8 streams
2. Set custom HTTP headers via `DefaultHttpDataSource.Factory()`:
   - `Referer: https://player.vidzee.wtf/`
   - `Origin: https://player.vidzee.wtf`

### API Timeout Fix (Jan 28, 2026)

**Problem:** Vidzee API was timing out on Android (worked in browser)

**Solution:** Added browser-like security headers to bypass server-side detection:
```kotlin
headers = mapOf(
    "sec-ch-ua" to "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\"",
    "sec-ch-ua-mobile" to "?0",
    "sec-ch-ua-platform" to "\"Windows\"",
    "sec-fetch-dest" to "empty",
    "sec-fetch-mode" to "cors",
    "sec-fetch-site" to "same-origin"
)
```

## 🎉 FLICKYSTREAM FULLY WORKING! (Jan 28, 2026)

### How We Found It

1. Used Firefox DevTools Network tab to capture API responses
2. Found encrypted `link` field in `/api/server` response
3. Used WebFetch to download vidzee.wtf JavaScript bundles
4. Found decryption functions `en()` (for API key) and `ei()` (for links)
5. Found secret `"b3f2a9d4c6e1f8a7b"` in module X._
6. Traced key fetch from `https://core.vidzee.wtf/api-key`
7. Verified decryption works - output matches browser network capture!

---

## Latest Update (Jan 28, 2026 - BREAKTHROUGH!)
**FOUND THE CORRECT API ENDPOINT!**

**Wrong:** `player.vidzee.wtf/server?id=X&sr=X` (returns 404)
**Correct:** `player.vidzee.wtf/api/server?id=X&sr=X` (returns m3u8 URL!)

**Full working chain:**
1. Call: `https://player.vidzee.wtf/api/server?id={tmdb_id}&sr={0,1,2}`
2. Response contains: `https://streams.smashystream.top/proxy/m3u8/{encoded_source}/{encoded_headers}`
3. Play with Referer: `https://rapidairmax.site/`

**Headers needed for API call:**
- Referer: `https://player.vidzee.wtf/?id={tmdb_id}`
- Origin: `https://player.vidzee.wtf`

**Headers needed for video playback:**
- Referer: `https://rapidairmax.site/`

---

## Status Summary

| Source | Home Page | Details | Episodes/Chapters | Playback |
|--------|-----------|---------|-------------------|----------|
| MangaDex | ✅ Works | ✅ Works | ✅ Works | ⚠️ Some chapters empty |
| AnimeKai | ✅ Works (30 items) | ✅ Works | ❌ 403 Error | ❌ Not tested |
| FlickyStream | ✅ Works (TMDB proxy) | ✅ Works | ✅ Works (movies=1 ep) | ✅ **WORKING!** |

---

## Detailed Issues

### AnimeKai (anikai.to)
**Problem:** Episodes endpoint returns 403 Forbidden
```
AJAX response: {"status":403,"result":null,"message":"Unable to read the request."}
```

**What works:**
- Home page loads 30 anime per section
- Clicking anime loads title, poster, details correctly
- URL pattern: `https://anikai.to/watch/{slug}`

**What's broken:**
- The AJAX endpoint for episodes is being rejected
- We tried: `/ajax/episodes/list?ani_id=X` - returns 403
- The `ani_id` selector was finding "signin" (login button) instead of actual ID
- Fixed selector but still getting 403 - likely needs special headers or tokens

**Next step:**
Open browser DevTools → Network tab → click an anime → find the XHR request that loads episodes → copy the URL and headers

---

### FlickyStream (flickystream.ru)
**BREAKTHROUGH FOUND!**

**URL Pattern discovered:**
- Movie page: `https://flickystream.ru/movie/{tmdb_id}` (JS rendered, can't scrape)
- Player page: `https://flickystream.ru/player/movie/{tmdb_id}` ← **THIS WORKS!**
- Video CDN: `https://khdiamondcdn.asia/gs/{hash}?=.mp4`

**Example (Anaconda - TMDB ID: 1234731):**

**FULL CHAIN DISCOVERED:**
```
FlickyStream → vidzee.wtf (embed) → smashystream.top (HLS streams)
```

**Key endpoints:**
- `https://player.vidzee.wtf/server?id={tmdb_id}&sr=0` (Server 0)
- `https://player.vidzee.wtf/server?id={tmdb_id}&sr=1` (Server 1)
- `https://player.vidzee.wtf/server?id={tmdb_id}&sr=2` (Server 2)
- Streams from: `streams.smashystream.top`
- **Required Referer:** `https://rapidairmax.site/`

**Other domains in chain:**
- `mid.vidzee.wtf` - TMDB proxy for images/metadata
- `core.vidzee.wtf` - API key stuff
- `api.themoviedb.org` - Direct TMDB calls
- `s.vdrk.site` - Subtitles (e.g., `English Hi2.vtt`)

**Current status:**
- ✅ Video playback working via vidzee.wtf endpoints!
- ✅ Home page working via TMDB proxy!
- ✅ Search working via TMDB proxy!

## FULLY WORKING!

**TMDB Proxy discovered:**
```
https://mid.vidzee.wtf/tmdb/{endpoint}?api_key=297f1b91919bae59d50ed815f8d2e14c&language=en-US&page=1
```

**Available endpoints:**
- `trending/movie/day` - Trending movies
- `movie/popular` - Popular movies
- `movie/top_rated` - Top rated
- `movie/upcoming` - Coming soon
- `tv/popular` - Popular TV shows
- `search/movie?query=X` - Search movies
- `search/tv?query=X` - Search TV shows

---

### MangaDex
**Status:** Mostly working

**Issue:** Some chapters return empty data:
```
Response: {"result":"ok","chapter":{"hash":"","data":[],"dataSaver":[]}}
```

**Explanation:** This is a MangaDex issue, not our bug. Some chapters are:
- External chapters (hosted on scanlator sites)
- Removed/unavailable chapters
- Official chapters that require different access

**No action needed** - this is expected behavior for some chapters.

---

## Files Modified This Session

1. **`VideoDetailViewModel.kt`**
   - Fixed URL construction for AnimeKai (`/watch/{id}`)
   - Fixed URL construction for FlickyStream (`/movie/{id}`)

2. **`AnimeKaiSource.kt`**
   - Improved `ani_id` extraction (avoids "signin" false positive)
   - Added multiple AJAX endpoint patterns to try
   - Added better logging

3. **`FlickyStreamSource.kt`**
   - Added API endpoint detection
   - Added `__NEXT_DATA__` parsing for Next.js sites
   - Added React Server Components detection
   - Better logging

4. **`MangaDexSource.kt`**
   - Added detailed logging for debugging page loading

5. **`SourceManager.kt`**
   - Added import for GoojaraSource (backup movie source if needed)

---

## How To Continue

### Step 1: Get AnimeKai Working
1. Open https://anikai.to in Chrome/Firefox
2. Press F12 → Network tab → filter by "XHR" or "Fetch"
3. Click on any anime
4. Find the request that loads episode list
5. Right-click → "Copy as cURL" or note:
   - Full URL
   - Request headers
   - Response format (JSON or HTML)

### Step 2: Get FlickyStream Working
1. Open https://flickystream.ru
2. Same process - Network tab, XHR filter
3. Look for requests returning movie JSON data
4. Share URL pattern and response

### Step 3: Update Code
Once you have the network info, I can update:
- Correct AJAX endpoints
- Required headers (tokens, cookies, etc.)
- Response parsing

---

## CloudStream Reference
AnimeKai has a working CloudStream provider at:
`https://github.com/phisher98/cloudstream-extensions-phisher/tree/master/AnimeKai`

Key findings from their code:
- They use encryption/decryption for video links
- External decryption services (`BuildConfig.KAIENC`, `BuildConfig.KAIDEC`)
- AJAX endpoints: `/ajax/episodes/list`, `/ajax/links/list`, `/ajax/links/view`

Without access to decryption services, video playback may not work even if we get episodes loading.

---

## Alternative Options If Sites Don't Work

| Type | Alternative | Status |
|------|-------------|--------|
| Movies | Goojara (ww1.goojara.to) | Code exists, not registered |
| Anime | HiAnime, GogoAnime | Not implemented |
| Manga | MangaDex | Working |

---

## Commands To Build & Test
```bash
# In Android Studio or terminal
./gradlew assembleDebug

# Install on device
adb install -r app/build/outputs/apk/debug/app-debug.apk

# View logs
adb logcat | grep -E "(AnimeKai|FlickyStream|MangaDex|VideoDetail|Reader)"
```

## What To Check In Logs (FlickyStream Video)
When clicking play on a movie, look for these log messages:
```
FlickyStream: Getting links for TMDB ID: XXXXX
FlickyStream: Trying vidzee embed: ...
FlickyStream: Vidzee embed response: ...
FlickyStream: Trying player.vidzee: ...
FlickyStream: Trying vidsrc.to: ...
FlickyStream: Found X links total
```

If you see `Found 0 links total`, check:
1. Which embed providers returned errors vs empty responses
2. If any returned HTML with iframes, we need to follow those
3. If response contains obfuscated JS, we need to decode it

**Key insight:** Many embed providers use JavaScript to decode video URLs client-side.
If an embed returns JS code instead of direct m3u8/mp4 URLs, we may need to:
- Find and call their API endpoints directly
- Or use a WebView approach to let JS execute

---

## Contact/Notes
- User prefers: AnimeKai and FlickyStream specifically (no alternatives unless necessary)
- User can help with DevTools inspection
- MangaDex is the manga source (working)

---

## Screenshots Location
`C:/Users/black/Pictures/Screenshots/`

**Recent debugging screenshots (Jan 28, 2026):**
- `Screenshot 2026-01-28 001923.png` - Network tab showing vidzee API responses
- `Screenshot 2026-01-28 000946.png` - Earlier network capture

---

## Jan 28 2026 - Debugging Session

### What the screenshot shows:
- Filter: `/api/server`
- 4 requests to `player.vidzee.wtf`:
  - `sr=0` → 404 (59 B)
  - `sr=1` → 404 (58 B)
  - `sr=2` → **200 OK (2.83 KB)** ← This works!
  - `sr=2` → 304 cached

**Response JSON structure:**
```json
{
  "headers": { "User-Agent": "..." },
  "provider": "Glory",
  "servers": [],
  "url": [...],           // ← EXPAND THIS - contains encrypted links
  "tracks": [{...}, ...], // ← Subtitles?
  "proxy": false,
  "thumbnail": null,
  "serverInfo": { "number": 2, "name": "Glory", "flag": "US" }
}
```

### BREAKTHROUGH - Correct Headers Found!

**Screenshots analyzed:**
- `Screenshot 2026-01-28 002646.png` - Shows 3 requests to smashystream
- `Screenshot 2026-01-28 002659.png` - Master playlist request (924 B)
- `Screenshot 2026-01-28 002705.png` - Video segments request (693 KB)

**The browser sends these headers to smashystream:**
```
Origin: https://player.vidzee.wtf
Referer: https://player.vidzee.wtf/
Host: streams.smashystream.top
```

**NOT rapidairmax.site!** We had the wrong referer.

**The URL structure:**
```
https://streams.smashystream.top/proxy/m3u8/{encoded_source_url}/{encoded_headers_for_source}
```

Where:
- `encoded_source_url` = URL-encoded elite39zone.site m3u8 URL
- `encoded_headers_for_source` = `{"referer":"https://rapidairmax.site/","origin":"https://rapidairmax.site"}` (used by proxy to fetch from source)

**SOLVED!** ✅
The API returns encrypted links (base64 + AES). We reverse-engineered the decryption:
1. API key from `https://core.vidzee.wtf/api-key` decrypted with AES-GCM using secret `"b3f2a9d4c6e1f8a7b"`
2. Video links decrypted with AES-CBC using the decrypted API key (`"ifyouscrapeyouaregay"`)
3. Implementation added to `CryptoUtils.kt` and `FlickyStreamSource.kt`

**Method used:** Modded Firefox with Network DevTools to capture real API calls and reverse engineer encryption.

---

## 🔧 Jan 28, 2026 - AnimeKai (anikai.to) Investigation

**Starting approach:** Using same modded Firefox method that cracked FlickyStream.

### CloudStream Reference Analysis
The CloudStream AnimeKai provider uses 4 external decryption services:
- `KAIENC` - encode text for AJAX `_=` parameter
- `KAIDEC` - decode responses
- `KAIMEG` - decrypt video links
- `KAISVA` - fallback service

These are private URLs in `local.properties`. We'll reverse-engineer the encryption ourselves.

### What To Capture in Modded Firefox

1. **Go to:** https://anikai.to
2. **Open Network Tab** → Filter by "XHR" or "Fetch"
3. **Click any anime** (e.g., Solo Leveling)
4. **Capture these requests:**
   - Any `/ajax/episodes/` requests
   - Any `/ajax/links/` requests
   - Look for the `_=` parameter value

5. **For video playback:**
   - Click play on an episode
   - Capture any `/e/` or `/media/` requests
   - Find the encrypted response and decrypted m3u8 URL

### Key things to screenshot/note:
- Full request URL with all parameters
- Request headers (especially cookies, tokens)
- Response body (the JSON/HTML returned)
- Any JavaScript that processes the response

---

## 🎉 Jan 28, 2026 - AnimeKai WebView Solution Implemented!

### The Problem
AnimeKai uses complex multi-layer encryption:
1. `window.__$` token on page → generates `_=` parameter for AJAX requests
2. AJAX responses are encrypted
3. Video embed URLs (4spromax.site) also have encrypted responses
4. Final m3u8 served from `rrr.code29wave.site`

The JavaScript is heavily obfuscated (14,000+ obfuscated function calls).

### The Solution: WebView Extraction
Instead of reverse-engineering the encryption, we use a WebView approach:
1. Load the episode page in a hidden WebView
2. Let the JavaScript execute naturally (it handles all decryption)
3. Intercept network requests to capture the m3u8 URL
4. Return the URL with proper headers for playback

### Files Created/Modified

**New file:** `core/webview/WebViewExtractor.kt`
- Generic WebView-based video URL extractor
- Intercepts network requests matching target patterns
- Returns video URL with referer and headers

**Modified:** `source/anime/AnimeKaiSource.kt`
- Now uses `WebViewExtractor` for `getLinks()`
- Takes `Context` parameter for WebView creation
- Targets `code29wave.site` and `.m3u8` patterns

**Modified:** `source/SourceManager.kt`
- Passes `context` to `AnimeKaiSource`

### Video Chain (discovered via Firefox DevTools)
```
AnimeKai page → 4spromax.site (embed) → rrr.code29wave.site (m3u8 CDN)
```

### Headers for Playback
```
Referer: https://4spromax.site/
Origin: https://4spromax.site
```

### Testing
Build and install:
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```
