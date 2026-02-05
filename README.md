<div align="center">

# 📱 OmniStream

### Unified Android Streaming Platform for Manga & Video

[![Latest Release](https://img.shields.io/github/v/release/TamerAli-0/OmniStream?style=for-the-badge&logo=android&color=3DDC84)](https://github.com/TamerAli-0/OmniStream/releases/latest)
[![Downloads](https://img.shields.io/github/downloads/TamerAli-0/OmniStream/total?style=for-the-badge&logo=download&color=brightgreen)](https://github.com/TamerAli-0/OmniStream/releases)
[![License](https://img.shields.io/badge/License-Educational-blue?style=for-the-badge)](LICENSE)
[![Android](https://img.shields.io/badge/Android-8.0+-3DDC84?style=for-the-badge&logo=android)](https://github.com/TamerAli-0/OmniStream)

**[📥 Download Latest APK](https://github.com/TamerAli-0/OmniStream/releases/latest)** | **[🐛 Report Bug](https://github.com/TamerAli-0/OmniStream/issues)** | **[✨ Request Feature](https://github.com/TamerAli-0/OmniStream/issues)**

</div>

---

## 🌟 About

OmniStream is a modern, feature-rich Android application that brings together anime and manga content management in one unified platform. Built with Kotlin and Jetpack Compose, it offers seamless AniList integration, beautiful Material 3 design, and powerful tracking capabilities.

> **Perfect for:** Anime enthusiasts, manga readers, and collectors who want to organize and track their content in style.

---

## ✨ Features

<table>
<tr>
<td width="50%">

### 📚 Library Management
- 📂 Organize anime & manga collections
- 📊 Track reading & watching progress
- ⭐ Manage watchlists and favorites
- 🎨 Beautiful Material You UI
- 🔄 Sort and filter your library

</td>
<td width="50%">

### 🔗 AniList Integration
- 🔄 Two-way sync with AniList
- ⚡ Automatic progress updates
- 📋 Sync your lists instantly
- 🌐 Update from anywhere
- 🔐 Secure OAuth authentication

</td>
</tr>
<tr>
<td width="50%">

### 📈 Progress Tracking
- ✅ Track episodes & chapters
- 🔖 Resume where you left off
- ❤️ Mark favorites
- 📜 View watch history
- 📊 Detailed statistics

</td>
<td width="50%">

### 🎨 Customization
- 🌓 Themes: Light, Dark, AMOLED
- 🎨 Accent colors: 6 options
- 📱 Layout views: Card, Compact, List
- 📖 Reading modes: Vertical, LTR, RTL
- ⚙️ First-time setup wizard

</td>
</tr>
<tr>
<td colspan="2">

### 🔐 Privacy & Security
- 🔒 **Encrypted credential storage** using Android EncryptedSharedPreferences
- 🔐 **App lock** with biometric authentication (fingerprint/face)
- 🕶️ **Incognito mode** for private browsing
- 💾 **Local backup & restore** for your data
- 🚫 **Zero data collection** — no analytics, no tracking
- 🏠 **No servers** — everything stays on your device

</td>
</tr>
</table>

---

## 🛠️ Tech Stack

<div align="center">

| **Category** | **Technology** |
|:------------:|:--------------:|
| **Language** | ![Kotlin](https://img.shields.io/badge/Kotlin-7F52FF?style=flat&logo=kotlin&logoColor=white) |
| **UI Framework** | ![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-4285F4?style=flat&logo=jetpack-compose&logoColor=white) ![Material 3](https://img.shields.io/badge/Material%203-757575?style=flat&logo=material-design&logoColor=white) |
| **Architecture** | MVVM + Clean Architecture |
| **Dependency Injection** | Hilt (Dagger) |
| **Database** | Room |
| **Networking** | OkHttp + GraphQL |
| **Image Loading** | Coil |
| **Async** | Kotlin Coroutines + Flow |
| **Storage** | DataStore + EncryptedSharedPreferences |
| **Background Tasks** | WorkManager |

</div>

---

## 📥 Installation

### Option 1: Download APK (Recommended)

1. **Download** the latest APK from [Releases](https://github.com/TamerAli-0/OmniStream/releases/latest)
2. **Enable** "Install from unknown sources" in Android settings
3. **Install** the APK and enjoy!

> **Minimum Requirements:** Android 8.0 (API 26) or higher

### Option 2: Build from Source

```bash
# Clone the repository
git clone https://github.com/TamerAli-0/OmniStream.git
cd OmniStream

# Build debug APK
./gradlew assembleDebug

# Build release APK (optimized)
./gradlew assembleRelease
```

**For development:**
1. Register your app at [AniList Developers](https://anilist.co/settings/developer)
2. Add your Client ID to `AniListAuthManager.kt`
3. Open in Android Studio (Hedgehog 2023.1.1 or newer)
4. Sync Gradle and run on device/emulator

---

## 🚀 Getting Started

1. **Install** OmniStream on your Android device
2. **Open** the app and complete the first-time setup wizard
3. **Connect** your AniList account (optional but recommended)
4. **Start** organizing and tracking your anime & manga!

### First Launch Setup

- Choose your preferred theme (Light, Dark, AMOLED)
- Select an accent color
- Pick your default layout view
- Optionally connect your AniList account
- Enable app lock for privacy (optional)

---

## 📸 Screenshots

<div align="center">

*Coming soon — showcasing the beautiful UI and features*

</div>

---

## 🤝 Contributing

This is a personal project, but contributions are welcome!

- 🐛 **Found a bug?** [Open an issue](https://github.com/TamerAli-0/OmniStream/issues)
- 💡 **Have a feature idea?** [Request it here](https://github.com/TamerAli-0/OmniStream/issues)
- 🔧 **Want to contribute?** Fork the repo and submit a PR

---

## ⚠️ Disclaimer

OmniStream is a **personal library management and tracking tool** designed to help users organize their anime and manga collections. Users are responsible for ensuring they have appropriate rights to any content they manage through this application.

This application uses the **AniList API** for tracking and list management. It is **not affiliated with or endorsed by AniList**.

---

## 🔒 Privacy Policy

**OmniStream respects your privacy:**

- ✅ **No data collection** — We don't track or collect any user data
- ✅ **No analytics** — No usage tracking or telemetry
- ✅ **No servers** — All data stored locally on your device
- ✅ **Encrypted storage** — Credentials stored using Android's EncryptedSharedPreferences
- ✅ **Your data stays yours** — We never see or access your information

---

## 📄 License

This project is for **educational and portfolio purposes**. All rights to anime and manga titles, artwork, and related content belong to their respective copyright holders.

---

## 🙏 Acknowledgments

- **AniList** for their excellent GraphQL API
- **Material Design** for the icon library
- **Jetpack Compose** for modern UI development
- UI design inspired by modern anime/manga tracking apps

---

<div align="center">

**Built with ❤️ by [Tamer Altaweel](https://github.com/TamerAli-0)**

[![Portfolio](https://img.shields.io/badge/Portfolio-FF5722?style=for-the-badge&logo=google-chrome&logoColor=white)](https://tamerali-0.github.io/portfolio/)
[![GitHub](https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/TamerAli-0)

### ⭐ Star this repo if you find it useful!

</div>
