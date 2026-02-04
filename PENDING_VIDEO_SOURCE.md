# 🎬 PENDING: Rive Video Source Implementation

## What I Need From You (When You Return):

### 1. **Firefox DevTools Inspection of Rive**

**Site:** https://rivestream.org/

**What to capture:**
1. Open Rive → Play any movie
2. F12 → Network tab
3. **Filter by:** `m3u8` OR `hakunamatata` (the CDN)
4. Look for **LARGE files** (500KB+, MB+)
5. Screenshot the **video request**, NOT subtitle/ads

**I need:**
- ✅ Video URL pattern (e.g., `https://cacdn.hakunamatata.com/video/{id}/master.m3u8`)
- ✅ Request headers (Referer, Origin, etc.)
- ✅ Any API endpoint pattern (e.g., `/api/stream?id=...`)

### 2. **What We Found So Far:**

✅ **Subtitle CDN:** `https://cacdn.hakunamatata.com/subtitle/{hash}.srt`
❌ **Video CDN:** Still need to capture this!

### 3. **Status:**

- **RiveStreamSource.kt** - Already created, needs video extraction logic
- **Consumet API** - Backup option if Rive fails
- **Current blocker:** Need actual video URL pattern from Rive

---

## When You Ask Me About This:

Show me the Firefox screenshot with the **video request** (big file, .m3u8 or .mp4), and I'll implement it in 10 minutes!

**Last worked on:** 2026-02-04
**File created:** `app/src/main/java/com/omnistream/source/movie/RiveStreamSource.kt`
