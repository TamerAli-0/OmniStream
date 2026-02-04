package com.omnistream.ui.auth

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    onSuccess: () -> Unit = { navController.popBackStack() }
) {
    val context = LocalContext.current
    val authManager = remember { AniListAuthManager(context) }
    val httpClient = remember { com.omnistream.core.network.OmniHttpClient() }
    val aniListApi = remember { AniListApi(httpClient, authManager) }
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
                        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                            super.onPageStarted(view, url, favicon)
                            android.util.Log.d("AniListLogin", "Page started loading: $url")
                        }

                        override fun onReceivedError(
                            view: WebView?,
                            request: android.webkit.WebResourceRequest?,
                            error: android.webkit.WebResourceError?
                        ) {
                            super.onReceivedError(view, request, error)
                            android.util.Log.e("AniListLogin", "WebView error: ${error?.description}")
                        }

                        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                            android.util.Log.d("AniListLogin", "URL intercepted: $url")

                            if (url.startsWith(AniListAuthManager.REDIRECT_URI)) {
                                // Extract token from URL fragment (after # or ?)
                                val token = when {
                                    url.contains("#access_token=") ->
                                        url.substringAfter("#access_token=").substringBefore("&")
                                    url.contains("?access_token=") ->
                                        url.substringAfter("?access_token=").substringBefore("&")
                                    url.contains("access_token=") ->
                                        url.substringAfter("access_token=").substringBefore("&")
                                    else -> ""
                                }

                                android.util.Log.d("AniListLogin", "Extracted token: ${token.take(10)}...")

                                if (token.isNotEmpty() && token != url) {
                                    authManager.saveAccessToken(token)

                                    // Fetch user info
                                    scope.launch {
                                        try {
                                            val user = aniListApi.getCurrentUser()
                                            if (user != null) {
                                                authManager.saveUserInfo(user.id, user.name, user.avatar)
                                                android.util.Log.d("AniListLogin", "User info saved: ${user.name}")
                                            }
                                            onSuccess()
                                        } catch (e: Exception) {
                                            android.util.Log.e("AniListLogin", "Error fetching user info", e)
                                            // Still call onSuccess since we have the token
                                            onSuccess()
                                        }
                                    }
                                }
                                return true
                            }
                            return false
                        }
                    }

                    val authUrl = authManager.getAuthUrl()
                    android.util.Log.d("AniListLogin", "Loading OAuth URL: $authUrl")
                    loadUrl(authUrl)
                }
            }
        )
    }
}
