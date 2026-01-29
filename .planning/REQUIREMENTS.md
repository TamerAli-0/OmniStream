# Requirements: OmniStream v2.0

## Milestone Requirements

### Bug Fixes

- [ ] **BUG-01**: User can see local Room favorites in Library screen (not just cloud sync data)
- [ ] **BUG-02**: User can type rapidly in search without race conditions (debounce + cancellation)
- [ ] **BUG-03**: User can resume reading a manga chapter from where they left off (reading progress persistence)

### Downloads

- [ ] **DL-01**: User can download individual episodes or manga chapters
- [ ] **DL-02**: User can batch download multiple episodes/chapters at once
- [ ] **DL-03**: User can view, pause, cancel, and delete downloads in a queue UI
- [ ] **DL-04**: User can play/read downloaded content offline without internet

### Player

- [ ] **PLAYER-01**: User can resume video/manga from last position via Continue row on Home screen
- [ ] **PLAYER-02**: User can load SRT/VTT subtitles (embedded + external)
- [ ] **PLAYER-03**: User can use Picture-in-Picture mode during video playback
- [ ] **PLAYER-04**: User can select video quality (720p, 1080p, etc.)
- [ ] **PLAYER-05**: User can swipe to seek, adjust volume/brightness with gestures
- [ ] **PLAYER-06**: User can switch video display modes (Fit/Stretch/Zoom)
- [ ] **PLAYER-07**: User can adjust playback speed (0.5x to 2x)
- [ ] **PLAYER-08**: User can skip intro/outro segments automatically (AniSkip integration)

### Search

- [ ] **SEARCH-01**: User can see recent search history with suggestions dropdown
- [ ] **SEARCH-02**: User gets graceful handling when sources are slow/unresponsive (timeout)
- [ ] **SEARCH-03**: User can filter search results by source type, genre, or year

### Notifications

- [ ] **NOTIF-01**: User can subscribe to shows and get notified when new episodes are available

## Future Requirements

- GogoAnime details page crash fix (paused, selectors need investigation)
- Chromecast support
- Web companion site
- External source repositories (plugin system)

## Out of Scope

| Feature | Reason |
|---------|--------|
| Play Store distribution | Scraping architecture incompatible with Play Store policies; sideload only |
| Real-time chat/social | Not core to streaming/reading value |
| Background audio-only | Niche use case, defer to v3 |
| Full ASS/SSA subtitle rendering | Too complex for v2.0, SRT/VTT covers 95% of use cases |
| Custom video player engine | Media3 ExoPlayer handles all requirements |
| Torrent/P2P downloads | Legal risk, unnecessary complexity |

## Traceability

<!-- Filled by roadmapper -->

| REQ-ID | Phase | Status |
|--------|-------|--------|
| | | |

---
*18 requirements across 5 categories*
*Last updated: 2026-01-29*
