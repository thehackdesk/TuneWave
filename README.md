# TuneWave

![Latest release](https://img.shields.io/github/v/release/thehackdesk/TuneWave?include_prereleases&label=Latest%20release)
![Downloads](https://img.shields.io/github/downloads/thehackdesk/TuneWave/v0.8.0-beta/total?label=Downloads)
![Android](https://img.shields.io/badge/Android-11%2B-green?logo=android)
![Top language](https://img.shields.io/github/languages/top/thehackdesk/TuneWave)
![License](https://img.shields.io/github/license/thehackdesk/TuneWave)

A privacy-first, offline-first Android music player, built from scratch with
original code. No account, no tracking — your music stays on your device.

> **Beta:** the current version is **0.8.0-beta**. Builds are debug builds for
> phone testing, not a Play Store release yet. Some features below are still
> in progress; see [What is not ready yet](#what-is-not-ready-yet).

## ‼️ DISCLAIMER

- TuneWave is an independent, original project. It is **not affiliated with,
  endorsed by, or connected to PixelPlayer** or any other music player. It
  was inspired only by the general usability of premium music library apps;
  all code, layouts and artwork in this repository are original.
- TuneWave plays **only music already on your device**. It does not download,
  stream, share or upload your audio files, and it never asks for broad
  storage access beyond what Android requires to read your music library.
- Lyrics (LRCLIB) and artist artwork (Deezer) are fetched from public
  third-party services: they receive only a song's title, artist, album and
  duration, or an artist's name. They need no API key and no account.
- Beta builds may contain bugs. There is no warranty; use at your own risk.
  See [LICENSE](#-license).

## Features (0.8.0-beta)

- Local library scan from MediaStore: songs, albums, artists, folders
  (folder grouping is best-effort — see below)
- Playback with queue, shuffle, repeat, seek, and a full Now Playing screen
  with an "Up next" queue list and a volume slider
- Background playback via MediaSessionService, with media notification
  controls, audio focus and headset controls
- Bottom navigation: Home, Songs, Liked, Library (Albums / Artists /
  Playlists / Folders / Stats)
- Home dashboard with a featured mix card, Recently played (last 20 tracks),
  Favourites and an Albums strip
- Search across songs, artists and albums; sort by title, artist or duration
- Playlists: create, rename, delete, add songs — all stored locally
- Favourites with a dedicated Liked view, and listening stats
- Daily Mix: a fresh date-seeded mix every day, favourites first
- Synced lyrics (LRCLIB, with current-line highlight) and artist artwork
  (Deezer) in the full player — both need internet, neither interrupts
  playback when they fail
- Equalizer presets (Flat, Bass Boost, Treble Boost, Vocal, Rock, Pop) and
  a crossfade duration setting
- Glance home screen widget with previous / play-pause / next / Open controls
- Themes: System / Light / Dark, plus an optional Material You dynamic
  colour switch; current look is charcoal-black with electric lime and
  soft violet accents
- Crash-hardened library scan and settings: a missing MediaStore column or
  a corrupt setting degrades to an empty library or a default, never a crash

### What is not ready yet

- True crossfade mixing (needs a dual-player rewrite), FFmpeg extension
  (needs an NDK build), TagLib tag editor (needs native TagLib)
- Chromecast, Android Auto, AI playlists (would need the user's own API key)
- Wear OS, backup/restore, streaming sources
- Signed Play Store release (AAB/APK) — current builds are debug-signed

### A note on folders

On modern Android, scoped storage does not reliably expose a song's real
folder path from MediaStore. TuneWave groups by the `RELATIVE_PATH` MediaStore
reports; if a device reports nothing usable, the Folders tab says so honestly
instead of showing fake folders. Songs, Albums and Artists always work
regardless.

## 🛠️ Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose, Material 3
- **Playback:** Media3 / ExoPlayer, MediaSessionService, Coil (album art),
  Glance (home screen widget)
- **Storage:** MediaStore (library), DataStore Preferences (favourites,
  playlists, play counts, recently played, settings) — no server, no database
- **Network (optional, online features only):** LRCLIB (lyrics), Deezer
  public artist search (artist artwork)
- **Build:** Gradle 8.9, Android Gradle Plugin (see `gradle/libs.versions.toml`),
  JDK 17, compileSdk 35, minSdk 30, targetSdk 35

## 📱 Requirements

- Android 11 (API 30) or higher
- Android Studio Ladybug (2024.2.1) or newer
- JDK 17
- On the phone: audio permission (`READ_MEDIA_AUDIO`) to see your music;
  internet is optional and only used for lyrics and artist artwork

## 🚀 Getting Started

### Run in Android Studio

1. Clone this repository.
2. Open the project in Android Studio.
3. Let Gradle sync.
4. Run on a device or emulator (Build > Make Project, then Run).

### Build from the command line

```bash
./gradlew assembleDebug
```

The debug APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.
Install it on your phone with `adb install` or by opening the APK file on
the device. It is debug-signed, for testing only.

## ⬇️ Download

- **Source code:** every GitHub Release on this repository ships the source
  as a zip/tarball.
- **Beta test APK:** there is no Play Store listing yet. Beta debug APKs are
  currently shared directly with testers (for example via Google Drive) while
  testing is in progress. To get the latest code right now, clone the repo
  and build it with the steps above.
- **Latest beta:** 0.8.0-beta (versionCode 11).

## 📂 Project Structure

```
app/src/main/java/com/tunewave/player/
├── MainActivity.kt          # App entry point, permission request
├── data/
│   ├── Song.kt              # Song model + album art URI
│   ├── MusicRepository.kt   # MediaStore library scan (crash-hardened)
│   ├── PlaylistStore.kt     # DataStore: favourites, playlists, plays, recent
│   └── LyricsRepository.kt  # LRCLIB lyrics + Deezer artist artwork
├── player/
│   ├── MusicService.kt      # MediaSessionService, background playback
│   ├── EqualizerManager.kt  # Android Equalizer presets
│   └── PlayerViewModel.kt   # Playback state, queue, volume, lyrics, stats
├── widget/                  # Glance home screen widget + actions + updater
└── ui/
    ├── theme/               # Material 3 theme (charcoal / lime / violet)
    └── screens/             # Library, full player, settings
        ├── LibraryScreen.kt
        ├── PlayerScreen.kt
        └── SettingsScreen.kt
```

Other top-level files: `build.gradle.kts`, `settings.gradle.kts`,
`gradle.properties`, `gradle/libs.versions.toml`, `gradlew`, `LICENSE`,
`.gitignore`.

## Privacy

TuneWave does not require an account and does not track you. Everything
except lyrics and artist artwork works fully offline. The two online features
send only the minimum metadata described in the Disclaimer, need no API key,
and can be ignored completely if the phone is offline — playback never
depends on them. Future AI playlists, when added, will require the user's own
API key and will be clearly marked as optional.

## 🤝 Contributing

Contributions are welcome — issues and pull requests included.

- Keep it original: do not copy code, artwork or layouts from other music
  players, proprietary or otherwise.
- Match the existing style: Kotlin + Compose, small focused changes, and a
  short note in the pull request about what you tested on a real device.
- Do not add new network services, accounts or tracking without discussing
  it in an issue first — privacy-first is the point of this app.
- If you add a feature that needs a permission, explain why in the PR; never
  request broad storage access unnecessarily.

## 📄 License

MIT — see [LICENSE](LICENSE). Copyright (c) thehackdesk.
