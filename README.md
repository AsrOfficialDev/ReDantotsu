# ReDantotsu

<p align="center">
  <a href="https://github.com/AsrOfficialDev/ReDantotsu/releases/latest">
    <img src="https://img.shields.io/github/v/release/AsrOfficialDev/ReDantotsu?style=for-the-badge&logo=github&color=00bfff&label=Current%20Release" alt="Current Release">
  </a>
  <a href="https://github.com/AsrOfficialDev/ReDantotsu/releases">
    <img src="https://img.shields.io/github/downloads/AsrOfficialDev/ReDantotsu/total?style=for-the-badge&logo=github&color=2ea44f&label=Total%20Downloads" alt="Total Downloads">
  </a>
  <a href="https://github.com/AsrOfficialDev/ReDantotsu/stargazers">
    <img src="https://img.shields.io/github/stars/AsrOfficialDev/ReDantotsu?style=for-the-badge&logo=github&color=yellow&label=Stars" alt="Stars">
  </a>
  <a href="https://discord.gg/fYEJmDsDz9">
    <img src="https://img.shields.io/badge/Discord-Join%20Us-7289da?style=for-the-badge&logo=discord&logoColor=white" alt="Discord">
  </a>
  <a href="./LICENSE.md">
    <img src="https://img.shields.io/badge/License-UPL-blue?style=for-the-badge&logo=open-source-initiative&logoColor=white" alt="License: UPL">
  </a>
  <img src="https://img.shields.io/badge/Android-14%2B-green?style=for-the-badge&logo=android" alt="Android 14+">
  <img src="https://komarev.com/ghpvc/?username=AsrOfficialDev-ReDantotsu&style=for-the-badge&label=Visits" alt="Visits">
</p>

> **✨ The Ultimate Anime & Manga Experience for Android**

ReDantotsu is a premium fan remake of the beloved Dantotsu application. I've taken the robust foundation of the original app and completely reimagined it, introducing a modern UI, expanding core functionality, and implementing crucial stability fixes to deliver the definitive anime and manga tracking experience.

## 📋 Table of Contents
- [What's New in ReDantotsu](#-whats-new-in-redantotsu)
- [Screenshots](#-screenshots)
- [Installation](#-installation)
- [Building from Source](#building-from-source)
- [Features](#-features)
- [Credits](#credits)
- [License](#license)
- [Disclaimer](#disclaimer)
- [Contributing](#-contributing)

## ✨ What's New in ReDantotsu

We've extensively upgraded the app far beyond a simple visual reskin. Here is what makes ReDantotsu the best version yet:

### 🌟 Brand New Features
- **Expanded Home Experience** - A redesigned home screen featuring dedicated Anime and Manga sections for better content discovery.
- **Source Deduplication** - Intelligent handling of extensions to eliminate duplicate entries and streamline your library.
- **Enhanced Integration** - Upgraded and fully stable AniList login utilizing modern dashboard redirect URIs, alongside MyAnimeList rating support.

### 🎨 Premium UI Overhaul (Liquid Glass)
- **Real-time Backdrop Blur** - Beautiful glass surfaces that dynamically blur content behind them for an iOS-inspired aesthetic.
- **Redesigned Navigation** - Modern pill-shaped bottom bars and a stunning sliding glass settings overlay.
- **Fluid Animations** - Smooth 60fps spring animations with subtle lens distortion and optimized GPU load.
- **Polished Visuals** - Consistent dark/light theming, flawless profile picture rendering, and improved layout consistency.

### 🔧 Core Fixes & Optimizations
- **Stable Core Systems** - Fixed the in-app update checker and resolved complex fragment lifecycle glitches present in the original app.
- **Optimized Performance** - Better memory management using derived state recomposition, allowing for a faster and smoother browsing experience.

## 📸 Screenshots

| Home | Manga | Anime |
|:---:|:---:|:---:|
| <img src="https://i.postimg.cc/Hn4Lk7DY/Home_page.jpg" width="300" /> | <img src="https://i.postimg.cc/Wz641JLk/Manga_page.jpg" width="300" /> | <img src="https://i.postimg.cc/KjrY8gSg/Anime_page.jpg" width="300" /> |

## 📥 Installation

1. Download the latest APK from the [Releases](https://github.com/AsrOfficialDev/ReDantotsu/releases) page.
2. Enable "Install from unknown sources" if prompted by your device.
3. Install and enjoy!

## 🛠️ Building from Source <a name="building-from-source"></a>

```bash
# Clone the repository
git clone https://github.com/AsrOfficialDev/ReDantotsu.git
cd ReDantotsu

# Build debug APK
./gradlew assembleGoogleAlpha

# Or build release APK (requires signing config)
./gradlew assembleGoogleRelease
```

## 🎯 Features

- **AniList Sync** - Real-time synchronization with your AniList account.
- **MAL Sync** - Optional MyAnimeList integration for ratings.
- **Discord Rich Presence** - Show off what you're currently watching or reading to your friends.
- **Extension System** - Modular source system for unlimited content discovery.
- **Offline Mode** - Download content for offline viewing.
- **Auto-Skip** - Automatically skip openings, endings, and recaps.
- **Timestamp Support** - Community-powered timestamps.

## 🏛️ Credits <a name="credits"></a>

### Original Project
- **[Dantotsu](https://git.rebelonion.dev/rebelonion/Dantotsu)** by [rebelonion](https://github.com/rebelonion)
- Built from the ashes of Saikou

### ReDantotsu
- **Fan Remake Developer:** Ashraful
- **Liquid Glass Effect:** Based on iOS 26 design language
- **Backdrop Library:** [backdrop](https://github.com/kyant0/backdrop) by kyant0

## 📜 License <a name="license"></a>

This project is licensed under the **Unabandon Public License (UPL)**, which extends GPLv3.

### Key Terms:
- ✅ **Free to use, modify, and distribute**
- ✅ **Source code must remain public** (GitHub fulfills this)
- ✅ **Same license for derivative works**
- ⚠️ **Must preserve original copyright notices**

> This is a derivative work of [Dantotsu](https://github.com/rebelonion/Dantotsu), licensed under GPLv3/UPL.

## ⚠️ Disclaimer <a name="disclaimer"></a>

- ReDantotsu does not host any content. All streaming sources come from 3rd party extensions.
- ReDantotsu is not affiliated with AniList, MyAnimeList, or any content providers.
- All anime/manga information is sourced from public APIs.
- The developers are not responsible for any misuse of the app.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

<p align="center">
  <b>ReDantotsu</b> • A Premium Anime & Manga Client ✨
</p>
