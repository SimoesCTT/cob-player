# 🌽 Cob Player

An all-format Android media player built on AndroidX Media3 (ExoPlayer).

## Theme
Corn/cob aesthetic — golden ambers, husk greens, deep soil darks.

## Features
- Media3 ExoPlayer engine
- Adaptive launcher icon (monochrome supported)
- Custom subtitle styling (amber-on-soil)
- MediaSessionService for background playback
- PiP-ready activity
- Intent filter for opening video/audio files

## Build
./gradlew assembleDebug

## FFmpeg extension (optional, for AC3/DTS/etc.)
The `media3-exoplayer-ffmpeg` module requires manually building native libs
via the Android NDK. Uncomment the dependency in `app/build.gradle.kts` after
building, and follow Media3's official FFmpeg build instructions.

## Structure
app/src/main/java/com/cobplayer/app/
├── service/PlaybackService.kt
└── ui/PlayerActivity.kt
