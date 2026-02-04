package com.omnistream.ui.settings

import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.omnistream.data.anilist.AniListAuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Comprehensive settings screen combining Saikou + Kotatsu features:
 * - Account (AniList, MAL, Kitsu, Shikimori)
 * - Appearance (Theme, Colors, AMOLED, Grid Size)
 * - Player (Quality, Auto-skip, Gestures, Speed)
 * - Reader (Reading Mode, Color Filter, Page Turn, Scaling, Preloading)
 * - Downloads (Location, Quality, Concurrent, Network)
 * - Security (Password, Fingerprint, Incognito)
 * - Backup & Sync
 * - Advanced (Data Saver, Cache, Update Check)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val authManager = viewModel.authManager
    val isAniListConnected = authManager.isLoggedIn()
    val anilistUsername = authManager.getUsername()
    val anilistAvatar = authManager.getAvatar()

    // Theme state
    val colorScheme by viewModel.colorScheme.collectAsState()
    val darkMode by viewModel.darkMode.collectAsState()
    var showColorPicker by remember { mutableStateOf(false) }
    var showThemePicker by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFF0a0a0a),
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212)
                )
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
            // ==================== ACCOUNT ====================
            item { SectionTitle("Account & Tracking") }

            // AniList
            item {
                if (isAniListConnected && anilistUsername != null) {
                    SettingCard {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (anilistAvatar != null) {
                                Surface(shape = RoundedCornerShape(50), modifier = Modifier.size(48.dp)) {
                                    AsyncImage(model = anilistAvatar, contentDescription = "AniList Avatar")
                                }
                            } else {
                                Icon(Icons.Default.AccountCircle, null, Modifier.size(48.dp), MaterialTheme.colorScheme.primary)
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("AniList Connected", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                                Text(anilistUsername, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                            }

                            TextButton(onClick = { authManager.logout() }) { Text("Disconnect") }
                        }
                    }
                } else {
                    SettingItem(Icons.Default.Link, "Connect AniList", "Sync anime & manga progress", { navController.navigate("anilist_login") })
                }
            }

            item { SettingItem(Icons.Default.TrackChanges, "MyAnimeList", "Coming soon - Connect MAL account", {}) }

            // Preferred tracking service selector
            item {
                val preferredService by viewModel.preferredTrackingService.collectAsState()
                SettingItem(
                    Icons.Default.Star,
                    "Preferred Tracking Service",
                    if (preferredService == "anilist") "AniList (showing AniList stats)" else "MyAnimeList (showing MAL stats)",
                    {
                        // Toggle between anilist and mal
                        viewModel.setPreferredTrackingService(
                            if (preferredService == "anilist") "mal" else "anilist"
                        )
                    }
                )
            }

            item { SettingToggle(Icons.Default.AutoMode, "Auto-sync Progress", "Automatically sync watch/read progress", false, {}) }

            item { Spacer(Modifier.height(8.dp)) }

            // ==================== APPEARANCE ====================
            item { SectionTitle("Appearance") }

            item { SettingItem(Icons.Default.Palette, "Theme", darkMode.replaceFirstChar { it.uppercase() }, { showThemePicker = true }) }
            item { SettingItem(Icons.Default.ColorLens, "Accent Color", colorScheme.replaceFirstChar { it.uppercase() }, { showColorPicker = true }) }
            item { SettingToggle(Icons.Default.DarkMode, "AMOLED Mode", "Pure black background for OLED screens", false, {}) }
            item { SettingToggle(Icons.Default.Contrast, "Material You", "Dynamic colors from wallpaper", true, {}) }
            item { SettingItem(Icons.Default.GridView, "Grid Size", "3 columns", {}) }
            item { SettingToggle(Icons.Default.ViewCompact, "Compact View", "Smaller cards and spacing", false, {}) }

            item { Spacer(Modifier.height(8.dp)) }

            // ==================== PLAYER ====================
            item { SectionTitle("Video Player") }

            item { SettingItem(Icons.Default.HighQuality, "Default Quality", "Auto (1080p preferred)", {}) }
            item { SettingItem(Icons.Default.Speed, "Playback Speed", "1.0x", {}) }
            item { SettingToggle(Icons.Default.SkipNext, "Auto Skip Intro", "Skip detected intro sequences", false, {}) }
            item { SettingToggle(Icons.Default.SkipNext, "Auto Skip Outro", "Skip ending credits", false, {}) }
            item { SettingToggle(Icons.Default.AutoAwesome, "Auto Play Next", "Continue to next episode automatically", true, {}) }
            item { SettingItem(Icons.Default.TouchApp, "Gesture Controls", "Configure player gestures", {}) }
            item { SettingItem(Icons.Default.Timer, "Skip Times", "Intro: 85s, Outro: 90s", {}) }
            item { SettingToggle(Icons.Default.PictureInPicture, "Picture-in-Picture", "Enable PiP mode", true, {}) }
            item { SettingToggle(Icons.Default.Subtitles, "Show Subtitles", "Display subtitles by default", true, {}) }
            item { SettingItem(Icons.Default.TextFields, "Subtitle Style", "Font size, color, background", {}) }

            item { Spacer(Modifier.height(8.dp)) }

            // ==================== READER ====================
            item { SectionTitle("Manga Reader") }

            item { SettingItem(Icons.Default.ChromeReaderMode, "Default Reading Mode", "Vertical Continuous", {}) }
            item { SettingItem(Icons.Default.SwapHoriz, "Page Turn", "Tap zones", {}) }
            item { SettingToggle(Icons.Default.VolumeUp, "Volume Key Navigation", "Use volume buttons to turn pages", false, {}) }
            item { SettingItem(Icons.Default.AspectRatio, "Image Scaling", "Fit Width", {}) }
            item { SettingToggle(Icons.Default.Contrast, "Color Filter", "Adjust brightness/contrast", false, {}) }
            item { SettingToggle(Icons.Default.WbSunny, "Keep Screen On", "Prevent sleep while reading", true, {}) }
            item { SettingToggle(Icons.Default.Numbers, "Show Page Number", "Display page counter", true, {}) }
            item { SettingToggle(Icons.Default.CloudDownload, "Preload Pages", "Load next chapter in advance", true, {}) }
            item { SettingItem(Icons.Default.Cached, "Preload Amount", "Next 3 chapters", {}) }
            item { SettingToggle(Icons.Default.Compress, "Image Compression", "Reduce image quality to save data", false, {}) }

            item { Spacer(Modifier.height(8.dp)) }

            // ==================== DOWNLOADS ====================
            item { SectionTitle("Downloads") }

            item { SettingItem(Icons.Default.Folder, "Download Location", "Internal Storage/OmniStream", {}) }
            item { SettingItem(Icons.Default.HighQuality, "Video Quality", "1080p", {}) }
            item { SettingItem(Icons.Default.FormatListNumbered, "Concurrent Downloads", "3 at a time", {}) }
            item { SettingToggle(Icons.Default.Wifi, "Download over WiFi Only", "Prevent mobile data usage", true, {}) }
            item { SettingToggle(Icons.Default.BatteryChargingFull, "Download While Charging Only", "Preserve battery", false, {}) }
            item { SettingToggle(Icons.Default.Notifications, "Download Notifications", "Show progress in status bar", true, {}) }
            item { SettingToggle(Icons.Default.AutoDelete, "Auto-Delete Watched", "Remove downloads after completion", false, {}) }
            item { SettingItem(Icons.Default.Storage, "Manage Downloads", "View and delete downloads", { navController.navigate("downloads") }) }

            item { Spacer(Modifier.height(8.dp)) }

            // ==================== SECURITY ====================
            item { SectionTitle("Security & Privacy") }

            item { SettingToggle(Icons.Default.Lock, "App Lock", "Require password/fingerprint to open", false, {}) }
            item { SettingItem(Icons.Default.Fingerprint, "Biometric Authentication", "Use fingerprint or face unlock", {}) }
            item { SettingToggle(Icons.Default.VisibilityOff, "Incognito Mode", "Don't save history while active", false, {}) }
            item { SettingToggle(Icons.Default.Screenshot, "Secure Screen", "Block screenshots and screen recording", false, {}) }
            item { SettingItem(Icons.Default.DeleteForever, "Clear History", "Delete watch/read history", {}) }
            item { SettingItem(Icons.Default.DeleteSweep, "Clear Cache", "Free up storage space", {}) }

            item { Spacer(Modifier.height(8.dp)) }

            // ==================== BACKUP & SYNC ====================
            item { SectionTitle("Backup & Sync") }

            item { SettingItem(Icons.Default.Backup, "Backup Data", "Export app data", {}) }
            item { SettingItem(Icons.Default.Restore, "Restore Data", "Import from backup", {}) }
            item { SettingToggle(Icons.Default.CloudSync, "Auto Cloud Backup", "Sync data across devices", false, {}) }
            item { SettingItem(Icons.Default.Schedule, "Backup Frequency", "Daily at 3 AM", {}) }
            item { SettingItem(Icons.Default.CloudUpload, "Backup Location", "Google Drive", {}) }

            item { Spacer(Modifier.height(8.dp)) }

            // ==================== ADVANCED ====================
            item { SectionTitle("Advanced") }

            item { SettingToggle(Icons.Default.DataSaverOn, "Data Saver Mode", "Reduce data usage", false, {}) }
            item { SettingItem(Icons.Default.Cached, "Cache Size Limit", "500 MB", {}) }
            item { SettingToggle(Icons.Default.Update, "Auto-Check Updates", "Check for app updates on startup", true, {}) }
            item { SettingItem(Icons.Default.BugReport, "Debug Mode", "Enable detailed logging", {}) }
            item { SettingToggle(Icons.Default.Analytics, "Analytics", "Help improve the app", false, {}) }
            item { SettingToggle(Icons.Default.Report, "Crash Reports", "Send crash reports automatically", false, {}) }

            item { Spacer(Modifier.height(8.dp)) }

            // ==================== ABOUT ====================
            item { SectionTitle("About") }

            item { SettingItem(Icons.Default.Info, "App Version", "1.0.0", {}) }
            item { SettingItem(Icons.Default.Update, "Check for Updates", "Last checked: Just now", {}) }
            item { SettingItem(Icons.Default.Code, "Open Source Licenses", "View third-party licenses", {}) }
            item { SettingItem(Icons.Default.Policy, "Privacy Policy", "Read our privacy policy", {}) }
            item { SettingItem(Icons.Default.Description, "Terms of Service", "Read terms and conditions", {}) }
            item { SettingItem(Icons.Default.Favorite, "Support Development", "Donate or contribute", {}) }

            item { Spacer(Modifier.height(8.dp)) }

            // ==================== LOGOUT ====================
            item { SettingItem(Icons.Default.ExitToApp, "Logout", "Sign out of your account", onLogout, destructive = true) }

            item { Spacer(Modifier.height(32.dp)) }

            // Credits
            item {
                Text(
                    "Inspired by Saikou & Kotatsu",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
                )
            }
        }
    }

    // Color Picker Dialog
    if (showColorPicker) {
        ColorPickerDialog(
            currentScheme = colorScheme,
            onSchemeSelected = {
                viewModel.setColorScheme(it)
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }

    // Theme Picker Dialog
    if (showThemePicker) {
        ThemePickerDialog(
            currentMode = darkMode,
            onModeSelected = {
                viewModel.setDarkMode(it)
                showThemePicker = false
            },
            onDismiss = { showThemePicker = false }
        )
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = if (destructive)
                    MaterialTheme.colorScheme.error.copy(alpha = 0.3f)
                else
                    Color.Black.copy(alpha = 0.4f)
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1a1a1a)
        )
    ) {
        Box {
            // Glass morphism shine
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
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
                        fontWeight = FontWeight.SemiBold,
                        color = if (destructive) MaterialTheme.colorScheme.error else Color.White
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = Color.Gray
                )
            }
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.4f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1a1a1a)
        )
    ) {
        Box {
            // Glass morphism shine
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.05f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
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
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = Color.DarkGray
                    )
                )
            }
        }
    }
}

@Composable
private fun ColorPickerDialog(
    currentScheme: String,
    onSchemeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val schemes = listOf("purple", "ocean", "emerald", "sunset", "rose", "midnight", "crimson", "gold", "saikou")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Color Scheme") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                schemes.forEach { scheme ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSchemeSelected(scheme) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = scheme == currentScheme,
                            onClick = { onSchemeSelected(scheme) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(scheme.replaceFirstChar { it.uppercase() })
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun ThemePickerDialog(
    currentMode: String,
    onModeSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val modes = listOf("dark", "light", "system")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Theme") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                modes.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onModeSelected(mode) }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = mode == currentMode,
                            onClick = { onModeSelected(mode) }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(mode.replaceFirstChar { it.uppercase() })
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
