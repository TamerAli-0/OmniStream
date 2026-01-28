# OmniStream - Complete Project Specification

> Kotatsu + CloudStream unified app for manga, anime, movies

---

## Scraping Knowledge (From Your Tutorials)

### Core Techniques

| Technique | Use Case | Implementation |
|-----------|----------|----------------|
| CSS Selectors | HTML parsing | Jsoup `.select("div.class")` |
| Regex | Pattern extraction | `Regex("""pattern""").find()` |
| JSON APIs | Dynamic content | Jackson/Kotlinx serialization |
| Headers | Avoid detection | User-Agent, Referer, X-Requested-With |
| Session | Keep cookies | OkHttp CookieJar |

### Video Extraction Flow
```
Website Page
    ↓ (find iframe src)
iFrame Embed
    ↓ (open separately)
Network Tab → Filter .m3u8/.mp4
    ↓ (trace backwards)
Source API (often encrypted)
    ↓ (decrypt Base64/AES)
Final Video URL
```

### Anti-Detection Headers (Standard Set)
```kotlin
val headers = mapOf(
    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
    "Accept-Language" to "en-US,en;q=0.9",
    "Referer" to "{source_url}",
    "X-Requested-With" to "XMLHttpRequest"
)
```

---

## Kotatsu-Inspired Features

### 1. Source System (Multi-Repository)
- Built-in sources (core)
- External source repositories (like Kotatsu-parsers)
- Speed-based auto-ranking
- Source health monitoring

### 2. Smart Search
- Levenshtein distance for fuzzy matching
- Multi-source parallel search
- Relevance scoring (exact > contains > starts with > fuzzy)
- Duplicate detection via normalized titles

### 3. Reading Experience
- Webtoon (vertical scroll) mode
- Manga (R2L) mode
- Comic (L2R) mode
- Page preloading (configurable depth)
- Reading direction per-manga memory
- Brightness/contrast filters
- Keep screen on option

### 4. Library Management
- Categories (favorites, reading, plan to read, completed)
- Tags (custom user tags)
- Reading progress sync
- Chapter tracking with read/unread markers
- Bulk operations (mark all read, download range)

### 5. Updates & Notifications
- Background chapter checking
- New chapter notifications
- Update feed sorted by date

### 6. Downloads
- Chapter range download
- Queue management
- Storage location selection
- Auto-cleanup of old downloads

---

## CloudStream-Inspired Features

### 1. Video Source System
- Provider interface (search, load, extract)
- Extractor system (VidCloud, StreamTape, MixDrop, etc.)
- Multi-server fallback
- Quality selection

### 2. Player Features
- ExoPlayer with HLS/DASH support
- Subtitle support (embedded + external)
- Skip intro/outro buttons
- Picture-in-picture
- Background playback
- Casting support (Chromecast)

### 3. Content Organization
- Movies vs TV Shows vs Anime separation
- Season/Episode structure
- Continue watching
- Watchlist

---

## Tech Stack

```
┌─────────────────────────────────────────────────────────┐
│                        UI Layer                         │
│  Jetpack Compose + Material 3 + Coil + Navigation       │
├─────────────────────────────────────────────────────────┤
│                    ViewModel Layer                       │
│         StateFlow + Coroutines + Hilt ViewModels        │
├─────────────────────────────────────────────────────────┤
│                    Domain Layer                          │
│              Use Cases + Repository Interfaces           │
├─────────────────────────────────────────────────────────┤
│                     Data Layer                           │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │    Local     │  │    Remote    │  │   Sources    │  │
│  │  Room + DS   │  │ OkHttp+Jsoup │  │   Manager    │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
└─────────────────────────────────────────────────────────┘
```

### Dependencies

```kotlin
// Core
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

// Android
implementation("androidx.core:core-ktx:1.12.0")
implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

// Compose
implementation(platform("androidx.compose:compose-bom:2024.02.00"))
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.navigation:navigation-compose:2.7.7")

// Network
implementation("com.squareup.okhttp3:okhttp:4.12.0")
implementation("org.jsoup:jsoup:1.17.2")

// Image Loading
implementation("io.coil-kt:coil-compose:2.5.0")

// Video Player
implementation("androidx.media3:media3-exoplayer:1.2.1")
implementation("androidx.media3:media3-exoplayer-hls:1.2.1")
implementation("androidx.media3:media3-ui:1.2.1")

// Database
implementation("androidx.room:room-runtime:2.6.1")
implementation("androidx.room:room-ktx:2.6.1")
ksp("androidx.room:room-compiler:2.6.1")

// DI
implementation("com.google.dagger:hilt-android:2.50")
ksp("com.google.dagger:hilt-compiler:2.50")
implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

// Background Work
implementation("androidx.work:work-runtime-ktx:2.9.0")
```

