# TuneWave

A privacy-first, offline-first Android music player, built from scratch.

Inspired by the idea behind PixelPlayer, but with original code: no account, no tracking. Your music stays on your device.

## Status

Phase 3 (part 1) — UI polish is implemented, but not yet tested on a device or emulator (no Android SDK in the development environment used to write this code). Phone testing is deferred to the end, as agreed.

Phase 3 part 1 includes: Material You dynamic colour on Android 12+, System/Light/Dark theme setting saved in DataStore, a Settings screen, and album art in the full player (loaded with Coil from MediaStore album art). Home screen widgets (Glance) are not yet implemented and remain in Phase 3.

Phase 1 includes: MediaStore library scan, song list, play/pause, next/previous, queue, shuffle, repeat, seek, mini-player, full player screen, and background playback via a MediaSessionService.

Phase 2 includes: browse by album and artist, full-text search, sort by title/artist/duration, favourites, playlists (create/add/delete, stored locally in DataStore), and a Stats tab (total songs, total time, total plays, most played song).

Note: folder browsing is not yet implemented. On modern Android, scoped storage does not reliably expose a song's folder path from MediaStore, so Phase 2 uses album/artist organization instead.

## Features (planned)

- Offline playback of local music (MP3, FLAC, AAC, OGG, WAV)
- Browse by song, album, artist, genre, and folder
- Queue, shuffle, and repeat
- Background playback with notification controls
- Material 3 UI with dark/light themes
- Playlists and favourites
- Later: synced lyrics, equalizer, Chromecast, AI playlists

## Requirements

- Android 11 (API 30) or higher
- Android Studio Ladybug (2024.2.1) or newer
- JDK 17

## Build

1. Open this project in Android Studio.
2. Let Gradle sync.
3. Run on a device or emulator (Build > Make Project, then Run).

## Project structure

```
app/src/main/java/com/tunewave/player/
├── MainActivity.kt          # App entry point, permission request
├── data/                    # Song model, MediaStore scan, playlists/favourites store
├── player/                  # MusicService (MediaSessionService), PlayerViewModel
└── ui/
    ├── theme/               # Material 3 theme
    └── screens/             # Library list, mini-player, full player
```

## Privacy

TuneWave does not require an account and does not track you. Optional online features (lyrics, artwork, AI playlists), when added, will require their own API keys and will be clearly marked as optional.

## License

MIT — see LICENSE.
