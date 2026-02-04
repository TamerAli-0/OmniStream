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
fun HomeScreenSaikou(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "OmniStream",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                actions = {
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Default.Settings, "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab selector for ANIME / MANGA
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            "ANIME",
                            fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            "MANGA",
                            fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }

            // Content based on selected tab
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                when (selectedTab) {
                    0 -> {
                        // ANIME TAB
                        // Continue Watching section
                        if (uiState.continueWatching.isNotEmpty()) {
                            item {
                                SectionHeader("Continue Watching")
                            }
                            item {
                                HorizontalMediaList(
                                    items = uiState.continueWatching.map { it.toMediaItem() },
                                    onItemClick = { item ->
                                        navController.navigate("video/${item.sourceId}/${item.id}")
                                    }
                                )
                            }
                        }

                        // Favourite Anime section
                        if (uiState.favoriteAnime.isNotEmpty()) {
                            item {
                                SectionHeader("Favourite Anime")
                            }
                            item {
                                HorizontalMediaList(
                                    items = uiState.favoriteAnime.map { it.toMediaItem() },
                                    onItemClick = { item ->
                                        navController.navigate("video/${item.sourceId}/${item.id}")
                                    }
                                )
                            }
                        }

                        // Trending Anime
                        if (uiState.trendingAnime.isNotEmpty()) {
                            item {
                                SectionHeader("Trending Now")
                            }
                            item {
                                HorizontalMediaList(
                                    items = uiState.trendingAnime.map { it.toMediaItem() },
                                    onItemClick = { item ->
                                        navController.navigate("video/${item.sourceId}/${item.id}")
                                    }
                                )
                            }
                        }
                    }
                    1 -> {
                        // MANGA TAB
                        // Continue Reading section
                        if (uiState.continueReading.isNotEmpty()) {
                            item {
                                SectionHeader("Continue Reading")
                            }
                            item {
                                HorizontalMediaList(
                                    items = uiState.continueReading.map { it.toMediaItem() },
                                    onItemClick = { item ->
                                        navController.navigate("manga/${item.sourceId}/${item.id}")
                                    }
                                )
                            }
                        }

                        // Favourite Manga section
                        if (uiState.favoriteManga.isNotEmpty()) {
                            item {
                                SectionHeader("Favourite Manga")
                            }
                            item {
                                HorizontalMediaList(
                                    items = uiState.favoriteManga.map { it.toMediaItem() },
                                    onItemClick = { item ->
                                        navController.navigate("manga/${item.sourceId}/${item.id}")
                                    }
                                )
                            }
                        }

                        // Trending Manga
                        if (uiState.trendingManga.isNotEmpty()) {
                            item {
                                SectionHeader("Trending Now")
                            }
                            item {
                                HorizontalMediaList(
                                    items = uiState.trendingManga.map { it.toMediaItem() },
                                    onItemClick = { item ->
                                        navController.navigate("manga/${item.sourceId}/${item.id}")
                                    }
                                )
                            }
                        }
                    }
                }

                // Empty state
                if ((selectedTab == 0 && uiState.continueWatching.isEmpty() && uiState.favoriteAnime.isEmpty() && uiState.trendingAnime.isEmpty()) ||
                    (selectedTab == 1 && uiState.continueReading.isEmpty() && uiState.favoriteManga.isEmpty() && uiState.trendingManga.isEmpty())) {
                    item {
                        EmptyState(
                            icon = if (selectedTab == 0) Icons.Default.Tv else Icons.Default.Book,
                            message = if (selectedTab == 0) "Start watching anime!" else "Start reading manga!",
                            onActionClick = {
                                navController.navigate(if (selectedTab == 0) "browse" else "search")
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
private fun HorizontalMediaList(
    items: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items) { item ->
            MediaCard(
                item = item,
                onClick = { onItemClick(item) }
            )
        }
    }
}

@Composable
private fun MediaCard(
    item: MediaItem,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(120.dp)
            .clickable(onClick = onClick)
    ) {
        // Cover image
        Surface(
            modifier = Modifier
                .width(120.dp)
                .height(170.dp),
            shape = RoundedCornerShape(12.dp),
            shadowElevation = 4.dp
        ) {
            AsyncImage(
                model = item.coverUrl,
                contentDescription = item.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Progress overlay if watching/reading
            if (item.progress > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                ) {
                    LinearProgressIndicator(
                        progress = { item.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.White.copy(alpha = 0.3f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Title
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(120.dp)
        )

        // Subtitle (episode/chapter info)
        if (item.subtitle != null) {
            Text(
                text = item.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    message: String,
    onActionClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Button(onClick = onActionClick) {
            Text("Explore")
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

// Extension function to convert WatchHistoryEntity to MediaItem
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