---

## Project Structure

```
app/src/main/java/com/omnistream/
│
├── OmniStreamApp.kt                 # Application class
│
├── core/
│   ├── network/
│   │   ├── OmniHttpClient.kt        # Configured OkHttp
│   │   ├── HeaderInterceptor.kt     # Anti-detection headers
│   │   └── RateLimiter.kt           # Request throttling
│   │
│   ├── parser/
│   │   ├── JsoupExtensions.kt       # Jsoup helpers
│   │   ├── RegexPatterns.kt         # Common patterns
│   │   └── JsonParser.kt            # Kotlinx helpers
│   │
│   └── crypto/
│       ├── Base64Decoder.kt
│       ├── AESDecryptor.kt
│       └── PackedJsUnpacker.kt
│
├── source/
│   ├── SourceManager.kt             # Load & manage sources
│   ├── SourceSpeedTester.kt         # Benchmark sources
│   │
│   ├── model/
│   │   ├── Source.kt                # Base source interface
│   │   ├── MangaSource.kt           # Manga-specific
│   │   ├── VideoSource.kt           # Video-specific
│   │   └── SourceMetadata.kt        # Name, icon, lang, etc.
│   │
│   ├── manga/                       # Built-in manga sources
│   │   ├── MangaDex.kt
│   │   ├── AsuraScans.kt
│   │   ├── MangaPlus.kt
│   │   ├── MangaPill.kt
│   │   ├── WeebCentral.kt
│   │   └── Toonily.kt
│   │
│   ├── anime/                       # Built-in anime sources
│   │   ├── GogoAnime.kt
│   │   ├── Zoro.kt
│   │   └── AnimePahe.kt
│   │
│   ├── movie/                       # Built-in movie sources
│   │   ├── FlixHQ.kt
│   │   ├── VidSrc.kt
│   │   └── SoaperTV.kt
│   │
│   └── extractor/                   # Video link extractors
│       ├── Extractor.kt             # Base interface
│       ├── VidCloudExtractor.kt
│       ├── StreamTapeExtractor.kt
│       ├── MixDropExtractor.kt
│       ├── DoodStreamExtractor.kt
│       └── FembedExtractor.kt
│
├── domain/
│   ├── model/
│   │   ├── Manga.kt
│   │   ├── Chapter.kt
│   │   ├── Page.kt
│   │   ├── Video.kt
│   │   ├── Episode.kt
│   │   └── VideoLink.kt
│   │
│   ├── repository/
│   │   ├── MangaRepository.kt
│   │   ├── VideoRepository.kt
│   │   ├── LibraryRepository.kt
│   │   └── DownloadRepository.kt
│   │
│   └── usecase/
│       ├── SearchUseCase.kt
│       ├── GetChaptersUseCase.kt
│       └── GetVideoLinksUseCase.kt
│
├── data/
│   ├── local/
│   │   ├── OmniDatabase.kt
│   │   ├── entity/
│   │   │   ├── MangaEntity.kt
│   │   │   ├── ChapterEntity.kt
│   │   │   ├── VideoEntity.kt
│   │   │   ├── EpisodeEntity.kt
│   │   │   └── SourceSpeedEntity.kt
│   │   └── dao/
│   │       ├── MangaDao.kt
│   │       ├── VideoDao.kt
│   │       └── SourceDao.kt
│   │
│   └── repository/
│       └── (implementations)
│
├── ui/
│   ├── theme/
│   │   ├── Theme.kt
│   │   ├── Color.kt
│   │   └── Type.kt
│   │
│   ├── navigation/
│   │   └── OmniNavigation.kt
│   │
│   ├── components/
│   │   ├── ContentCard.kt           # Manga/video card
│   │   ├── SourceChip.kt            # Source indicator
│   │   ├── LoadingShimmer.kt
│   │   └── ErrorView.kt
│   │
│   ├── home/
│   │   ├── HomeScreen.kt
│   │   └── HomeViewModel.kt
│   │
│   ├── browse/
│   │   ├── BrowseScreen.kt
│   │   └── BrowseViewModel.kt
│   │
│   ├── search/
│   │   ├── SearchScreen.kt
│   │   └── SearchViewModel.kt
│   │
│   ├── detail/
│   │   ├── MangaDetailScreen.kt
│   │   ├── VideoDetailScreen.kt
│   │   └── DetailViewModel.kt
│   │
│   ├── reader/
│   │   ├── ReaderScreen.kt
│   │   ├── ReaderViewModel.kt
│   │   ├── WebtoonReader.kt
│   │   └── PagerReader.kt
│   │
│   ├── player/
│   │   ├── PlayerScreen.kt
│   │   ├── PlayerViewModel.kt
│   │   └── PlayerControls.kt
│   │
│   ├── library/
│   │   ├── LibraryScreen.kt
│   │   └── LibraryViewModel.kt
│   │
│   ├── sources/
│   │   ├── SourcesScreen.kt
│   │   └── SourcesViewModel.kt
│   │
│   └── settings/
│       ├── SettingsScreen.kt
│       └── SettingsViewModel.kt
│
└── di/
    ├── AppModule.kt
    ├── NetworkModule.kt
    ├── DatabaseModule.kt
    └── SourceModule.kt
```

