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
</p>

> **🎨 Fan Remake of the original Dantotsu app with iOS 26-inspired Liquid Glass UI**

ReDantotsu is a premium remake of the beloved Dantotsu anime & manga app, featuring a stunning Liquid Glass visual overhaul that brings an iOS 26-inspired design language to Android.

## 📋 Table of Contents
- [What's New](#-whats-new-in-redantotsu)
- [Screenshots](#-screenshots)
- [Installation](#-installation)
- [Building from Source](#-building-from-source)
- [Features](#-features)
- [Credits](#-credits)
- [License](#-license)
- [Disclaimer](#-disclaimer)
- [Contributing](#-contributing)

## ✨ What's New in ReDantotsu

### 🌟 Liquid Glass Theme
- **Real-time backdrop blur effects** - Glass surfaces that blur the content behind them
- **Dynamic lens distortion** - Subtle liquid-like refraction effects
- **Smooth 60fps animations** - Optimized spring animations and reduced GPU load
- **Premium pill-shaped bottom bars** - Consistent semi-transparent design across all screens

### 🎯 UI Improvements
- **Glass Settings Overlay** - Beautiful slide-up settings panel with glass effect
- **Redesigned bottom navigation** - Modern pill-shaped navigation with smooth tab switching
- **Consistent dark/light themes** - Proper theming across all glass components
- **Optimized performance** - 60fps animations with reduced blur radii
- **MyAnimeList rating** - Added support for MyAnimeList ratings

### 🔧 Technical Improvements
- **Fixed loading glitches** - Proper fragment lifecycle management in Compose
- **Better memory management** - Optimized recomposition with derived state
- **Hardware-accelerated effects** - Leverages GPU for glass effects

## 📸 Screenshots

| Home | Manga | Anime |
|:---:|:---:|:---:|
| <img src="https://i.postimg.cc/Hn4Lk7DY/Home_page.jpg" width="300" /> | <img src="https://i.postimg.cc/Wz641JLk/Manga_page.jpg" width="300" /> | <img src="https://i.postimg.cc/KjrY8gSg/Anime_page.jpg" width="300" /> |

## 📥 Installation

1. Download the latest APK from [Releases](https://github.com/AsrOfficialDev/ReDantotsu/releases)
2. Enable "Install from unknown sources" if prompted
3. Install and enjoy!

## 🛠️ Building from Source

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

- **Stream & Download Anime** - Through 3rd party extensions
- **Read Manga** - With built-in reader supporting multiple layouts
- **AniList Sync** - Real-time synchronization with your AniList account
- **MAL Sync** - Optional MyAnimeList integration
- **Discord Rich Presence** - Show what you're watching/reading
- **Extension System** - Modular source system
- **Offline Mode** - Download content for offline viewing
- **Auto-Skip** - Skip openings, endings, and recaps
- **Timestamp Support** - Community-powered timestamps

## 🏛️ Credits

### Original Project
- **[Dantotsu](https://git.rebelonion.dev/rebelonion/Dantotsu)** by [rebelonion](https://github.com/rebelonion)
- Built from the ashes of Saikou

### ReDantotsu
- **Fan Remake Developer:** Ashraful
- **Liquid Glass Effect:** Based on iOS 26 design language
- **Backdrop Library:** [backdrop](https://github.com/kyant0/backdrop) by kyant0

## 📜 License

This project is licensed under the **Unabandon Public License (UPL)**, which extends GPLv3.

### Key Terms:
- ✅ **Free to use, modify, and distribute**
- ✅ **Source code must remain public** (GitHub fulfills this)
- ✅ **Same license for derivative works**
- ⚠️ **Must preserve original copyright notices**

> This is a derivative work of [Dantotsu](https://github.com/rebelonion/Dantotsu), licensed under GPLv3/UPL.

## ⚠️ Disclaimer

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
  <b>ReDantotsu</b> • Fan Remake with ❤️ and Liquid Glass ✨
</p>
