# TuneWave

A privacy-first, offline-first Android music player, built from scratch.

Inspired by the idea behind PixelPlayer, but with original code: no account, no tracking. Your music stays on your device.

## Status

Phase 5 (part 1) — synced lyrics and artist artwork are implemented: lyrics are fetched from LRCLIB (synced LRC when available, otherwise plain lyrics, with the current line highlighted as the song plays) and artist photos are fetched from Deezer's public artist search, shown next to the artist name in the full player. Both need internet, need no API key and no account, and a failed fetch only shows "lyrics not found" — it never interrupts playback. App version is now 0.4.0.

Phase 4 remaining items are still pending: true crossfade mixing (needs a dual-player rewrite), the FFmpeg extension (needs an NDK build), and a TagLib tag editor (needs native TagLib). Doing those without device testing would risk breaking playback, so they are deferred to the device-testing stage.

Phase 3 (complete) — the Glance home screen widget is implemented: it shows the current/last song title and artist, with previous, play/pause, next, and Open-app controls. The widget's state is refreshed by MusicService, which owns the player, so it updates even when the app UI is closed. Album art is not shown in the widget yet.

Phase 4 (part 1) — advanced audio settings are implemented, but not yet tested on a device or emulator (no Android SDK in the development environment used to write this code). Phone testing is deferred to the end, as agreed.

Phase 4 part 1 includes: Android Equalizer presets (Flat, Bass Boost, Treble Boost, Vocal, Rock, Pop) applied to the playback audio session on service start, an equalizer on/off setting, and a crossfade duration setting stored for future use. Gapless playback is ExoPlayer's default. True crossfade mixing, FFmpeg extension, and a TagLib tag editor are not yet implemented.

Phase 3 part 1 includes: Material You dynamic colour on Android 12+, System/Light/Dark theme setting saved in DataStore, a Settings screen, and album art in the full player (loaded with Coil from MediaStore album art). The Glance home screen widget (see above) completes Phase 3; like all other code here, it has not been device-tested yet and its Glance API usage should be confirmed at the first Android Studio build.

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
├── data/                    # Song model, MediaStore scan, playlists/favourites store, lyrics/artwork fetch
├── player/                  # MusicService (MediaSessionService), PlayerViewModel
├── widget/                  # Glance home screen widget, its actions and updater
└── ui/
    ├── theme/               # Material 3 theme
    └── screens/             # Library list, mini-player, full player
```

## Privacy

TuneWave does not require an account and does not track you. Its online features are lyrics (LRCLIB) and artist artwork (Deezer): they send only the song title, artist, album and duration, or the artist name, to those services, and need no API key. Future AI playlists, when added, will require the user's own API key and will be clearly marked as optional.

## License

MIT — see LICENSE.