---

## Source Interfaces

### MangaSource (Kotatsu-style)

```kotlin
interface MangaSource {
    val id: String
    val name: String
    val baseUrl: String
    val lang: String
    val isNsfw: Boolean

    // Catalog
    suspend fun getPopular(page: Int): List<Manga>
    suspend fun getLatest(page: Int): List<Manga>
    suspend fun search(query: String, page: Int): List<Manga>

    // Details
    suspend fun getDetails(manga: Manga): Manga
    suspend fun getChapters(manga: Manga): List<Chapter>
    suspend fun getPages(chapter: Chapter): List<Page>

    // Health
    suspend fun ping(): Boolean
}
```

### VideoSource (CloudStream-style)

```kotlin
interface VideoSource {
    val id: String
    val name: String
    val baseUrl: String
    val lang: String
    val supportedTypes: Set<VideoType> // ANIME, MOVIE, TV_SERIES

    // Catalog
    suspend fun getHomePage(): List<HomeSection>
    suspend fun search(query: String, page: Int): List<Video>

    // Details
    suspend fun getDetails(video: Video): Video
    suspend fun getEpisodes(video: Video): List<Episode>
    suspend fun getLinks(episode: Episode): List<VideoLink>

    // Health
    suspend fun ping(): Boolean
}

enum class VideoType { ANIME, MOVIE, TV_SERIES }

data class VideoLink(
    val url: String,
    val quality: String,
    val extractorName: String,
    val referer: String? = null,
    val isM3u8: Boolean = false,
    val subtitles: List<Subtitle> = emptyList()
)
```

### Extractor

```kotlin
interface Extractor {
    val name: String
    val domains: List<String>

    fun canHandle(url: String): Boolean = domains.any { url.contains(it) }
    suspend fun extract(url: String, referer: String?): List<VideoLink>
}
```

---

## UI Mockups

### Home Screen
```
╔═══════════════════════════════════════╗
║  OmniStream                    🔍  ⚙️ ║
╠═══════════════════════════════════════╣
║  ┌─────┐ ┌─────┐ ┌─────┐ ┌─────┐     ║
║  │MANGA│ │ANIME│ │MOVIE│ │  TV │     ║  ← Category tabs
║  └─────┘ └─────┘ └─────┘ └─────┘     ║
╠═══════════════════════════════════════╣
║  ╭─────────────────────────────────╮  ║
║  │      Hero Banner Carousel       │  ║
║  │         (Featured)              │  ║
║  ╰─────────────────────────────────╯  ║
╠═══════════════════════════════════════╣
║  Continue ─────────────────────────▶  ║
║  ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐      ║
║  │ █ │ │ █ │ │ █ │ │ █ │ │ █ │      ║
║  │▓▓▓│ │▓░░│ │▓▓░│ │▓▓▓│ │░░░│      ║  ← Progress bars
║  └───┘ └───┘ └───┘ └───┘ └───┘      ║
╠═══════════════════════════════════════╣
║  Trending ─────────────────────────▶  ║
║  ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐      ║
║  │   │ │   │ │   │ │   │ │   │      ║
║  └───┘ └───┘ └───┘ └───┘ └───┘      ║
╠═══════════════════════════════════════╣
║  New Releases ─────────────────────▶  ║
║  ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐      ║
║  │   │ │   │ │   │ │   │ │   │      ║
║  └───┘ └───┘ └───┘ └───┘ └───┘      ║
╚═══════════════════════════════════════╝
║  🏠    📖    🔍    📥    ⚙️          ║  ← Bottom nav
╚═══════════════════════════════════════╝
```

