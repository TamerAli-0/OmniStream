package com.omnistream.source.movie

import com.omnistream.core.crypto.CryptoUtils
import com.omnistream.core.network.OmniHttpClient
import com.omnistream.domain.model.Episode
import com.omnistream.domain.model.HomeSection
import com.omnistream.domain.model.Video
import com.omnistream.domain.model.VideoLink
import com.omnistream.domain.model.VideoStatus
import com.omnistream.source.model.VideoSource
import com.omnistream.source.model.VideoType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import org.jsoup.Jsoup

/**
 * FlickyStream source for movies and TV shows.
 * Site: https://flickystream.ru
 */
class FlickyStreamSource(
    private val httpClient: OmniHttpClient
) : VideoSource {

    override val id = "flickystream"
    override val name = "FlickyStream"
    override val baseUrl = "https://flickystream.ru"
    override val lang = "en"
    override val supportedTypes = setOf(VideoType.MOVIE, VideoType.TV_SERIES)

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // Vidzee TMDB proxy API
    private val tmdbProxyUrl = "https://mid.vidzee.wtf/tmdb"
    private val tmdbApiKey = "297f1b91919bae59d50ed815f8d2e14c"

    // Vidzee decryption key (cached)
    private var vidzeeKey: String? = null

    /**
     * Fetch and decrypt the Vidzee API key used for decrypting video links.
     * Key is fetched from https://core.vidzee.wtf/api-key and decrypted with AES-GCM.
     */
    private suspend fun getVidzeeKey(): String {
        vidzeeKey?.let { return it }

        return try {
            val response = httpClient.get("https://core.vidzee.wtf/api-key")
            val decryptedKey = CryptoUtils.vidzeeDecryptApiKey(response.trim())
            android.util.Log.d("FlickyStream", "Fetched vidzee key: $decryptedKey")
            vidzeeKey = decryptedKey
            decryptedKey
        } catch (e: Exception) {
            android.util.Log.e("FlickyStream", "Failed to fetch vidzee key", e)
            ""
        }
    }

    override suspend fun getHomePage(): List<HomeSection> {
        val sections = mutableListOf<HomeSection>()

        try {
            android.util.Log.d("FlickyStream", "Loading home page via TMDB proxy")

            // Trending
            val trending = fetchFromTmdbProxy("trending/movie/day")
            if (trending.isNotEmpty()) {
                sections.add(HomeSection("Trending", trending.take(20)))
            }

            // Popular Movies
            val popular = fetchFromTmdbProxy("movie/popular")
            if (popular.isNotEmpty()) {
                sections.add(HomeSection("Popular Movies", popular.take(20)))
            }

            // Top Rated
            val topRated = fetchFromTmdbProxy("movie/top_rated")
            if (topRated.isNotEmpty()) {
                sections.add(HomeSection("Top Rated", topRated.take(20)))
            }

            // Upcoming
            val upcoming = fetchFromTmdbProxy("movie/upcoming")
            if (upcoming.isNotEmpty()) {
                sections.add(HomeSection("Coming Soon", upcoming.take(20)))
            }

            // Popular TV Shows
            val tvPopular = fetchFromTmdbProxy("tv/popular")
            if (tvPopular.isNotEmpty()) {
                sections.add(HomeSection("Popular TV Shows", tvPopular.take(20)))
            }

        } catch (e: Exception) {
            android.util.Log.e("FlickyStream", "Failed to load home page", e)
        }

        return sections
    }

    private suspend fun fetchFromTmdbProxy(endpoint: String): List<Video> {
        return try {
            val url = "$tmdbProxyUrl/$endpoint?api_key=$tmdbApiKey&language=en-US&page=1"
            android.util.Log.d("FlickyStream", "Fetching: $url")

            val response = httpClient.get(url)
            android.util.Log.d("FlickyStream", "TMDB response length: ${response.length}")

            parseTmdbResponse(response, if (endpoint.contains("tv")) VideoType.TV_SERIES else VideoType.MOVIE)
        } catch (e: Exception) {
            android.util.Log.e("FlickyStream", "TMDB proxy fetch failed: $endpoint", e)
            emptyList()
        }
    }

    private fun parseTmdbResponse(response: String, type: VideoType): List<Video> {
        return try {
            val jsonObj = json.parseToJsonElement(response)
            if (jsonObj !is JsonObject) return emptyList()

            val results = jsonObj["results"]
            if (results !is JsonArray) return emptyList()

            results.mapNotNull { element ->
                try {
                    val item = element as? JsonObject ?: return@mapNotNull null
                    val id = (item["id"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: return@mapNotNull null
                    val title = (item["title"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                        ?: (item["name"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                        ?: return@mapNotNull null
                    val posterPath = (item["poster_path"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                    val releaseDate = (item["release_date"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                        ?: (item["first_air_date"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                    val voteAverage = (item["vote_average"] as? kotlinx.serialization.json.JsonPrimitive)?.content?.toFloatOrNull()

                    Video(
                        id = id,
                        sourceId = this.id,
                        title = title,
                        url = "$baseUrl/${if (type == VideoType.TV_SERIES) "tv" else "movie"}/$id",
                        type = type,
                        posterUrl = posterPath?.let { "https://image.tmdb.org/t/p/w342$it" },
                        year = releaseDate?.take(4)?.toIntOrNull(),
                        rating = voteAverage
                    )
                } catch (e: Exception) {
                    null
                }
            }.also {
                android.util.Log.d("FlickyStream", "Parsed ${it.size} items from TMDB")
            }
        } catch (e: Exception) {
            android.util.Log.e("FlickyStream", "TMDB parse failed", e)
            emptyList()
        }
    }

    private fun parseApiResponse(response: String): List<Video> {
        return try {
            // Try parsing as array
            if (response.trimStart().startsWith("[")) {
                val items = json.decodeFromString<List<FlickyItem>>(response)
                return items.mapNotNull { parseFlickyItem(it) }
            }

            // Try parsing as object with results/data array
            val jsonObj = json.parseToJsonElement(response)
            if (jsonObj is kotlinx.serialization.json.JsonObject) {
                val results = jsonObj["results"] ?: jsonObj["data"] ?: jsonObj["movies"] ?: jsonObj["items"]
                if (results is kotlinx.serialization.json.JsonArray) {
                    return results.mapNotNull { element ->
                        try {
                            val item = json.decodeFromJsonElement<FlickyItem>(element)
                            parseFlickyItem(item)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
            }
            emptyList()
        } catch (e: Exception) {
            android.util.Log.e("FlickyStream", "API parse failed", e)
            emptyList()
        }
    }

    private fun parseFlickyItem(item: FlickyItem): Video? {
        val videoId = item.id?.toString() ?: item.slug ?: return null
        val title = item.title ?: item.name ?: return null

        return Video(
            id = videoId,
            sourceId = id,
            title = title,
            url = "$baseUrl/movie/$videoId",
            type = VideoType.MOVIE,
            posterUrl = item.poster ?: item.image ?: if (item.poster_path != null) "https://image.tmdb.org/t/p/w342${item.poster_path}" else null,
            year = item.year ?: item.release_date?.take(4)?.toIntOrNull(),
            rating = item.rating?.toFloatOrNull() ?: item.vote_average
        )
    }

    private fun parseNextData(nextData: String, type: VideoType): List<Video> {
        return try {
            val jsonObj = json.parseToJsonElement(nextData)
            if (jsonObj !is JsonObject) return emptyList()

            val props = jsonObj["props"] as? JsonObject ?: return emptyList()
            val pageProps = props["pageProps"] as? JsonObject ?: return emptyList()

            // Try different possible keys for movie/tv data
            val possibleKeys = listOf("movies", "trending", "popular", "results", "data", "items", "shows", "tvShows")
            for (key in possibleKeys) {
                val items = pageProps[key]
                if (items is JsonArray && items.isNotEmpty()) {
                    android.util.Log.d("FlickyStream", "Found ${items.size} items in __NEXT_DATA__.$key")
                    return items.mapNotNull { element ->
                        try {
                            val item = json.decodeFromJsonElement<FlickyItem>(element)
                            parseFlickyItem(item)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }
            }
            emptyList()
        } catch (e: Exception) {
            android.util.Log.e("FlickyStream", "Failed to parse __NEXT_DATA__", e)
            emptyList()
        }
    }

    private suspend fun fetchContent(url: String, type: VideoType): List<Video> {
        return try {
            val response = httpClient.get(url)
            android.util.Log.d("FlickyStream", "Fetching: $url, response length: ${response.length}")

            // Try JSON API first
            if (response.trimStart().startsWith("[") || response.trimStart().startsWith("{")) {
                return parseJsonResponse(response, type)
            }

            // Fall back to HTML parsing
            val doc = Jsoup.parse(response, url)

            // Try to find __NEXT_DATA__ script (Next.js apps embed initial data here)
            val nextDataScript = doc.selectFirst("script#__NEXT_DATA__")
            if (nextDataScript != null) {
                val nextData = nextDataScript.data()
                android.util.Log.d("FlickyStream", "Found __NEXT_DATA__: ${nextData.take(500)}")
                val movies = parseNextData(nextData, type)
                if (movies.isNotEmpty()) return movies
            }

            // Also check for self.__next_f.push patterns (React Server Components)
            val rscPattern = Regex("""self\.__next_f\.push\(\[([\d]+),"(.+?)"\]\)""")
            doc.select("script").forEach { script ->
                val content = script.data()
                if (content.contains("__next_f")) {
                    android.util.Log.d("FlickyStream", "Found RSC data")
                }
            }

            // FlickyStream uses a[href^="/movie/"] or a[href^="/tv/"] for cards
            val movieLinks = doc.select("a[href^='/movie/'], a[href^='/tv/']")
            android.util.Log.d("FlickyStream", "Found ${movieLinks.size} movie/tv links")

            movieLinks.mapNotNull { element ->
                try {
                    val href = element.attr("href")
                    if (href.isBlank()) return@mapNotNull null

                    val videoId = href.substringAfterLast("/").substringBefore("?")
                    if (videoId.isBlank()) return@mapNotNull null

                    // Title from h3 or img alt attribute
                    val title = element.selectFirst("h3")?.text()?.trim()
                        ?: element.selectFirst("img")?.attr("alt")?.trim()
                        ?: return@mapNotNull null
                    if (title.isBlank()) return@mapNotNull null

                    // Poster from img src (TMDB images)
                    val posterUrl = element.selectFirst("img")?.attr("src")

                    // Year from span.line-clamp-1
                    val year = element.selectFirst("span.line-clamp-1")?.text()?.trim()?.toIntOrNull()

                    // Rating from the amber div (text after SVG)
                    val ratingText = element.selectFirst("div.text-amber-400, div[class*='amber']")?.text()?.trim()
                    val rating = ratingText?.toFloatOrNull()

                    val isMovie = href.contains("/movie/")

                    Video(
                        id = videoId,
                        sourceId = id,
                        title = title,
                        url = "$baseUrl$href",
                        type = if (isMovie) VideoType.MOVIE else VideoType.TV_SERIES,
                        posterUrl = posterUrl,
                        year = year,
                        rating = rating
                    )
                } catch (e: Exception) {
                    null
                }
            }.distinctBy { it.id }.also {
                android.util.Log.d("FlickyStream", "Parsed ${it.size} videos")
            }
        } catch (e: Exception) {
            android.util.Log.e("FlickyStream", "Fetch failed: $url", e)
            emptyList()
        }
    }

    private fun parseJsonResponse(response: String, type: VideoType): List<Video> {
        return try {
            val items = json.decodeFromString<List<FlickyItem>>(response)
            items.mapNotNull { item ->
                Video(
                    id = item.id?.toString() ?: item.slug ?: return@mapNotNull null,
                    sourceId = id,
                    title = item.title ?: item.name ?: return@mapNotNull null,
                    url = "$baseUrl/${if (type == VideoType.MOVIE) "movie" else "show"}/${item.slug ?: item.id}",
                    type = type,
                    posterUrl = item.poster ?: item.image,
                    year = item.year,
                    rating = item.rating?.toFloatOrNull()
                )
            }
        } catch (e: Exception) {
            android.util.Log.e("FlickyStream", "JSON parse failed", e)
            emptyList()
        }
    }

    private fun extractId(href: String): String {
        return href
            .substringAfterLast("/")
            .substringBefore("?")
            .substringBefore("#")
    }

    override suspend fun search(query: String, page: Int): List<Video> {
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val results = mutableListOf<Video>()

        try {
            // Search movies via TMDB proxy
            val movieUrl = "$tmdbProxyUrl/search/movie?api_key=$tmdbApiKey&language=en-US&query=$encodedQuery&page=$page"
            android.util.Log.d("FlickyStream", "Searching movies: $movieUrl")
            val movieResponse = httpClient.get(movieUrl)
            results.addAll(parseTmdbResponse(movieResponse, VideoType.MOVIE))

            // Search TV shows via TMDB proxy
            val tvUrl = "$tmdbProxyUrl/search/tv?api_key=$tmdbApiKey&language=en-US&query=$encodedQuery&page=$page"
            android.util.Log.d("FlickyStream", "Searching TV: $tvUrl")
            val tvResponse = httpClient.get(tvUrl)
            results.addAll(parseTmdbResponse(tvResponse, VideoType.TV_SERIES))

        } catch (e: Exception) {
            android.util.Log.e("FlickyStream", "Search failed", e)
        }

        return results.distinctBy { it.id }.also {
            android.util.Log.d("FlickyStream", "Search returned ${it.size} results")
        }
    }

    override suspend fun getDetails(video: Video): Video {
        return try {
            val doc = httpClient.getDocument(video.url)
            android.util.Log.d("FlickyStream", "Getting details: ${video.url}")

            val title = doc.selectFirst("h1, .title, .movie-title")?.text()?.trim() ?: video.title

            val posterUrl = doc.selectFirst(".poster img, .cover img, img.poster")?.let { img ->
                img.attr("src").ifBlank { img.attr("data-src") }
            }?.let { if (it.startsWith("http")) it else "$baseUrl$it" } ?: video.posterUrl

            val description = doc.selectFirst(".description, .overview, .synopsis, p.plot")?.text()?.trim()

            val year = doc.selectFirst(".year, .release-year, span:contains(20)")?.text()
                ?.let { Regex("""(\d{4})""").find(it)?.groupValues?.get(1)?.toIntOrNull() }
                ?: video.year

            val genres = doc.select(".genres a, .genre a, a[href*='/genre/']")
                .map { it.text().trim() }
                .filter { it.isNotBlank() }

            val rating = doc.selectFirst(".rating, .imdb, span:contains(IMDb)")?.text()
                ?.let { Regex("""([\d.]+)""").find(it)?.groupValues?.get(1)?.toFloatOrNull() }
                ?: video.rating

            video.copy(
                title = title,
                posterUrl = posterUrl,
                description = description,
                year = year,
                genres = genres,
                rating = rating
            )
        } catch (e: Exception) {
            android.util.Log.e("FlickyStream", "Get details failed", e)
            video
        }
    }

    override suspend fun getEpisodes(video: Video): List<Episode> {
        // For movies, return single episode
        if (video.type == VideoType.MOVIE || video.url.contains("/movie/")) {
            return listOf(
                Episode(
                    id = video.id,
                    videoId = video.id,
                    sourceId = id,
                    url = video.url,
                    title = video.title,
                    number = 1
                )
            )
        }

        // For TV series, parse episodes
        return try {
            val doc = httpClient.getDocument(video.url)
            android.util.Log.d("FlickyStream", "Getting episodes: ${video.url}")

            val episodes = mutableListOf<Episode>()

            // Find episode links
            val episodeSelectors = listOf(
                "a[href*='/episode/'], a[href*='/ep/']",
                ".episode-list a, .episodes a",
                "a[data-episode], a[data-ep]"
            )

            for (selector in episodeSelectors) {
                doc.select(selector).forEach { element ->
                    try {
                        val href = element.attr("href")
                        val epId = extractId(href)

                        // Extract S01E01 format
                        val match = Regex("""[Ss](\d+)[Ee](\d+)""").find(href + element.text())
                        val season = match?.groupValues?.get(1)?.toIntOrNull() ?: 1
                        val epNum = match?.groupValues?.get(2)?.toIntOrNull()
                            ?: Regex("""(\d+)""").find(element.text())?.groupValues?.get(1)?.toIntOrNull()
                            ?: return@forEach

                        val epTitle = element.attr("title").ifBlank {
                            element.selectFirst(".ep-title, .title")?.text()
                        }

                        episodes.add(Episode(
                            id = epId,
                            videoId = video.id,
                            sourceId = id,
                            url = if (href.startsWith("http")) href else "$baseUrl$href",
                            title = epTitle,
                            number = epNum,
                            season = season
                        ))
                    } catch (e: Exception) {
                        // Skip
                    }
                }

                if (episodes.isNotEmpty()) break
            }

            episodes.sortedWith(compareBy({ it.season }, { it.number })).also {
                android.util.Log.d("FlickyStream", "Found ${it.size} episodes")
            }
        } catch (e: Exception) {
            android.util.Log.e("FlickyStream", "Get episodes failed", e)
            emptyList()
        }
    }

    override suspend fun getLinks(episode: Episode): List<VideoLink> {
        val links = mutableListOf<VideoLink>()
        val videoId = episode.id
        val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

        try {
            android.util.Log.d("FlickyStream", "Getting links for TMDB ID: $videoId")

            // Method 1: Try player.vidzee.wtf API with proper decryption
            try {
                // First, get the decryption key
                val decryptionKey = getVidzeeKey()
                android.util.Log.d("FlickyStream", "Using vidzee decryption key: $decryptionKey")

                for (server in listOf(2, 0, 1)) { // Try server 2 first - it usually works!
                    if (links.isNotEmpty()) break
                    val apiUrl = "https://player.vidzee.wtf/api/server?id=$videoId&sr=$server"
                    android.util.Log.d("FlickyStream", "Trying vidzee API server $server: $apiUrl")

                    try {
                        val response = httpClient.get(apiUrl, headers = mapOf(
                            "User-Agent" to userAgent,
                            "Referer" to "https://player.vidzee.wtf/embed/movie/$videoId",
                            "Origin" to "https://player.vidzee.wtf",
                            "Accept" to "application/json, text/plain, */*",
                            "Accept-Language" to "en-US,en;q=0.9",
                            "sec-ch-ua" to "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"",
                            "sec-ch-ua-mobile" to "?0",
                            "sec-ch-ua-platform" to "\"Windows\"",
                            "sec-fetch-dest" to "empty",
                            "sec-fetch-mode" to "cors",
                            "sec-fetch-site" to "same-origin"
                        ))
                        android.util.Log.d("FlickyStream", "Vidzee API server $server response: ${response.take(500)}")

                        // Check for error response
                        if (response.contains("\"error\"")) {
                            android.util.Log.d("FlickyStream", "Server $server returned error, trying next...")
                            continue
                        }

                        // Parse JSON response - format: {"provider":"Glory","url":[{"lang":"English","link":"encrypted..."}]}
                        if (response.contains("\"url\"") && response.contains("\"link\"")) {
                            try {
                                val jsonObj = json.parseToJsonElement(response) as? kotlinx.serialization.json.JsonObject
                                val urlArray = jsonObj?.get("url") as? kotlinx.serialization.json.JsonArray
                                val provider = (jsonObj?.get("provider") as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "Unknown"

                                urlArray?.forEach { urlItem ->
                                    val urlObj = urlItem as? kotlinx.serialization.json.JsonObject
                                    val encryptedLink = (urlObj?.get("link") as? kotlinx.serialization.json.JsonPrimitive)?.content
                                    val lang = (urlObj?.get("lang") as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "Unknown"
                                    val linkType = (urlObj?.get("type") as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "hls"

                                    if (encryptedLink != null && decryptionKey.isNotEmpty()) {
                                        android.util.Log.d("FlickyStream", "Decrypting link for $lang (provider: $provider)...")

                                        // Decrypt the link using AES-CBC
                                        val decryptedUrl = CryptoUtils.vidzeeDecryptLink(encryptedLink, decryptionKey)

                                        if (decryptedUrl.isNotEmpty() && decryptedUrl.startsWith("http")) {
                                            android.util.Log.d("FlickyStream", "Decrypted URL: $decryptedUrl")
                                            links.add(VideoLink(
                                                url = decryptedUrl,
                                                quality = "$lang - $provider",
                                                extractorName = "Vidzee",
                                                isM3u8 = linkType == "hls" || decryptedUrl.contains(".m3u8"),
                                                referer = "https://player.vidzee.wtf/"
                                            ))
                                        } else {
                                            android.util.Log.d("FlickyStream", "Decryption produced invalid URL: ${decryptedUrl.take(100)}")
                                        }
                                    }
                                }

                                // Also extract subtitles from tracks array
                                val tracksArray = jsonObj?.get("tracks") as? kotlinx.serialization.json.JsonArray
                                tracksArray?.forEach { trackItem ->
                                    val trackObj = trackItem as? kotlinx.serialization.json.JsonObject
                                    val trackUrl = (trackObj?.get("url") as? kotlinx.serialization.json.JsonPrimitive)?.content
                                    val trackLang = (trackObj?.get("lang") as? kotlinx.serialization.json.JsonPrimitive)?.content
                                    if (trackUrl != null && trackLang != null) {
                                        android.util.Log.d("FlickyStream", "Found subtitle: $trackLang - $trackUrl")
                                        // TODO: Store subtitles for player
                                    }
                                }

                            } catch (e: Exception) {
                                android.util.Log.e("FlickyStream", "JSON parse failed: ${e.message}")
                            }
                        }
                    } catch (e: Exception) {
                        android.util.Log.d("FlickyStream", "Vidzee API server $server failed: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("FlickyStream", "Vidzee API failed", e)
            }

            // Method 1b: Try vidsrc.xyz as fallback
            if (links.isEmpty()) {
                try {
                    val vidsrcXyzUrl = "https://vidsrc.xyz/embed/movie/$videoId"
                    android.util.Log.d("FlickyStream", "Trying vidsrc.xyz: $vidsrcXyzUrl")
                    extractVidsrcLinks(vidsrcXyzUrl, links, userAgent)
                } catch (e: Exception) {
                    android.util.Log.e("FlickyStream", "vidsrc.xyz failed", e)
                }
            }

            // Method 2: Try vidsrc.me
            if (links.isEmpty()) {
                try {
                    val vidsrcMeUrl = "https://vidsrc.me/embed/movie?tmdb=$videoId"
                    android.util.Log.d("FlickyStream", "Trying vidsrc.me: $vidsrcMeUrl")
                    extractVidsrcLinks(vidsrcMeUrl, links, userAgent)
                } catch (e: Exception) {
                    android.util.Log.d("FlickyStream", "vidsrc.me failed")
                }
            }

            // Method 3: Try vidsrc.net
            if (links.isEmpty()) {
                try {
                    val vidsrcNetUrl = "https://vidsrc.net/embed/movie/$videoId"
                    android.util.Log.d("FlickyStream", "Trying vidsrc.net: $vidsrcNetUrl")
                    extractVidsrcLinks(vidsrcNetUrl, links, userAgent)
                } catch (e: Exception) {
                    android.util.Log.d("FlickyStream", "vidsrc.net failed")
                }
            }

            // Method 4: Try embedsu
            if (links.isEmpty()) {
                try {
                    val embedsuUrl = "https://embed.su/embed/movie/$videoId"
                    android.util.Log.d("FlickyStream", "Trying embed.su: $embedsuUrl")
                    val response = httpClient.get(embedsuUrl, headers = mapOf(
                        "User-Agent" to userAgent,
                        "Referer" to "https://flickystream.ru/"
                    ))
                    android.util.Log.d("FlickyStream", "embed.su response: ${response.take(500)}")
                    extractLinksFromHtml(response, links)
                    followIframeChain(response, embedsuUrl, links, userAgent, 0)
                } catch (e: Exception) {
                    android.util.Log.d("FlickyStream", "embed.su failed")
                }
            }

            // Method 5: Try 2embed.org (different from 2embed.cc)
            if (links.isEmpty()) {
                try {
                    val twoEmbedUrl = "https://2embed.org/embed/movie/$videoId"
                    android.util.Log.d("FlickyStream", "Trying 2embed.org: $twoEmbedUrl")
                    val response = httpClient.get(twoEmbedUrl, headers = mapOf(
                        "User-Agent" to userAgent,
                        "Referer" to "https://flickystream.ru/"
                    ))
                    extractLinksFromHtml(response, links)
                    followIframeChain(response, twoEmbedUrl, links, userAgent, 0)
                } catch (e: Exception) {
                    android.util.Log.d("FlickyStream", "2embed.org failed")
                }
            }

            // Method 6: Try moviesapi
            if (links.isEmpty()) {
                try {
                    val moviesApiUrl = "https://moviesapi.club/movie/$videoId"
                    android.util.Log.d("FlickyStream", "Trying moviesapi: $moviesApiUrl")
                    val response = httpClient.get(moviesApiUrl, headers = mapOf(
                        "User-Agent" to userAgent,
                        "Referer" to "https://flickystream.ru/"
                    ))
                    extractLinksFromHtml(response, links)
                    followIframeChain(response, moviesApiUrl, links, userAgent, 0)
                } catch (e: Exception) {
                    android.util.Log.d("FlickyStream", "moviesapi failed")
                }
            }

            // Method 7: Try smashystream endpoints
            if (links.isEmpty()) {
                val smashyEndpoints = listOf(
                    "https://player.smashy.stream/movie/$videoId",
                    "https://embed.smashystream.com/playere.php?tmdb=$videoId",
                    "https://player.smashystream.com/movie/$videoId",
                    "https://smashystream.com/e/movie/$videoId"
                )

                for (smashyUrl in smashyEndpoints) {
                    if (links.isNotEmpty()) break
                    try {
                        android.util.Log.d("FlickyStream", "Trying smashy: $smashyUrl")
                        val response = httpClient.get(smashyUrl, headers = mapOf(
                            "User-Agent" to userAgent,
                            "Referer" to "https://rapidairmax.site/"
                        ))
                        android.util.Log.d("FlickyStream", "Smashy response (${smashyUrl.substringAfterLast("/")}): ${response.take(800)}")

                        // Look for the proxy m3u8 pattern
                        val proxyPattern = Regex("""(https?://streams\.smashystream\.top/proxy/m3u8/[^"'\s<>]+)""")
                        proxyPattern.findAll(response).forEach { match ->
                            val url = match.groupValues[1].replace("\\u002F", "/").replace("\\/", "/")
                            android.util.Log.d("FlickyStream", "Found smashy proxy URL: $url")
                            links.add(VideoLink(
                                url = url,
                                quality = "Auto",
                                extractorName = "SmashyStream",
                                isM3u8 = true,
                                referer = "https://rapidairmax.site/"
                            ))
                        }

                        // Look for sources array in JS
                        val sourcesMatch = Regex("""sources\s*[:=]\s*\[([^\]]+)\]""").find(response)
                        if (sourcesMatch != null) {
                            android.util.Log.d("FlickyStream", "Found sources: ${sourcesMatch.groupValues[1].take(200)}")
                            Regex("""["']?(https?://[^"'\s,\]]+)["']?""").findAll(sourcesMatch.groupValues[1]).forEach { urlMatch ->
                                val url = urlMatch.groupValues[1].replace("\\/", "/")
                                if ((url.contains(".m3u8") || url.contains("proxy")) && !links.any { it.url == url }) {
                                    links.add(VideoLink(
                                        url = url,
                                        quality = extractQuality(url),
                                        extractorName = "SmashyStream",
                                        isM3u8 = true,
                                        referer = "https://rapidairmax.site/"
                                    ))
                                }
                            }
                        }

                        // Look for file: pattern
                        val fileMatch = Regex("""["']?file["']?\s*[:=]\s*["']([^"']+)["']""").find(response)
                        if (fileMatch != null) {
                            val url = fileMatch.groupValues[1].replace("\\/", "/")
                            android.util.Log.d("FlickyStream", "Found file URL: $url")
                            if (!links.any { it.url == url }) {
                                links.add(VideoLink(
                                    url = url,
                                    quality = "Auto",
                                    extractorName = "SmashyStream",
                                    isM3u8 = url.contains(".m3u8") || url.contains("proxy"),
                                    referer = "https://rapidairmax.site/"
                                ))
                            }
                        }

                        extractLinksFromHtml(response, links)
                        followIframeChain(response, smashyUrl, links, userAgent, 0)
                    } catch (e: Exception) {
                        android.util.Log.d("FlickyStream", "Smashy endpoint failed: ${smashyUrl.substringAfterLast("/")}")
                    }
                }
            }

            // Method 8: Try nontongo
            if (links.isEmpty()) {
                try {
                    val nontongoUrl = "https://www.nontongo.win/embed/movie/$videoId"
                    android.util.Log.d("FlickyStream", "Trying nontongo: $nontongoUrl")
                    val response = httpClient.get(nontongoUrl, headers = mapOf(
                        "User-Agent" to userAgent,
                        "Referer" to "https://flickystream.ru/"
                    ))
                    extractLinksFromHtml(response, links)
                    followIframeChain(response, nontongoUrl, links, userAgent, 0)
                } catch (e: Exception) {
                    android.util.Log.d("FlickyStream", "nontongo failed")
                }
            }

        } catch (e: Exception) {
            android.util.Log.e("FlickyStream", "Get links failed", e)
        }

        return links.distinctBy { it.url }.also {
            android.util.Log.d("FlickyStream", "Found ${it.size} links total")
        }
    }

    private suspend fun extractVidsrcLinks(url: String, links: MutableList<VideoLink>, userAgent: String) {
        val response = httpClient.get(url, headers = mapOf(
            "User-Agent" to userAgent,
            "Referer" to "https://flickystream.ru/"
        ))
        android.util.Log.d("FlickyStream", "Vidsrc response length: ${response.length}")

        // Extract direct video links
        extractLinksFromHtml(response, links)

        // Look for data-hash or encoded sources
        val hashMatch = Regex("""data-hash=["']([^"']+)["']""").find(response)
        if (hashMatch != null) {
            val hash = hashMatch.groupValues[1]
            android.util.Log.d("FlickyStream", "Found data-hash: $hash")
            // Try to decode or fetch from API
            try {
                val apiUrl = url.substringBefore("/embed") + "/api/source/$hash"
                val apiResponse = httpClient.get(apiUrl, headers = mapOf(
                    "User-Agent" to userAgent,
                    "Referer" to url
                ))
                android.util.Log.d("FlickyStream", "Hash API response: ${apiResponse.take(500)}")
                extractLinksFromHtml(apiResponse, links)
            } catch (e: Exception) {
                android.util.Log.d("FlickyStream", "Hash API failed")
            }
        }

        // Look for rcpUrl pattern (common in vidsrc)
        val rcpMatch = Regex("""["']?rcp["']?\s*[:=]\s*["']([^"']+)["']""").find(response)
        if (rcpMatch != null) {
            val rcpUrl = rcpMatch.groupValues[1]
            android.util.Log.d("FlickyStream", "Found rcp URL: $rcpUrl")
            try {
                val fullRcpUrl = if (rcpUrl.startsWith("//")) "https:$rcpUrl"
                    else if (rcpUrl.startsWith("/")) url.substringBefore("/", "").substringBeforeLast("/") + rcpUrl
                    else rcpUrl
                val rcpResponse = httpClient.get(fullRcpUrl, headers = mapOf(
                    "User-Agent" to userAgent,
                    "Referer" to url
                ))
                extractLinksFromHtml(rcpResponse, links)
            } catch (e: Exception) {
                android.util.Log.d("FlickyStream", "RCP URL failed")
            }
        }

        // Follow iframe chain
        followIframeChain(response, url, links, userAgent, 0)
    }

    private suspend fun followIframeChain(html: String, referer: String, links: MutableList<VideoLink>, userAgent: String, depth: Int) {
        if (depth > 3) return // Prevent infinite recursion

        // Find all iframe sources
        val iframePattern = Regex("""<iframe[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        iframePattern.findAll(html).forEach { match ->
            var iframeSrc = match.groupValues[1]
            if (iframeSrc.isBlank() || iframeSrc.contains("ads") || iframeSrc.contains("banner")) return@forEach

            // Normalize URL
            iframeSrc = when {
                iframeSrc.startsWith("//") -> "https:$iframeSrc"
                iframeSrc.startsWith("/") -> {
                    val baseUrl = referer.substringBefore("/", "https://").let {
                        if (it == referer) referer.substringBefore("/", referer.take(50))
                        else it
                    }
                    "$baseUrl$iframeSrc"
                }
                else -> iframeSrc
            }

            android.util.Log.d("FlickyStream", "Following iframe (depth $depth): $iframeSrc")

            try {
                val iframeResponse = httpClient.get(iframeSrc, headers = mapOf(
                    "User-Agent" to userAgent,
                    "Referer" to referer
                ))

                // Extract links from iframe content
                extractLinksFromHtml(iframeResponse, links)

                // Look for video sources in JS
                extractJsVideoSources(iframeResponse, links, iframeSrc)

                // Recursively follow nested iframes
                if (links.isEmpty()) {
                    followIframeChain(iframeResponse, iframeSrc, links, userAgent, depth + 1)
                }
            } catch (e: Exception) {
                android.util.Log.d("FlickyStream", "Iframe failed: $iframeSrc - ${e.message}")
            }
        }
    }

    private fun extractJsVideoSources(html: String, links: MutableList<VideoLink>, referer: String) {
        // Look for common JS patterns that contain video URLs

        // Pattern: file: "url" or source: "url"
        val filePatterns = listOf(
            Regex("""["']?file["']?\s*[:=]\s*["']([^"']+\.m3u8[^"']*)["']"""),
            Regex("""["']?source["']?\s*[:=]\s*["']([^"']+\.m3u8[^"']*)["']"""),
            Regex("""["']?src["']?\s*[:=]\s*["']([^"']+\.m3u8[^"']*)["']"""),
            Regex("""["']?url["']?\s*[:=]\s*["']([^"']+\.m3u8[^"']*)["']"""),
            Regex("""["']?file["']?\s*[:=]\s*["']([^"']+\.mp4[^"']*)["']"""),
            Regex("""["']?source["']?\s*[:=]\s*["']([^"']+\.mp4[^"']*)["']"""),
        )

        filePatterns.forEach { pattern ->
            pattern.findAll(html).forEach { match ->
                val url = match.groupValues[1].replace("\\/", "/")
                if (!links.any { it.url == url }) {
                    android.util.Log.d("FlickyStream", "Found JS video source: $url")
                    links.add(VideoLink(
                        url = url,
                        quality = extractQuality(url),
                        extractorName = "FlickyStream",
                        isM3u8 = url.contains(".m3u8"),
                        referer = referer
                    ))
                }
            }
        }

        // Look for sources array
        val sourcesPattern = Regex("""sources\s*[:=]\s*\[([^\]]+)\]""")
        sourcesPattern.find(html)?.let { match ->
            val sourcesContent = match.groupValues[1]
            Regex("""["']?(https?://[^"'\s,\]]+)["']?""").findAll(sourcesContent).forEach { urlMatch ->
                val url = urlMatch.groupValues[1].replace("\\/", "/")
                if ((url.contains(".m3u8") || url.contains(".mp4")) && !links.any { it.url == url }) {
                    android.util.Log.d("FlickyStream", "Found source array URL: $url")
                    links.add(VideoLink(
                        url = url,
                        quality = extractQuality(url),
                        extractorName = "FlickyStream",
                        isM3u8 = url.contains(".m3u8"),
                        referer = referer
                    ))
                }
            }
        }
    }

    private suspend fun extractFromEmbed(embedUrl: String): List<VideoLink> {
        val links = mutableListOf<VideoLink>()

        try {
            android.util.Log.d("FlickyStream", "Extracting from: $embedUrl")
            val response = httpClient.get(embedUrl, headers = mapOf("Referer" to baseUrl))
            extractLinksFromHtml(response, links)
        } catch (e: Exception) {
            android.util.Log.e("FlickyStream", "Embed extract failed: $embedUrl", e)
        }

        return links
    }

    private fun extractLinksFromHtml(content: String, links: MutableList<VideoLink>) {
        // M3U8 streams
        Regex("""["']?(https?://[^"'\s]+\.m3u8[^"'\s]*)["']?""").findAll(content).forEach { match ->
            val url = match.groupValues[1].replace("\\/", "/")
            if (!links.any { it.url == url }) {
                links.add(VideoLink(
                    url = url,
                    quality = extractQuality(url),
                    extractorName = "FlickyStream",
                    isM3u8 = true,
                    referer = baseUrl
                ))
            }
        }

        // MP4 direct links
        Regex("""["']?(https?://[^"'\s]+\.mp4[^"'\s]*)["']?""").findAll(content).forEach { match ->
            val url = match.groupValues[1].replace("\\/", "/")
            if (!links.any { it.url == url }) {
                links.add(VideoLink(
                    url = url,
                    quality = extractQuality(url),
                    extractorName = "FlickyStream",
                    isM3u8 = false,
                    referer = baseUrl
                ))
            }
        }

        // File/sources in JSON
        Regex("""["']?file["']?\s*:\s*["']([^"']+)["']""").findAll(content).forEach { match ->
            val url = match.groupValues[1].replace("\\/", "/")
            if ((url.contains(".m3u8") || url.contains(".mp4")) && !links.any { it.url == url }) {
                links.add(VideoLink(
                    url = url,
                    quality = extractQuality(url),
                    extractorName = "FlickyStream",
                    isM3u8 = url.contains(".m3u8"),
                    referer = baseUrl
                ))
            }
        }
    }

    private fun extractQuality(url: String): String {
        return when {
            url.contains("4k", true) || url.contains("2160") -> "4K"
            url.contains("1080") -> "1080p"
            url.contains("720") -> "720p"
            url.contains("480") -> "480p"
            url.contains("360") -> "360p"
            else -> "Auto"
        }
    }

    override suspend fun ping(): Boolean {
        return try {
            httpClient.ping(baseUrl) > 0
        } catch (e: Exception) {
            false
        }
    }

    @Serializable
    private data class FlickyItem(
        val id: Int? = null,
        val slug: String? = null,
        val title: String? = null,
        val name: String? = null,
        val poster: String? = null,
        val image: String? = null,
        val poster_path: String? = null,
        val year: Int? = null,
        val rating: String? = null,
        val vote_average: Float? = null,
        val release_date: String? = null,
        val overview: String? = null
    )
}
