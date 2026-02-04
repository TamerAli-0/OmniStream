package com.omnistream.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Glossy gradient top bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1a1a2e),
                                    Color(0xFF0f0f1e)
                                )
                            )
                        )
                        .padding(vertical = 12.dp, horizontal = 16.dp)
                ) {
                    Text(
                        "OmniStream",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterStart)
                    )
                    IconButton(
                        onClick = { navController.navigate("settings") },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(Icons.Default.Settings, "Settings", tint = Color.White)
                    }
                }
            }

            // Glossy tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1a1a2e),
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    Box(
                        Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab])
                            .height(4.dp)
                            .padding(horizontal = 32.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.secondary
                                    )
                                ),
                                RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                            )
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "ANIME LIST",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "MANGA LIST",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                )
            }

            // Content
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // ANIME TAB
                        if (uiState.continueWatching.isNotEmpty()) {
                            item { GlossySectionHeader("Continue Watching") }
                            item {
                                GlossyMediaRow(
                                    items = uiState.continueWatching.map { it.toMediaItem() },
                                    onItemClick = { navController.navigate("video/${it.sourceId}/${it.id}") }
                                )
                            }
                        }

                        if (uiState.favoriteAnime.isNotEmpty()) {
                            item { GlossySectionHeader("Favourite Anime") }
                            item {
                                GlossyMediaRow(
                                    items = uiState.favoriteAnime.map { it.toMediaItem() },
                                    onItemClick = { navController.navigate("video/${it.sourceId}/${it.id}") }
                                )
                            }
                        }

                        if (uiState.trendingAnime.isNotEmpty()) {
                            item { GlossySectionHeader("Trending Now") }
                            item {
                                GlossyMediaRow(
                                    items = uiState.trendingAnime.map { it.toMediaItem() },
                                    onItemClick = { navController.navigate("video/${it.sourceId}/${it.id}") }
                                )
                            }
                        }
                    }
                    1 -> {
                        // MANGA TAB
                        if (uiState.continueReading.isNotEmpty()) {
                            item { GlossySectionHeader("Continue Reading") }
                            item {
                                GlossyMediaRow(
                                    items = uiState.continueReading.map { it.toMediaItem() },
                                    onItemClick = { navController.navigate("manga/${it.sourceId}/${it.id}") }
                                )
                            }
                        }

                        if (uiState.favoriteManga.isNotEmpty()) {
                            item { GlossySectionHeader("Favourite Manga") }
                            item {
                                GlossyMediaRow(
                                    items = uiState.favoriteManga.map { it.toMediaItem() },
                                    onItemClick = { navController.navigate("manga/${it.sourceId}/${it.id}") }
                                )
                            }
                        }

                        if (uiState.trendingManga.isNotEmpty()) {
                            item { GlossySectionHeader("Trending Now") }
                            item {
                                GlossyMediaRow(
                                    items = uiState.trendingManga.map { it.toMediaItem() },
                                    onItemClick = { navController.navigate("manga/${it.sourceId}/${it.id}") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GlossySectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = Color.White,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun GlossyMediaRow(
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            GlossyMediaCard(item, onItemClick)
        }
    }
}

@Composable
private fun GlossyMediaCard(
    item: MediaItem,
    onItemClick: (MediaItem) -> Unit
) {
    Column(
        modifier = Modifier
            .width(140.dp)
            .clickable { onItemClick(item) }
    ) {
        // Glossy card with shadow and elevation
        Surface(
            modifier = Modifier
                .width(140.dp)
                .height(200.dp)
                .shadow(8.dp, RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 4.dp
        ) {
            Box {
                AsyncImage(
                    model = item.coverUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Gradient overlay at bottom
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.8f)
                                ),
                                startY = 100f
                            )
                        )
                )

                // Progress bar
                if (item.progress > 0f) {
                    LinearProgressIndicator(
                        progress = { item.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.2f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(140.dp)
        )

        // Subtitle
        if (item.subtitle != null) {
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
        "manga" -> if (totalChapters > 0) {
            "Chapter ${chapterIndex + 1}/$totalChapters"
        } else {
            "Chapter ${chapterIndex + 1}"
        }
        else -> if (totalChapters > 0) {
            "Episode ${chapterIndex + 1}/$totalChapters"
        } else {
            "Episode ${chapterIndex + 1}"
        }
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