### Source Manager
```
╔═══════════════════════════════════════╗
║  ← Sources                      🔄    ║
╠═══════════════════════════════════════╣
║  Speed Test:  ████████░░░  847ms avg  ║
╠═══════════════════════════════════════╣
║  ── MANGA ──────────────────────────  ║
║                                       ║
║  ⚡ MangaDex         ✓    120ms  ██░  ║
║  ⚡ AsuraScans       ✓    340ms  ██░  ║
║  ⚡ MangaPlus        ✓    450ms  ██░  ║
║  ⚠️ MangaPill        ✓    890ms  █░░  ║
║  ❌ MangaFire        ✗   Broken  ░░░  ║
║                                       ║
║  ── ANIME ──────────────────────────  ║
║                                       ║
║  ⚡ Zoro             ✓    180ms  ██░  ║
║  ⚡ GogoAnime        ✓    420ms  ██░  ║
║                                       ║
║  ── MOVIES ─────────────────────────  ║
║                                       ║
║  ⚡ FlixHQ           ✓    380ms  ██░  ║
║  ⚡ VidSrc           ✓    520ms  ██░  ║
╠═══════════════════════════════════════╣
║  [+ Add External Repository]          ║
╚═══════════════════════════════════════╝
```

### Reader (Webtoon Mode)
```
╔═══════════════════════════════════════╗
║              (content)                ║
║                                       ║
║         ┌─────────────────┐           ║
║         │                 │           ║
║         │    Page 1       │           ║
║         │                 │           ║
║         │                 │           ║
║         └─────────────────┘           ║
║                                       ║
║         ┌─────────────────┐           ║
║         │                 │           ║
║         │    Page 2       │           ║
║         │                 │           ║
║         │                 │           ║
║         └─────────────────┘           ║
║                                       ║
║         ┌─────────────────┐           ║
║         │                 │           ║
║         │    Page 3       │           ║
║         │                 │           ║
║         └─────────────────┘           ║
╚═══════════════════════════════════════╝
        (tap center for controls)

╔═══════════════════════════════════════╗
║ ← Ch.45: Title           ≡    ⚙️     ║
╠═══════════════════════════════════════╣
║                                       ║
║              (content)                ║
║                                       ║
╠═══════════════════════════════════════╣
║ ◀ Prev     Page 12/45      Next ▶    ║
║ ═══════════════●═══════════════════  ║
╚═══════════════════════════════════════╝
```

---

## Development Phases

### Phase 1: Core Foundation
- [ ] Android Studio project setup
- [ ] Gradle configuration (all deps)
- [ ] Core network layer (OkHttp + headers)
- [ ] Base source interfaces
- [ ] Hilt DI setup

### Phase 2: First Source (MangaDex)
- [ ] Implement MangaDex source (has official API)
- [ ] Basic UI: browse, search, detail
- [ ] Room database for favorites/history

### Phase 3: Manga Reader
- [ ] Webtoon vertical scroll
- [ ] Paged reader (L2R, R2L)
- [ ] Image preloading
- [ ] Progress tracking

### Phase 4: More Manga Sources
- [ ] AsuraScans
- [ ] MangaPlus
- [ ] Source speed testing

### Phase 5: Video Foundation
- [ ] VideoSource interface
- [ ] ExoPlayer setup
- [ ] First extractor (VidCloud)

### Phase 6: Video Sources
- [ ] GogoAnime
- [ ] FlixHQ
- [ ] More extractors

### Phase 7: Polish
- [ ] Material 3 theming
- [ ] Animations
- [ ] Downloads (WorkManager)
- [ ] Settings

---

## Ready to Create Project?

Let me know and I'll generate:
1. `build.gradle.kts` files
2. Core classes (network, source interfaces)
3. First source implementation (MangaDex)
4. Basic UI screens

The app will be called **OmniStream** and work on your Infinix Note 30.
