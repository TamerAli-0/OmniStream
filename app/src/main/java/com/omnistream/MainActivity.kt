package com.omnistream

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.omnistream.data.anilist.AniListApi
import com.omnistream.data.anilist.AniListAuthManager
import com.omnistream.ui.MainViewModel
import com.omnistream.ui.MainViewModel.StartDestination
import com.omnistream.ui.navigation.OmniNavigation
import com.omnistream.ui.theme.AppColorScheme
import com.omnistream.ui.theme.DarkModeOption
import com.omnistream.ui.theme.OmniStreamTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    @Inject
    lateinit var authManager: AniListAuthManager

    @Inject
    lateinit var aniListApi: AniListApi

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Handle deep link if coming from AniList OAuth
        handleDeepLink(intent)

        enableEdgeToEdge()

        // Configure edge-to-edge with auto-hiding system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.apply {
            // Make system bars transparent and overlay content
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            // Show bars by default (will hide in fullscreen content like video/reader)
            show(WindowInsetsCompat.Type.systemBars())
        }

        setContent {
            val colorSchemeKey by mainViewModel.colorScheme.collectAsState()
            val darkModeKey by mainViewModel.darkMode.collectAsState()

            val appColorScheme = AppColorScheme.fromKey(colorSchemeKey)
            val darkModeOption = DarkModeOption.fromKey(darkModeKey)
            val isDark = when (darkModeOption) {
                DarkModeOption.DARK -> true
                DarkModeOption.LIGHT -> false
                DarkModeOption.SYSTEM -> isSystemInDarkTheme()
            }

            OmniStreamTheme(darkTheme = isDark, appColorScheme = appColorScheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val startDest by mainViewModel.startDestination.collectAsState()

                    when (startDest) {
                        is StartDestination.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator()
                            }
                        }
                        is StartDestination.AccessGate -> OmniNavigation(startDestination = "access_gate")
                        is StartDestination.Login -> OmniNavigation(startDestination = "login")
                        is StartDestination.Home -> OmniNavigation(startDestination = "home")
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data
        android.util.Log.d("MainActivity", "Deep link received: $data")

        if (data != null && data.scheme == "omnistream" && data.host == "anilist-callback") {
            // Extract access token from fragment (after #)
            val fragment = data.fragment ?: data.query ?: ""
            android.util.Log.d("MainActivity", "Fragment/Query: $fragment")

            val token = when {
                fragment.contains("access_token=") -> {
                    fragment.substringAfter("access_token=").substringBefore("&")
                }
                else -> null
            }

            android.util.Log.d("MainActivity", "Extracted token: ${token?.take(10)}...")

            if (!token.isNullOrEmpty()) {
                // Save token
                authManager.saveAccessToken(token)
                android.util.Log.d("MainActivity", "Token saved successfully")

                // Fetch and save user info
                lifecycleScope.launch {
                    try {
                        val user = aniListApi.getCurrentUser()
                        if (user != null) {
                            authManager.saveUserInfo(user.id, user.name, user.avatar)
                            android.util.Log.d("MainActivity", "User info saved: ${user.name}")
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("MainActivity", "Failed to fetch user info", e)
                    }
                }

                // Show success message
                android.widget.Toast.makeText(
                    this,
                    "AniList connected successfully!",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } else {
                android.util.Log.e("MainActivity", "Failed to extract token from: $fragment")
            }
        }
    }
}
