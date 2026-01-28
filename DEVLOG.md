# OmniStream Development Log

**Last Updated:** January 28, 2026
**Session:** GogoAnime replacing AnimeKai

---

## Current Goal
Get 3 working sources:
1. **Manga:** MangaDex ✅
2. **Anime:** GogoAnime (gogoanime.by) 🔧 IN PROGRESS
3. **Movies:** FlickyStream (flickystream.ru) ✅ **SOLVED!**

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
