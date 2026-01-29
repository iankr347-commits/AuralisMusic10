<div align="center">
  <img src="assets/logo.png" alt="Auralis Music Logo" width="120" height="120">
  
  <h1>Auralis Music</h1>
  
  <p>A robust, open-source music streaming client offering an ad-free experience, offline capabilities, and advanced music discovery.</p>
  
  
  <p>
    <a href="../assets/download.png">
      <img src="assets/download.png" alt="Direct Download" width="200">
    </a>
    <a href="https://github.com/iankr347/AuralisMusic467">
      <img src="assets/github.png" alt="Available on GitHub" width="200">
    </a>
  </p>
</div>

---

## Overview

Auralis Music is a feature-rich music streaming application designed for Android devices. Built with a focus on user experience and performance, it provides seamless access to millions of songs while maintaining a clean, intuitive interface. Whether you're a casual listener or a music enthusiast, Auralis Music offers the tools you need to enjoy your favorite tracks without interruptions.

---

## Important

This project is still under active development. Some features may be incomplete or contain bugs. Please report any issues you encounter on our GitHub repository.

---

## Features

### 🎵 Streaming and Playback
- **High-Quality Audio**: Stream music in various bitrates up to 320kbps
- **Background Playback**: Keep music playing even when the app is in the background
- **Gapless Playback**: Seamless transitions between tracks
- **Crossfade Support**: Smooth audio transitions between songs
- **Audio Enhancement**: Built-in equalizer with presets and custom settings
- **Sleep Timer**: Automatically stop playback after a set duration

### 🎨 User Interface
- **Material Design 3**: Modern, clean interface following Android's latest design guidelines
- **Multiple Themes**: Light, Dark, and Pure Black (AMOLED) themes
- **Customizable Home Screen**: Personalize your music experience
- **Intuitive Navigation**: Easy-to-use interface with smooth animations
- **Responsive Design**: Optimized for phones and tablets

### 📚 Library Management
- **Full Library Access**: Browse your entire music collection
- **Smart Playlists**: Automatically generated playlists based on your listening habits
- **Import/Export**: Support for various playlist formats
- **Offline Mode**: Download songs for offline listening
- **Metadata Editing**: Edit song information and album art
- **Duplicate Detection**: Find and remove duplicate tracks

### 🔍 Discovery and Recommendations
- **Advanced Search**: Find songs, albums, artists, and playlists
- **Personalized Recommendations**: Discover new music based on your taste
- **Trending Charts**: Stay updated with the latest hits
- **Radio Stations**: Create custom radio stations based on your preferences
- **Similar Artists**: Explore music from artists similar to your favorites

### ⚙️ Advanced Capabilities
- **Last.fm Integration**: Scrobble your tracks to Last.fm
- **Discord Rich Presence**: Show what you're listening to on Discord
- **Lyrics Support**: Display synchronized lyrics for your favorite songs
- **Podcast Support**: Listen to your favorite podcasts
- **Cloud Sync**: Sync your library across multiple devices
- **Proxy Support**: Use custom proxy settings for enhanced privacy

---

## Screenshots

### Mobile

<div align="center">
  <img src="assets/sh1.jpg" alt="Home Screen" width="180">
  <img src="assets/sh1.jpg" alt="Now Playing" width="180">
  <img src="assets/sh1.jpg" alt="Search" width="180">
  <img src="assets/sh1.jpg" alt="Library" width="180">
 
</div>

---

## Installation

### Direct Download (APK)

1. Download the latest APK from our [Releases Page](https://github.com/iankr347/AuralisMusic467/releases)
2. Enable "Install from unknown sources" in your device settings
3. Locate and install the downloaded APK file
4. Launch Auralis Music and enjoy!

### Build from Source

1. **Clone the Repository**
   ```bash
   git clone https://github.com/iankr347/AuralisMusic10.git
   cd AuralisMusic10
   ```

2. **Configure Android SDK**
   Create a `local.properties` file in the project root and define your Android SDK path:
   ```bash
   echo "sdk.dir=/path/to/your/android/sdk" > local.properties
   ```

3. **Firebase Configuration**
   - Firebase setup is required for certain features
   - Follow the instructions in `FIREBASE_SETUP.md` to add your `google-services.json`
   - This step is optional for basic functionality

4. **Build**
   ```bash
   ./gradlew assembleFossDebug
   ```

   The APK will be generated in `app/build/outputs/apk/foss/debug/`

---

## Requirements

- **Android Version**: 8.0 (API level 26) or higher
- **Storage**: Minimum 100MB free space for app and cache
- **Network**: Internet connection required for streaming
- **RAM**: Minimum 2GB RAM recommended for optimal performance

---

## Contributing

We welcome contributions from the community! Here's how you can help:

1. **Fork the repository**
2. **Create a feature branch** (`git checkout -b feature/AmazingFeature`)
3. **Commit your changes** (`git commit -m 'Add some AmazingFeature'`)
4. **Push to the branch** (`git push origin feature/AmazingFeature`)
5. **Open a Pull Request**

Please read our [Contributing Guidelines](CONTRIBUTING.md) for more details.

---

## License

This project is licensed under the GPL-3.0 License. See the [LICENSE](LICENSE) file for details.

---

## Support

- **Issues**: Report bugs and request features on [GitHub Issues](https://github.com/iankr347/AuralisMusic10/issues)
- **Discussions**: Join our community discussions on [GitHub Discussions](https://github.com/iankr347/AuralisMusic10/discussions)
- **Wiki**: Check out our [Wiki](https://github.com/iankr347/AuralisMusic10/wiki) for detailed guides

---

<div align="center">
  <p>Made with ❤️ for music lovers</p>
  <p>© 2024 Auralis Music. All rights reserved.</p>
</div>
