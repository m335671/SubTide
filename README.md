# SubTide

A native Android client for [SubWave](https://github.com/perminder-klair/subwave), a
self-hosted internet radio server.

[![Release signed APK](https://github.com/m335671/SubTide/actions/workflows/release-apk.yml/badge.svg)](https://github.com/m335671/SubTide/actions/workflows/release-apk.yml)
[![Latest release](https://img.shields.io/github/v/release/m335671/SubTide?label=latest%20release)](https://github.com/m335671/SubTide/releases/latest)

> [!WARNING]
> **This project is vibecoded.** Most of the code was written by an AI coding agent
> (Claude Code) from natural-language instructions, reviewed and tested by a human rather
> than hand-written line by line. If that's not the kind of project you're comfortable
> running or relying on, this app isn't for you — no hard feelings, just look elsewhere.

## About

The official SubWave client is React Native/Expo. SubTide is a ground-up Kotlin/Jetpack
Compose rewrite of the same product, built to talk to any SubWave server's public HTTP API,
with full native Android Auto support the official client doesn't have. No server is baked
in — you point it at your own SubWave instance on first launch.

## Download

Grab the latest signed APK from the [Releases page](https://github.com/m335671/SubTide/releases/latest).
A new build is published automatically every time `main` changes (see [`.github/workflows/release-apk.yml`](.github/workflows/release-apk.yml)).

## Features

| | |
|---|---|
| **Live player** | Now-playing track with cover art, artist/album, genre/BPM/key, mood tags, and a seamless elapsed/duration clock that ticks locally between server polls instead of jumping in fixed steps. |
| **Hand-drawn transport console** | Power ring, signal/listener/latency meter with a graduated scale, and a rotary volume knob — all custom-drawn, no stock Material sliders or icon packs. |
| **Android Auto** | A `MediaLibraryService`-backed `MediaSession` drives playback, the lock screen, the notification, and the car's media browser, so the stream is fully controllable from a head unit. |
| **Timeline** | Scrollable history of previously played tracks. |
| **Booth** | Live feed of DJ chatter/segments from the station. |
| **Song requests** | Submit a free-text track request and follow its pending / resolved / rejected status. |
| **Multi-theme** | Switch between server-provided palettes (e.g. classic light, midnight dark) without restarting the app. |
| **Admin skip** | Authenticated DJs can skip the current track; credentials are stored via Jetpack Security's `EncryptedSharedPreferences`, never in plaintext. |

## Style

SubTide reproduces SubWave's visual identity pixel-for-pixel rather than inventing its own
look: a vintage radio fascia reinterpreted as brutalist newsprint — cream paper background,
near-black ink, a single warm vermillion accent, serif display type for titles, monospace for
anything numeric (BPM, duration, latency, LLM token counts), zero border radius anywhere, and
hairline dividers instead of Material elevation. Every color in the app comes from a themable
7-token palette (`bg`, `ink`, `muted`, `accent`, `overlay`, `soft-border`, `field`) fetched
from the server, not hardcoded.

## Stack

Kotlin, Jetpack Compose (Material 3 as a technical skeleton only — the visual theme is fully
overridden), Media3 ExoPlayer + MediaSession, Retrofit + kotlinx.serialization, Coil,
DataStore, and Jetpack Security.

## Building from source

```bash
./gradlew assembleRelease
```

Producing a signed APK requires a keystore and the `SUBTIDE_STORE_FILE`,
`SUBTIDE_STORE_PASSWORD`, `SUBTIDE_KEY_ALIAS`, `SUBTIDE_KEY_PASSWORD` environment variables
(see `app/build.gradle.kts`); without them, `assembleRelease` still runs but produces an
unsigned APK.
