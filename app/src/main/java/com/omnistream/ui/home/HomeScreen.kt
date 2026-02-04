package com.omnistream.ui.home

import com.omnistream.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    // Get stats from AniList or watch history
    val stats = uiState.anilistStats
    val totalEpisodesWatched = stats?.episodesWatched ?: uiState.continueWatching.size
    val totalChaptersRead = stats?.chaptersRead ?: uiState.continueReading.sumOf { it.chapterIndex + 1 }
    val username = viewModel.getUsername() ?: "Guest"

    // Use custom background images from drawable resources

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0a0a0a))
    ) {
        // Top bar - NO elevation/shadow
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0a0a0a))
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    username,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Episodes: $totalEpisodesWatched • Chapters: $totalChaptersRead",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            IconButton(onClick = { navController.navigate("settings") }) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // GLOSSY TABS - STICK AT TOP
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
                // ANIME LIST - Custom background
                GlossyTabCardWithImage(
                    modifier = Modifier.weight(1f),
                    title = "ANIME",
                    backgroundRes = R.drawable.bg_anime,
                    isSelected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    accentColor = Color(0xFFFF6B6B) // Red accent
                )

                // MANGA LIST - Custom background
                GlossyTabCardWithImage(
                    modifier = Modifier.weight(1f),
                    title = "MANGA",
                    backgroundRes = R.drawable.bg_manga,
                    isSelected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    accentColor = Color(0xFF4ECDC4) // Cyan accent
                )

                // MOVIES/TV - Custom background
                GlossyTabCardWithImage(
                    modifier = Modifier.weight(1f),
                    title = "MOVIES",
                    backgroundRes = R.drawable.bg_movies,
                    isSelected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    accentColor = Color(0xFFFFD93D) // Yellow accent
                )
        }

        // Content - scrollable
        LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 20.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // ANIME LIST
                        // Continue Watching
                        if (uiState.continueWatching.isNotEmpty()) {
                            item {
                                SectionTitle("Continue Watching")
                                Spacer(Modifier.height(12.dp))
                            }
                            item {
                                CardRow(
                                    items = uiState.continueWatching.map { it.toMediaItem() },
                                    onClick = { navController.navigate("video/${it.sourceId}/${it.id}") }
                                )
                                Spacer(Modifier.height(24.dp))
                            }
                        }

                        // Favourite Anime
                        if (uiState.favoriteAnime.isNotEmpty()) {
                            item {
                                SectionTitle("Favourite Anime")
                                Spacer(Modifier.height(12.dp))
                            }
                            item {
                                CardRow(
                                    items = uiState.favoriteAnime.map { it.toMediaItem() },
                                    onClick = { navController.navigate("video/${it.sourceId}/${it.id}") }
                                )
                                Spacer(Modifier.height(24.dp))
                            }
                        }

                        // Discovery sections - Anime only (filter out movies)
                        uiState.videoSections.take(3).forEach { section ->
                            item {
                                SectionTitle(section.title)
                                Spacer(Modifier.height(12.dp))
                            }
                            item {
                                VideoCardRow(
                                    items = section.items.take(10),
                                    onClick = { video ->
                                        navController.navigate("video/${section.sourceId}/${video.id}")
                                    }
                                )
                                Spacer(Modifier.height(24.dp))
                            }
                        }
                    }
                    1 -> {
                        // MANGA LIST
                        // Continue Reading
                        if (uiState.continueReading.isNotEmpty()) {
                            item {
                                SectionTitle("Continue Reading")
                                Spacer(Modifier.height(12.dp))
                            }
                            item {
                                CardRow(
                                    items = uiState.continueReading.map { it.toMediaItem() },
                                    onClick = { navController.navigate("manga/${it.sourceId}/${it.id}") }
                                )
                                Spacer(Modifier.height(24.dp))
                            }
                        }

                        // Favourite Manga
                        if (uiState.favoriteManga.isNotEmpty()) {
                            item {
                                SectionTitle("Favourite Manga")
                                Spacer(Modifier.height(12.dp))
                            }
                            item {
                                CardRow(
                                    items = uiState.favoriteManga.map { it.toMediaItem() },
                                    onClick = { navController.navigate("manga/${it.sourceId}/${it.id}") }
                                )
                                Spacer(Modifier.height(24.dp))
                            }
                        }

                        // Discovery sections - Manga
                        uiState.mangaSections.take(3).forEach { section ->
                            item {
                                SectionTitle(section.title)
                                Spacer(Modifier.height(12.dp))
                            }
                            item {
                                MangaCardRow(
                                    items = section.items.take(10),
                                    onClick = { manga ->
                                        navController.navigate("manga/${section.sourceId}/${manga.id}")
                                    }
                                )
                                Spacer(Modifier.height(24.dp))
                            }
                        }
                    }
                    2 -> {
                        // MOVIES/TV SHOWS LIST
                        // Discovery sections - Movies/TV
                        uiState.videoSections.take(5).forEach { section ->
                            item {
                                SectionTitle(section.title)
                                Spacer(Modifier.height(12.dp))
                            }
                            item {
                                VideoCardRow(
                                    items = section.items.take(10),
                                    onClick = { video ->
                                        navController.navigate("video/${section.sourceId}/${video.id}")
                                    }
                                )
                                Spacer(Modifier.height(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }

@Composable
private fun GlossyTabCardWithImage(
    modifier: Modifier = Modifier,
    title: String,
    backgroundRes: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    accentColor: Color
) {
    Card(
        modifier = modifier
            .height(90.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                           else Color.Black.copy(alpha = 0.5f)
            )
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Custom background image
            AsyncImage(
                model = backgroundRes,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Glass morphism overlay - semi-transparent dark
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.65f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )

            // Top shine effect
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Selected state shine overlay with accent color
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    accentColor.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }

            // Text
            Text(
                title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Center)
            )

            // Bottom border glow with accent color
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    accentColor.copy(alpha = 0.9f),
                                    Color.Transparent
                                )
                            )
                        )
                        .align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Composable
private fun VideoCardRow(
    items: List<com.omnistream.domain.model.Video>,
    onClick: (com.omnistream.domain.model.Video) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items) { video ->
            VideoGlossyCard(video, onClick)
        }
    }
}

