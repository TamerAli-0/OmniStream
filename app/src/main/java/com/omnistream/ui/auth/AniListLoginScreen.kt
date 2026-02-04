package com.omnistream.ui.auth

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.omnistream.data.anilist.AniListApi
import com.omnistream.data.anilist.AniListAuthManager
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AniListLoginScreen(
    navController: NavController,
    authManager: AniListAuthManager,
    aniListApi: AniListApi,
    onSuccess: () -> Unit
) {
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connect AniList") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }
            )
        }
    ) { padding ->
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                            if (url.startsWith(AniListAuthManager.REDIRECT_URI)) {
                                // Extract token from URL fragment
                                val token = url.substringAfter("access_token=")
                                    .substringBefore("&")

                                if (token.isNotEmpty()) {
                                    authManager.saveAccessToken(token)

                                    // Fetch user info
                                    scope.launch {
                                        val user = aniListApi.getCurrentUser()
                                        if (user != null) {
                                            authManager.saveUserInfo(user.id, user.name, user.avatar)
                                        }
                                        onSuccess()
                                        navController.popBackStack()
                                    }
                                }
                                return true
                            }
                            return false
                        }
                    }

                    loadUrl(authManager.getAuthUrl())
                }
            }
        )
    }
}
