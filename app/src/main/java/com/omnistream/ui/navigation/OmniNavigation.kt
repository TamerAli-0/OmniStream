package com.omnistream.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.omnistream.ui.auth.AccessGateScreen
import com.omnistream.ui.auth.LoginScreen
import com.omnistream.ui.auth.RegisterScreen
import com.omnistream.ui.browse.BrowseScreen
import com.omnistream.ui.detail.MangaDetailScreen
import com.omnistream.ui.detail.VideoDetailScreen
import com.omnistream.ui.downloads.DownloadsScreen
import com.omnistream.ui.home.HomeScreen
import com.omnistream.ui.library.LibraryScreen
import com.omnistream.ui.player.PlayerScreen
import com.omnistream.ui.reader.ReaderScreen
import com.omnistream.ui.reader.SaikouReaderScreen
import com.omnistream.ui.search.SearchScreen
import com.omnistream.ui.settings.SettingsScreen

/**
 * Navigation routes
 */
sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Browse : Screen(
        route = "browse",
        title = "Browse",
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore
    )

    data object Search : Screen(
        route = "search",
        title = "Search",
        selectedIcon = Icons.Filled.Search,
        unselectedIcon = Icons.Outlined.Search
    )

    data object Library : Screen(
        route = "library",
        title = "Library",
        selectedIcon = Icons.Filled.VideoLibrary,
        unselectedIcon = Icons.Outlined.VideoLibrary
    )

    data object Downloads : Screen(
        route = "downloads",
        title = "Downloads",
        selectedIcon = Icons.Filled.Download,
        unselectedIcon = Icons.Outlined.Download
    )
}

// Bottom nav items
val bottomNavItems = listOf(
    Screen.Home,
    Screen.Browse,
    Screen.Search,
    Screen.Library,
    Screen.Downloads
)

// Auth routes (no bottom nav)
private val authRoutes = setOf("access_gate", "login", "register")

@Composable
fun OmniNavigation(
    startDestination: String = Screen.Home.route,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route

    // Show bottom nav only on main tabs (not auth screens)
    val showBottomNav = currentRoute !in authRoutes && bottomNavItems.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == screen.route
                        } == true

                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                    contentDescription = screen.title
                                )
                            },
                            label = { Text(screen.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
            enterTransition = {
                fadeIn(animationSpec = tween(200))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(200))
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(200))
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(200))
            }
        ) {
            // --- Auth screens ---
            composable("access_gate") {
                AccessGateScreen(
                    onUnlocked = {
                        navController.navigate("login") {
                            popUpTo("access_gate") { inclusive = true }
                        }
                    }
                )
            }

            composable("login") {
                LoginScreen(
                    onLoginSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onNavigateToRegister = {
                        navController.navigate("register")
                    }
                )
            }

            composable("register") {
                RegisterScreen(
                    onRegisterSuccess = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo("login") { inclusive = true }
                        }
                    },
                    onNavigateToLogin = {
                        navController.popBackStack()
                    }
                )
            }

            // --- Main tabs ---
            composable(Screen.Home.route) {
                HomeScreen(navController = navController)
            }

            composable(Screen.Browse.route) {
                BrowseScreen(navController = navController)
            }

            composable(Screen.Search.route) {
                SearchScreen(navController = navController)
            }

            composable(Screen.Library.route) {
                LibraryScreen(navController = navController)
            }

            composable(Screen.Downloads.route) {
                DownloadsScreen(navController = navController)
            }

            // Settings
            composable("settings") {
                SettingsScreen(
                    navController = navController,
                    onLogout = {
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            // Manga detail
            composable(
                route = "manga/{sourceId}/{mangaId}",
                arguments = listOf(
                    navArgument("sourceId") { type = NavType.StringType },
                    navArgument("mangaId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                MangaDetailScreen(
                    navController = navController,
                    sourceId = backStackEntry.arguments?.getString("sourceId") ?: "",
                    mangaId = backStackEntry.arguments?.getString("mangaId") ?: ""
                )
            }

            // Video detail
            composable(
                route = "video/{sourceId}/{videoId}",
                arguments = listOf(
                    navArgument("sourceId") { type = NavType.StringType },
                    navArgument("videoId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                VideoDetailScreen(
                    navController = navController,
                    sourceId = backStackEntry.arguments?.getString("sourceId") ?: "",
                    videoId = backStackEntry.arguments?.getString("videoId") ?: ""
                )
            }

            // Manga reader
            composable(
                route = "reader/{sourceId}/{mangaId}/{chapterId}/{title}/{coverUrl}",
                arguments = listOf(
                    navArgument("sourceId") { type = NavType.StringType },
                    navArgument("mangaId") { type = NavType.StringType },
                    navArgument("chapterId") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType; defaultValue = "" },
                    navArgument("coverUrl") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                SaikouReaderScreen(
                    navController = navController,
                    sourceId = backStackEntry.arguments?.getString("sourceId") ?: "",
                    mangaId = backStackEntry.arguments?.getString("mangaId") ?: "",
                    chapterId = backStackEntry.arguments?.getString("chapterId") ?: ""
                )
            }

            // Video player
            composable(
                route = "player/{sourceId}/{videoId}/{episodeId}/{title}/{coverUrl}",
                arguments = listOf(
                    navArgument("sourceId") { type = NavType.StringType },
                    navArgument("videoId") { type = NavType.StringType },
                    navArgument("episodeId") { type = NavType.StringType },
                    navArgument("title") { type = NavType.StringType; defaultValue = "" },
                    navArgument("coverUrl") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                PlayerScreen(
                    navController = navController,
                    sourceId = backStackEntry.arguments?.getString("sourceId") ?: "",
                    videoId = backStackEntry.arguments?.getString("videoId") ?: "",
                    episodeId = backStackEntry.arguments?.getString("episodeId") ?: ""
                )
            }
        }
    }
}
