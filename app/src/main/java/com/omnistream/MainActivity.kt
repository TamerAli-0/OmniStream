package com.omnistream

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
import com.omnistream.ui.MainViewModel
import com.omnistream.ui.MainViewModel.StartDestination
import com.omnistream.ui.navigation.OmniNavigation
import com.omnistream.ui.theme.AppColorScheme
import com.omnistream.ui.theme.DarkModeOption
import com.omnistream.ui.theme.OmniStreamTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

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
}
