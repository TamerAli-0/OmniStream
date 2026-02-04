package com.omnistream.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.omnistream.data.anilist.AniListAuthManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenSaikou(
    navController: NavController,
    authManager: AniListAuthManager,
    onLogout: () -> Unit
) {
    val isAniListConnected = authManager.isLoggedIn()
    val anilistUsername = authManager.getUsername()
    val anilistAvatar = authManager.getAvatar()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Account Section
            item {
                SectionTitle("Account")
            }

            // AniList Connection
            item {
                if (isAniListConnected && anilistUsername != null) {
                    // Connected state
                    SettingCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Avatar
                            if (anilistAvatar != null) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    AsyncImage(
                                        model = anilistAvatar,
                                        contentDescription = "AniList Avatar"
                                    )
                                }
                            } else {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "AniList Connected",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    anilistUsername,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }

                            TextButton(onClick = {
                                authManager.logout()
                            }) {
                                Text("Disconnect")
                            }
                        }
                    }
                } else {
                    // Not connected state
                    SettingItem(
                        icon = Icons.Default.Link,
                        title = "Connect AniList",
                        subtitle = "Sync your anime & manga progress",
                        onClick = {
                            navController.navigate("anilist_login")
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Appearance Section
            item {
                SectionTitle("Appearance")
            }

            item {
                SettingItem(
                    icon = Icons.Default.Palette,
                    title = "Theme",
                    subtitle = "System default",
                    onClick = { /* TODO: Theme selector */ }
                )
            }

            item {
                SettingItem(
                    icon = Icons.Default.ColorLens,
                    title = "Accent Color",
                    subtitle = "Pink (Saikou style)",
                    onClick = { /* TODO: Color picker */ }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Player Section
            item {
                SectionTitle("Player")
            }

            item {
                SettingItem(
                    icon = Icons.Default.HighQuality,
                    title = "Default Quality",
                    subtitle = "Auto",
                    onClick = { /* TODO: Quality selector */ }
                )
            }

            item {
                SettingToggle(
                    icon = Icons.Default.SkipNext,
                    title = "Auto Skip Intro",
                    subtitle = "Skip detected intro sequences",
                    checked = false,
                    onCheckedChange = { /* TODO */ }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Reader Section
            item {
                SectionTitle("Reader")
            }

            item {
                SettingItem(
                    icon = Icons.Default.ChromeReaderMode,
                    title = "Default Reading Mode",
                    subtitle = "Vertical Continuous",
                    onClick = { /* TODO: Reading mode selector */ }
                )
            }

            item {
                SettingToggle(
                    icon = Icons.Default.WbSunny,
                    title = "Keep Screen On",
                    subtitle = "Prevent screen from sleeping while reading",
                    checked = true,
                    onCheckedChange = { /* TODO */ }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Downloads Section
            item {
                SectionTitle("Downloads")
            }

            item {
                SettingItem(
                    icon = Icons.Default.Folder,
                    title = "Download Location",
                    subtitle = "Internal Storage",
                    onClick = { /* TODO: Folder picker */ }
                )
            }

            item {
                SettingItem(
                    icon = Icons.Default.HighQuality,
                    title = "Download Quality",
                    subtitle = "1080p",
                    onClick = { /* TODO: Quality selector */ }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // About Section
            item {
                SectionTitle("About")
            }

            item {
                SettingItem(
                    icon = Icons.Default.Info,
                    title = "App Version",
                    subtitle = "1.0.0",
                    onClick = { /* TODO: Check for updates */ }
                )
            }

            item {
                SettingItem(
                    icon = Icons.Default.Code,
                    title = "Open Source Licenses",
                    subtitle = "View licenses",
                    onClick = { /* TODO: Licenses screen */ }
                )
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Logout
            item {
                SettingItem(
                    icon = Icons.Default.ExitToApp,
                    title = "Logout",
                    subtitle = "Sign out of your account",
                    onClick = onLogout,
                    destructive = true
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun SettingCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        content()
    }
}

@Composable
private fun SettingItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
private fun SettingToggle(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}
