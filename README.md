<div align="center">

# 🎬 Subget

**Modern, Blazing-Fast Subtitle Downloader for Movies & TV Shows**

[![GitHub Release](https://img.shields.io/github/v/release/Ysn4Irix/subget?color=E50914&style=for-the-badge&logo=github)](https://github.com/Ysn4Irix/subget/releases/latest)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Windows-blue?style=for-the-badge&logo=windows&logoColor=white)](https://github.com/Ysn4Irix/subget/releases)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Compose Multiplatform](https://img.shields.io/badge/Compose%20Multiplatform-1.7.0-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white)](https://www.jetbrains.com/lp/compose-multiplatform/)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=for-the-badge)](https://opensource.org/licenses/MIT)

<p align="center">
  A cross-platform subtitle search and download manager built with <b>Compose Multiplatform (CMP)</b>.<br/>
  Featuring instant SubDL lookup, Cinemeta poster integration, intelligent TV show episode breakdown, in-memory zip extraction, and a dedicated <b>Cinema Modern</b> dark aesthetic.
</p>

[Download Latest Release](https://github.com/Ysn4Irix/subget/releases/latest) • [Features](#-key-features) • [Installation](#-installation--downloads) • [Configuration](#-configuration) • [Monorepo Architecture](#-monorepo-architecture) • [Building from Source](#-building-from-source)

---

</div>

## 🌟 Overview

**Subget** simplifies finding and downloading subtitles for your favorite movies and television series. Instead of navigating ad-heavy subtitle websites, Subget provides a clean, native, and ad-free experience powered by the SubDL API and Cinemeta metadata engine.

Whether running on **Android** or as a frameless, hardware-accelerated **Windows Desktop** app, Subget shares over 90% of its UI, ViewModels, networking, and business logic through Kotlin Multiplatform.

---

## ✨ Key Features

### 🔍 Intelligent Search & Live Suggestions
- **Instant Search**: Query SubDL's extensive subtitle database with sub-second response times.
- **Cinemeta Live Autocomplete**: Real-time title suggestions, IMDb ratings, and high-resolution poster artwork as you type.
- **Smart Query Parsing**: Automatically detects movies vs. TV series and extracts season and episode numbers from release names.

### 🎯 Granular Filtering Engine
- **Language Chips**: Quick multi-select filtering for English, Arabic, Spanish, French, German, Italian, and more.
- **Season & Episode Breakdown**: Interactive episode chips displaying available subtitle counts per episode.
- **Quality Groupings**: Filter releases by video resolution — `4K UHD`, `1080p / BluRay`, `720p / WEB-DL`, and `Other`.
- **Accessibility Filter**: Hearing Impaired (`HI`) badge and toggle to locate SDH subtitles easily.

### 📦 Seamless Zip Decompression
- **In-Memory Extraction**: Subtitles packaged inside zip archives are unpacked on the fly.
- **Multi-Format Support**: Direct extraction and saving of `.srt`, `.vtt`, `.ass`, and `.sub` files.
- **Clean Naming**: Saves clean subtitle files named after the release title directly into your downloads storage.

### 📂 Integrated Downloads Vault
- **Local History**: View, inspect, and organize all previously downloaded subtitle files.
- **System Player Launch**: Open subtitle files directly with your system's default media player (VLC, MPC-HC, MPV, etc.).
- **Explorer Integration**: One-click "Reveal in Folder" (`explorer.exe /select`) on Windows and system file manager on Android.
- **Sharing & Management**: Share subtitle files to external apps or clean up old downloads.

### 🖥️ Windows Desktop "Cinema Modern" Experience
- **Frameless Window**: Custom undecorated obsidian (`#0B0E14`) window design with a razor-thin accent divider.
- **Modern Draggable Title Bar**: Integrated dragging area, brand icon, and interactive control buttons (Minimize, Maximize/Restore, Close with Cinema Red hover).
- **Native Windows Pathing**: Automatically saves to `%USERPROFILE%\Downloads\Subget\`.

### 📱 Android Refinement
- **Material 3 Cinema Theme**: True black / obsidian aesthetic optimized for OLED displays.
- **Edge-to-Edge Layout**: Fully immersive system bar handling for Android 8.0 (API 26) through Android 15 (API 35).
- **Secure Storage**: Encrypted credential storage for API keys and seamless MediaStore integration.

---

## 📥 Installation & Downloads

Pre-built binaries are available for every tagged release on GitHub.

### Latest Release: [v1.1.0](https://github.com/Ysn4Irix/subget/releases/tag/v1.1.0)

| Platform | Package Format | Download Link | Description |
| :--- | :--- | :--- | :--- |
| **Android** | `.apk` | [Subget-v1.1.0.apk](https://github.com/Ysn4Irix/subget/releases/download/v1.1.0/Subget-v1.1.0.apk) | Android 8.0+ (ARM64, x86_64), signed |
| **Windows** | Setup `.exe` | [Subget-v1.1.0-windows-setup.exe](https://github.com/Ysn4Irix/subget/releases/download/v1.1.0/Subget-v1.1.0-windows-setup.exe) | Single-file installer for Windows 10/11 |
| **Windows** | MSI `.msi` | [Subget-v1.1.0-windows-installer.msi](https://github.com/Ysn4Irix/subget/releases/download/v1.1.0/Subget-v1.1.0-windows-installer.msi) | Windows Installer package for managed setups |

#### Verifying Checksums

You can verify the integrity of downloaded binaries against our official SHA-256 manifest:

```powershell
# Windows PowerShell
Get-FileHash -Algorithm SHA256 Subget-v1.1.0.apk
Get-FileHash -Algorithm SHA256 Subget-v1.1.0-windows-setup.exe
Get-FileHash -Algorithm SHA256 Subget-v1.1.0-windows-installer.msi
```

Or view the [Subget-v1.1.0.sha256](https://github.com/Ysn4Irix/subget/releases/download/v1.1.0/Subget-v1.1.0.sha256) file directly.

---

## ⚙️ Configuration

Subget connects to the **SubDL** subtitle API. A free API key is required to query subtitles.

1. **Sign Up**: Register for a free account at [SubDL.com](https://subdl.com).
2. **Retrieve API Key**: Go to your SubDL dashboard / profile settings and copy your personal API key.
3. **Configure in Subget**:
   - Launch Subget.
   - Navigate to the **Settings** tab (`⚙️`).
   - Paste your API key into the **SubDL API Key** input field and click **Save**.
   - Your key is securely saved locally and will be used automatically for all searches.

---

## 🏗️ Monorepo Architecture

Subget is structured as a **Compose Multiplatform (CMP)** monorepo using Gradle:

```
subget/
├── androidApp/                  # Android application module
│   ├── src/main/java/           # MainActivity, Application class
│   └── src/main/res/            # Android resources, launcher icons
├── desktopApp/                  # Windows Desktop application module
│   └── src/jvmMain/kotlin/      # Desktop entry point (Main.kt) & CinemaModernTitleBar
├── shared/                      # Shared Kotlin Multiplatform (CMP) module
│   ├── src/commonMain/          # 90%+ Shared code
│   │   ├── kotlin/.../api/      # SubdlApiService, PosterRepository (Cinemeta)
│   │   ├── kotlin/.../data/     # SubdlModels, MediaPoster, ZipExtractor
│   │   ├── kotlin/.../storage/  # SubtitleStorageManager interface & common storage logic
│   │   ├── kotlin/.../ui/       # SubgetApp, Screens (Search, Downloads, Settings)
│   │   ├── kotlin/.../theme/    # Cinema theme, colors, typography
│   │   └── composeResources/    # Shared vector assets and brand logos
│   ├── src/androidMain/         # Android implementations (MediaStore, EncryptedPrefs)
│   └── src/desktopMain/         # Desktop implementations (Swing/Desktop IO, Explorer integration)
├── gradle/                      # Version catalog (libs.versions.toml) & wrapper
└── build.gradle.kts             # Root Gradle build script
```

---

## 🛠️ Building from Source

### Prerequisites

- **Java Development Kit (JDK)**: JDK 17 or higher (recommended: OpenJDK 17 or Eclipse Temurin 17+).
- **Android SDK**: Compile SDK 35, Build-Tools 35.0.0 (required for `:androidApp`).
- **Operating System**: Windows 10/11 (for desktop installers), macOS, or Linux.

### 1. Clone Repository

```bash
git clone https://github.com/Ysn4Irix/subget.git
cd subget
```

### 2. Run Desktop App in Development Mode

```bash
./gradlew :desktopApp:run
```

### 3. Build Windows Desktop Release Packages

Builds both the standalone `.exe` setup and the `.msi` Windows installer in `desktopApp/build/compose/binaries/main/`:

```bash
./gradlew :desktopApp:packageReleaseDistributionForCurrentOS
```

### 4. Build Android Release APK

```bash
./gradlew :androidApp:assembleRelease
```

The APK will be generated at:
`androidApp/build/outputs/apk/release/androidApp-release-unsigned.apk` (or signed if signing keys are configured).

### 5. Run Unit Tests

Execute the complete multiplatform unit test suite (testing query parsing, poster mapping, zip extraction, quality filtering, and episode matching):

```bash
./gradlew test --continue
```

---

## 🧰 Tech Stack & Libraries

| Category | Technology |
| :--- | :--- |
| **Language** | [Kotlin 2.0.21](https://kotlinlang.org/) |
| **UI Framework** | [Compose Multiplatform 1.7.0](https://www.jetbrains.com/lp/compose-multiplatform/) (Material 3) |
| **Navigation** | JetBrains Navigation Compose Multiplatform `2.8.0-alpha10` |
| **State & Lifecycle** | Lifecycle ViewModel Compose Multiplatform `2.8.4` |
| **Networking** | OkHttp `4.12.0` with Logging Interceptor |
| **Image Loading** | Coil 3 (`3.0.4`) with Ktor3 Network Engine |
| **Serialization** | Kotlinx Serialization JSON `1.7.3` |
| **Concurrency** | Kotlinx Coroutines `1.9.0` (Core, Android, Swing) |
| **Testing** | JUnit 4.13.2 & Kotlin Test |

---

## 🤝 Contributing

Contributions, feature suggestions, and bug reports are welcome!

1. Fork the repository.
2. Create a feature branch (`git checkout -b feature/amazing-feature`).
3. Commit your changes (`git commit -m 'feat: add amazing feature'`).
4. Push to the branch (`git push origin feature/amazing-feature`).
5. Open a Pull Request.

---

## 📄 License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.

<div align="center">
  <sub>Built with ❤️ using Compose Multiplatform.</sub>
</div>
