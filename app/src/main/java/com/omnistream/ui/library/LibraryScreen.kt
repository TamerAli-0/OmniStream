package com.omnistream.ui.library

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch

/**
 * Library screen inspired by Kotatsu's shelf/favorites system.
 * Shows user's saved manga and videos organized by categories.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    navController: NavController
) {
    // Kotatsu-style categories
    val categories = listOf(
        LibraryCategory("Favorites", 12),
        LibraryCategory("Reading", 5),
        LibraryCategory("Plan to Read", 8),
        LibraryCategory("Completed", 23),
        LibraryCategory("On Hold", 3)
    )

    val pagerState = rememberPagerState(pageCount = { categories.size })
    val coroutineScope = rememberCoroutineScope()

    var isGridView by remember { mutableStateOf(true) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = {
                Text(
                    "Library",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            },
            actions = {
                IconButton(onClick = { /* Search library */ }) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
                IconButton(onClick = { isGridView = !isGridView }) {
                    Icon(
                        if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                        contentDescription = "Toggle view"
                    )
                }
                IconButton(onClick = { /* Filter/sort */ }) {
                    Icon(Icons.Default.FilterList, contentDescription = "Filter")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )

        // Category tabs (Kotatsu style)
        TabRow(
            selectedTabIndex = pagerState.currentPage,
            containerColor = Color.Transparent
        ) {
            categories.forEachIndexed { index, category ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    },
                    text = {
                        BadgedBox(
                            badge = {
                                if (category.count > 0) {
                                    Badge {
                                        Text(category.count.toString())
                                    }
                                }
                            }
                        ) {
                            Text(category.name)
                        }
                    }
                )
            }
        }

        // Paged content
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            if (categories[page].count > 0) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = if (isGridView) 110.dp else 300.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(categories[page].count) { index ->
                        if (isGridView) {
                            LibraryGridCard(
                                title = "Saved Item ${index + 1}",
                                progress = 0.1f + kotlin.random.Random.nextFloat() * 0.8f,
                                unreadCount = (0..10).random(),
                                onClick = { }
                            )
                        } else {
                            LibraryListCard(
                                title = "Saved Item ${index + 1}",
                                source = "MangaDex",
                                lastRead = "Chapter 45",
                                unreadCount = (0..10).random(),
                                onClick = { }
                            )
                        }
                    }
                }
            } else {
                // Empty state
                EmptyLibraryState(categoryName = categories[page].name)
            }
        }
    }
}

@Composable
private fun LibraryGridCard(
    title: String,
    progress: Float,
    unreadCount: Int,
    imageUrl: String? = null,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column {
            Box {
                // Cover
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.7f)
                        .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    if (imageUrl != null) {
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                // Unread badge (Kotatsu style - corner badge)
                if (unreadCount > 0) {
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            // Title
            Text(
                text = title,
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun LibraryListCard(
    title: String,
    source: String,
    lastRead: String,
    unreadCount: Int,
    imageUrl: String? = null,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Thumbnail
            Surface(
                modifier = Modifier
                    .size(width = 60.dp, height = 85.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = source,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = lastRead,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Unread badge
            if (unreadCount > 0) {
                Badge {
                    Text(unreadCount.toString())
                }
            }
        }
    }
}

@Composable
private fun EmptyLibraryState(categoryName: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.BookmarkBorder,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
            Text(
                "No items in $categoryName",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
            Text(
                "Add manga or videos to see them here",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

private data class LibraryCategory(
    val name: String,
    val count: Int
)
