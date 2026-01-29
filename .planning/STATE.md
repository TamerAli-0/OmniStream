# State: OmniStream

## Project Reference

See: .planning/PROJECT.md (updated 2026-01-29)

**Core value:** Users can discover, stream, and read content from multiple sources in one app with a seamless, crash-free experience.
**Current focus:** Not started (defining requirements)

## Current Position

Phase: Not started (defining requirements)
Plan: --
Status: Defining requirements
Last activity: 2026-01-29 -- Milestone v2.0 started

## Accumulated Context

### Decisions
- WebView approach for AnimeKai (encryption too complex)
- Vidzee AES decryption cracked and implemented in CryptoUtils.kt
- WatchFlix uses simple 3-layer HTML parsing (no encryption)
- TMDB keys moved to BuildConfig for security
- Room database chosen for favorites (replaces cloud-only approach)

### Known Issues
- Library screen only shows cloud sync data, not local Room favorites
- No reading progress persistence (chapter position lost on exit)
- GogoAnime details page crashes (paused, selectors may be wrong)
- Search race condition with rapid typing (no debounce)
- No search timeout handling
- Downloads screen is placeholder

### Technical Notes
- Backend: https://omnistream-api-q2rh.onrender.com (Render free tier)
- MongoDB Atlas: cluster0.gyrtw6w.mongodb.net
- Test device: Infinix Note 30
- Vidzee API key secret: "b3f2a9d4c6e1f8a7b" (AES-256-GCM)
- Vidzee decrypted key: used for AES-256-CBC link decryption

---
*Last updated: 2026-01-29*
