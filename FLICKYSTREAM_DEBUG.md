# Flickystream Debugging Guide

## Problem
Videos from flickystream source take too long to load and show 0:00 duration with no playback.

## Diagnosis Steps

### 1. Check Logcat for Link Extraction
When you try to play a flickystream video, look for these log tags in logcat:
```
FlickyStream
VideoPlayer
PlayerViewModel
```

### 2. Look for These Key Messages
- `FlickyStream: Getting links for TMDB ID: ...` - Shows extraction started
- `FlickyStream: Using vidzee decryption key: ...` - Vidzee API key fetched
- `FlickyStream: Decrypted URL: ...` - Actual video URL extracted
- `VideoPlayer: Loading URL: ...` - URL being loaded in player
- `VideoPlayer: Request headers: ...` - Headers being sent

### 3. Common Failure Patterns

**Pattern 1: No links found**
```
FlickyStream: Found 0 links total
```
→ All extraction methods failed. Vidzee API might be down or changed.

**Pattern 2: Links found but player shows 0:00**
```
VideoPlayer: Loading URL: https://...
VideoPlayer: Playback state: 3 (READY but duration=0)
```
→ URL is valid but returns empty/invalid video stream

**Pattern 3: Smashystream proxy URL not parsed**
```
FlickyStream: Detected smashystream proxy URL, extracting source...
FlickyStream: lastJsonStart position: -1
```
→ parseStreamUrl failed to extract actual source from proxy

### 4. Quick Test
Try a known working movie (e.g., TMDB ID 550 - Fight Club):
1. Search "Fight Club" in the app
2. Select the movie
3. Try to play
4. Check logcat for the extraction logs

### 5. Expected Working Flow
```
PlayerViewModel: Loading video links: sourceId=flickystream, videoId=movie-550
FlickyStream: Getting links for TMDB ID: 550, isMovie: true
FlickyStream: Using vidzee decryption key: [key]
FlickyStream: Trying vidzee API server 2
FlickyStream: Decrypted URL: https://tmstr3.quibblezoomfable.com/pl/.../master.m3u8
VideoPlayer: Loading URL: https://tmstr3.quibblezoomfable.com/pl/.../master.m3u8
VideoPlayer: Request headers: {Referer=https://cloudnestra.com/, Origin=https://cloudnestra.com}
VideoPlayer: Playback state: 3 (READY, duration > 0)
```

## Possible Fixes

### Fix 1: Vidzee API Changed
If logs show "Server X returned error", the Vidzee API format may have changed.

### Fix 2: URLs Expire Quickly
If URLs are extracted but fail when loaded, they might have short expiration times.

###  Fix 3: CDN Blocked
If quibblezoomfable.com URLs fail, the CDN might be blocking requests.

## Next Steps
Send me the logcat output when trying to play a flickystream video and I'll identify the exact issue.
