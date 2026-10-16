# 🌽 Cob Player

> *Every seed of media, played.*

A modern Android media player built for 2026 — universal codec support, network shares, cross-device resume, and a corn-themed UI that shits all over VLC.

---

## Why Cob Player

VLC is a player from 2001 that plays everything.
Cob Player is a player from 2026 that plays everything **and respects modern Android**.

| Capability | VLC | Cob |
|---|---|---|
| Universal codecs | ✅ | ✅ (via FFmpeg) |
| Network shares (SMB/FTP/SFTP) | ✅ | ✅ |
| Subtitles + track selector | ✅ | ✅ |
| Gesture controls | ✅ | ✅ |
| Equalizer | ✅ | ✅ |
| Playlists | ✅ | ✅ |
| Background playback | ✅ | ✅ |
| Picture-in-Picture | ✅ | ✅ |
| **Auto-PiP on home press** | Manual | ✅ |
| **Playback speed per-file** | ❌ | ✅ |
| **Chapter markers on scrubber** | Buried | ✅ |
| **Auto-subtitle discovery** | ❌ | ✅ |
| **Cross-device resume sync** | ❌ | ✅ (WebDAV) |
| **Encrypted credential storage** | ❌ | ✅ (AES-256) |
| **Auto TV show grouping** | ❌ | ✅ (`S02E07`) |
| **Continue-watching carousel** | ❌ | ✅ |
| **Stream URL history** | Basic | ✅ |
| **Drag-to-reorder playlists** | Basic | ✅ |
| **Corn-themed audio spectrum** | ❌ | ✅ |
| **Modern Material 3 UI** | Legacy | ✅ |
| **100% Kotlin** | C core | ✅ |
| **APK size** | ~50 MB | ~30 MB |

---

## Features

### Playback
- Universal codecs via NextLib FFmpeg — AC3, EAC3, DTS, TrueHD, MLP, ALAC
- Hardware + software decoding
- Adaptive streaming (HLS, DASH)
- Network protocols: HTTP, HTTPS, FTP, SFTP, SMB2/3, RTSP, MMS
- Audio focus management, headphone unplug pause
- Background playback with MediaSession
- Picture-in-Picture (auto on home-press)
- Cast support (Google Cast SDK)

### Controls
- Big play/pause, ±10s seek, prev/next
- Scrubber with chapter markers
- Playback speed 0.25× – 4× (persisted per file)
- Aspect ratio (Fit / Fill / Zoom / 16:9 / 4:3)
- Repeat / shuffle / A-B loop
- Sleep timer
- Frame-step forward/back
- Video rotation
- Resume position (auto-save every 5s)
- Snapshot (save frame to gallery)

### Gestures
- Horizontal swipe → seek preview
- Vertical swipe (left half) → brightness
- Vertical swipe (right half) → volume
- Double-tap left/right → ±10 seconds
- Double-tap center → toggle controls

### Audio & Subtitles
- 5-band equalizer with presets
- Track selector for all audio + subtitle streams
- Embedded + external subtitle loading
- Auto-subtitle discovery (finds `<video>.srt` automatically)
- Subtitle delay ±500 ms
- Audio delay ±500 ms
- Corn-themed audio spectrum

### Library
- Videos / Audio / Shows / Recent tabs
- Auto TV show grouping (`S02E07`, `2x07` parsing)
- Continue-watching carousel
- Grid / list view toggle
- Video thumbnails
- 6 sort modes
- Folder grouping
- Full-text search (also walks filesystem)
- Long-press actions

### File Browsing
- In-app file browser with direct filesystem access
- SAF fallback for restricted modes
- SD card / USB support
- Media filtering (10 video + 9 audio extensions)
- Human-readable file sizes

### Playlists
- Create / rename / delete
- Add / remove items
- Drag-to-reorder
- Play from any item
- Import / export `.m3u`

### Network
- Network streams (HLS/DASH/RTSP/MMS/HTTP)
- Persistent stream URL store
- SMB / FTP / SFTP server browser
- SMB connection pooling
- Encrypted credential storage

### Cross-Device
- WebDAV snapshot sync
- Auto-push on player exit
- Merge by fingerprint

---

## Download

Prebuilt APKs: [Releases](https://github.com/SimoesCTT/cob-player/releases)

Also available on **[Corn Store](https://simoesctt.github.io/corn-store-index/)** — an index of free and open-source Android apps.

---

## Build from Source

```bash
git clone https://github.com/SimoesCTT/cob-player.git
cd cob-player
./gradlew assembleDebug
The APK will be at app/build/outputs/apk/debug/app-debug.apk.

Requirements
Android Studio Hedgehog (2023.1.1) or newer

JDK 17+

Gradle 8.9 (bundled via wrapper)

Android SDK 35 (installed via Android Studio or sdkmanager)

Building a release
./gradlew assembleRelease
The release APK will be at app/build/outputs/apk/release/app-release.apk.

Tech Stack
Layer	Technology
Language	Kotlin 2.1
Build	Gradle 8.9, AGP 8.7.2
Playback	AndroidX Media3 ExoPlayer 1.8.0
Codecs	NextLib FFmpeg (AC3, EAC3, DTS, TrueHD, MLP, ALAC)
Session	Media3 MediaSessionService
Cast	Google Cast SDK 21.5 + MediaRouter
Network	smbj 0.13 (SMB2/3), Apache Commons Net (FTP), JSch (SFTP)
Crypto	AndroidX Security (AES-256-GCM via Android Keystore)
Screenshots
Coming soon — drop your own in docs/screenshots/ and reference them here.

Contributing
Issues and PRs welcome. Please keep the corn theme alive.

Fork the repo

Create a feature branch (git checkout -b feature/amazing-feature)

Commit your changes (git commit -m 'Add amazing feature')

Push to the branch (git push origin feature/amazing-feature)

Open a Pull Request

License
GPL-3.0 — see LICENSE

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

Author
Americo N. F. Simoes — @SimoesCTT

Physicist | Cybersecurity Researcher | Author

🌽 Every seed of media, played.
