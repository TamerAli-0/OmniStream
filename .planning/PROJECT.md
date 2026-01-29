# OmniStream

## What This Is

A unified Android app for streaming and reading manga, anime, and movies — combining the best of Kotatsu (manga) and CloudStream (video) into a single polished experience. Built for personal use and public distribution via Play Store. Targets Android with Kotlin, Jetpack Compose, and Material 3.

## Core Value

Users can discover, stream, and read content from multiple sources in one app with a seamless, crash-free experience.

## Requirements

### Validated

<!-- Shipped and confirmed valuable. -->

- Multi-source content aggregation (MangaDex, AnimeKai, FlickyStream, WatchFlix, AsuraComics, MangaKakalot, ManhuaPlus, VidSrc, Goojara)
- Manga reader with vertical scroll and chapter navigation
- Video player with HLS/DASH support and AES decryption chains (Vidzee, Cloudnestra)
- Two-tier access gate (VIP + Standard passcodes with SHA-256)
- Auth system (register/login/JWT) with Node.js backend on Render + MongoDB Atlas
- Room database for favorites persistence
- Settings with 8 color themes + dark/light/system mode
- Library with cloud sync categories (Favorites, Reading, Plan to Read, Completed, On Hold)
- Unified search across sources
- Home screen with trending/popular content per source
- Browse screen for per-source catalog browsing
- TMDB API key security via BuildConfig

### Active

<!-- Current scope. Building toward these. -->

- [ ] Fix library screen to show local favorites (not just cloud sync)
- [ ] Reading progress persistence across sessions
- [ ] Fix GogoAnime details page crash
- [ ] Fix search race conditions (debounce + cancellation)
- [ ] Search timeout handling
- [ ] Download system (individual + batch, queue, WorkManager)
- [ ] Continue watching / reading resume position
- [ ] Subtitle support (embedded + external .srt/.vtt)
- [ ] Picture-in-Picture mode
- [ ] Video quality selection
- [ ] Search history with suggestions
- [ ] Play Store level polish and stability

### Out of Scope

<!-- Explicit boundaries. Includes reasoning to prevent re-adding. -->

- OmniStream Web companion site — deferred to future milestone, focus on Android first
- External source repositories (plugin system) — complexity too high for v2, core sources sufficient
- Real-time chat/social features — not core to streaming/reading value
- Chromecast support — defer to v3, PiP covers mobile use case
- Background playback (audio-only) — niche use, defer
- Skip intro/outro buttons — requires per-source metadata or ML detection, defer

## Context

- App runs on user's Infinix Note 30 as primary test device
- Backend deployed on Render free tier (cold starts ~30-50s after inactivity)
- MongoDB Atlas with open IP whitelist (Render dynamic IPs)
- Video sources use scraping + decryption chains that may break if upstream sites change
- AnimeKai uses WebView approach due to complex encryption (unreliable)
- GogoAnime work paused mid-implementation (crashes on details page)
- FlickyStream + WatchFlix fully working with Vidzee/Cloudnestra decryption
- MangaDex uses official API (most stable source)
- Modded Firefox used for reverse-engineering video source chains

## Constraints

- **Platform**: Android only (Kotlin + Jetpack Compose)
- **Min SDK**: Must run on Infinix Note 30
- **Backend**: Node.js on Render free tier (no paid infrastructure)
- **Sources**: Depend on third-party sites that can change/break without notice
- **Store policy**: Play Store content policies may restrict certain source types

## Current Milestone: v2.0 Play Store Ready

**Goal:** Fix existing bugs, add downloads + player upgrades + search improvements, and polish to Play Store quality.

**Target features:**
- Bug fixes (library, search race conditions, GogoAnime crash, reading progress)
- Download system with individual + batch downloads and queue management
- Continue watching / reading resume
- Subtitle support, PiP mode, quality selection
- Search history and suggestions
- Professional polish for Play Store submission

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Kotlin + Jetpack Compose | Modern Android stack, declarative UI | -- Pending |
| WebView for AnimeKai | Encryption too complex to reverse-engineer | -- Pending |
| Vidzee decryption in-app | No external decryption services needed | -- Pending |
| Room for local data | Standard Android persistence, reactive with Flow | -- Pending |
| Render free tier | Zero cost, sufficient for auth + sync | -- Pending |
| Two-tier access (VIP/Standard) | Monetization model for personal distribution | -- Pending |

---
*Last updated: 2026-01-29 after milestone v2.0 initialization*
