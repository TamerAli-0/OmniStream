# OmniStream

A modern Android app for managing and tracking your anime and manga collection with AniList integration.

## Download

**[Download Latest APK](https://github.com/TamerAli-0/OmniStream/releases/latest)**

> Requires Android 8.0 (API 26) or higher

## Features

### 📱 Library Management
- Organize your anime and manga collection
- Track your reading and watching progress
- Manage your personal watchlists and reading lists
- Beautiful Material You UI with customizable themes

### 🔄 AniList Integration
- Seamless sync with your AniList account
- Automatic progress tracking
- Two-way synchronization with AniList lists
- Update your lists from anywhere

### 📖 Progress Tracking
- Keep track of episodes watched and chapters read
- Resume from where you left off
- Mark favorites and completed items
- View your watch and read history

### 🎨 Customization
- Multiple reading modes (Vertical, LTR, RTL)
- Theme options (Light, Dark, AMOLED)
- Accent color selection (Pink, Purple, Blue, Green, Orange, Red)
- Layout customization (Card, Compact, List views)
- First-time setup wizard for personalization

### 🔐 Privacy & Security
- Encrypted credential storage using Android EncryptedSharedPreferences
- Optional app lock with biometric authentication
- Incognito mode for private browsing
- Local data backup and restore
- No data collection or analytics

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Clean Architecture |
| DI | Hilt (Dagger) |
| Database | Room |
| Networking | OkHttp + GraphQL |
| Image Loading | Coil |
| Async | Kotlin Coroutines + Flow |
| Storage | DataStore + EncryptedSharedPreferences |
| Background | WorkManager |

## Setup

1. Clone the repository
   ```bash
   git clone https://github.com/TamerAli-0/OmniStream.git
   ```
2. Register your app at [AniList Developers](https://anilist.co/settings/developer)
3. Add your AniList Client ID to `AniListAuthManager.kt`
4. Open in Android Studio (Hedgehog or newer)
5. Sync Gradle and run on device/emulator

## Building from Source

```bash
./gradlew assembleDebug
```

For release build:
```bash
./gradlew assembleRelease
```

## Installation

1. Download the APK from [Releases](https://github.com/TamerAli-0/OmniStream/releases/latest)
2. Enable "Install from unknown sources" in Android settings
3. Open the APK and install

## Requirements

- Android 8.0 (API 26) or higher
- AniList account (optional but recommended for sync features)

## Disclaimer

OmniStream is a personal library management and tracking tool designed to help users organize their anime and manga collections. Users are responsible for ensuring they have appropriate rights to any content they manage through this application.

This application uses the AniList API for tracking and list management. It is not affiliated with or endorsed by AniList.

## Privacy

OmniStream does not collect any personal data or analytics. All authentication tokens are stored locally using Android's EncryptedSharedPreferences. Your credentials never leave your device, and we don't have any servers.

## License

This project is for educational and portfolio purposes only. All rights to anime and manga titles, artwork, and related content belong to their respective copyright holders.

## Contributing

This is a personal project. Bug reports and feature suggestions are welcome via Issues.

## Acknowledgments

- UI design inspired by modern anime/manga tracking apps
- Built with Jetpack Compose
- Powered by AniList GraphQL API
- Icons from Material Design Icons

---

Built by [Tamer Altaweel](https://github.com/TamerAli-0)