@Composable
private fun MangaCardRow(
    items: List<com.omnistream.domain.model.Manga>,
    onClick: (com.omnistream.domain.model.Manga) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items) { manga ->
            MangaGlossyCard(manga, onClick)
        }
    }
}

@Composable
private fun VideoGlossyCard(
    video: com.omnistream.domain.model.Video,
    onClick: (com.omnistream.domain.model.Video) -> Unit
) {
    Column(
        modifier = Modifier
            .width(135.dp)
            .clickable { onClick(video) }
    ) {
        Card(
            modifier = Modifier
                .width(135.dp)
                .height(195.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    ambientColor = Color.Black.copy(alpha = 0.6f)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = video.posterUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                ),
                                startY = 80f
                            )
                        )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = video.title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun MangaGlossyCard(
    manga: com.omnistream.domain.model.Manga,
    onClick: (com.omnistream.domain.model.Manga) -> Unit
) {
    Column(
        modifier = Modifier
            .width(135.dp)
            .clickable { onClick(manga) }
    ) {
        Card(
            modifier = Modifier
                .width(135.dp)
                .height(195.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    ambientColor = Color.Black.copy(alpha = 0.6f)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = manga.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                ),
                                startY = 80f
                            )
                        )
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = manga.title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

@Composable
private fun CardRow(
    items: List<MediaItem>,
    onClick: (MediaItem) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(items) { item ->
            GlossyCard(item, onClick)
        }
    }
}

@Composable
private fun GlossyCard(
    item: MediaItem,
    onClick: (MediaItem) -> Unit
) {
    Column(
        modifier = Modifier
            .width(135.dp)
            .clickable { onClick(item) }
    ) {
        // Card with MASSIVE shadow and glossy effects
        Card(
            modifier = Modifier
                .width(135.dp)
                .height(195.dp)
                .shadow(
                    elevation = 16.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    ambientColor = Color.Black.copy(alpha = 0.6f)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Cover image
                AsyncImage(
                    model = item.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Glass shine effect on top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = 0.15f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Gradient overlay for text readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.7f)
                                ),
                                startY = 80f
                            )
                        )
                )

                // Progress indicator
                if (item.progress > 0f) {
                    LinearProgressIndicator(
                        progress = { item.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Title
        Text(
            text = item.title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp
        )

        // Subtitle
        item.subtitle?.let {
            Text(
                text = it,
                color = Color.Gray,
                fontSize = 11.sp,
                maxLines = 1
            )
        }
    }
}

data class MediaItem(
    val id: String,
    val sourceId: String,
    val title: String,
    val coverUrl: String?,
    val subtitle: String?,
    val progress: Float = 0f
)

private fun com.omnistream.data.local.WatchHistoryEntity.toMediaItem(): MediaItem {
    val progressPercent = if (totalDuration > 0) {
        progressPosition.toFloat() / totalDuration.toFloat()
    } else {
        progressPercentage
    }

    val subtitle = when (contentType) {
        "manga" -> if (totalChapters > 0) "Ch. ${chapterIndex + 1}/$totalChapters" else "Ch. ${chapterIndex + 1}"
        else -> if (totalChapters > 0) "Ep. ${chapterIndex + 1}/$totalChapters" else "Ep. ${chapterIndex + 1}"
    }

    return MediaItem(
        id = contentId,
        sourceId = sourceId,
        title = title ?: "Unknown",
        coverUrl = coverUrl,
        subtitle = subtitle,
        progress = progressPercent
    )
}
