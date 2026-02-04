package com.omnistream.ui.home

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

    Scaffold(
        containerColor = Color(0xFF0a0a0a),
        topBar = {
            // Top bar with username and settings
            Surface(
                color = Color(0xFF121212),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "brahmkshatriya",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Chapters Read 1789",
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
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Glossy tabs ANIME LIST / MANGA LIST
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1a1a1a),
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    Box(
                        Modifier
                            .tabIndicatorOffset(tabPositions[selectedTab])
                            .height(3.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 }
                ) {
                    Text(
                        "ANIME LIST",
                        modifier = Modifier.padding(vertical = 16.dp),
                        fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 }
                ) {
                    Text(
                        "MANGA LIST",
                        modifier = Modifier.padding(vertical = 16.dp),
                        fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }
            }

            // Content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 20.dp)
            ) {
                when (selectedTab) {
                    0 -> {
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
                    }
                    1 -> {
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
                            }
                        }
                    }
                }
            }
        }
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
            .width(130.dp)
            .clickable { onClick(item) }
    ) {
        // Card with massive shadow and elevation
        Card(
            modifier = Modifier
                .width(130.dp)
                .height(185.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(12.dp),
                    spotColor = Color.Black.copy(alpha = 0.5f)
                ),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1a1a1a))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Cover image
                AsyncImage(
                    model = item.coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Gradient overlay
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
