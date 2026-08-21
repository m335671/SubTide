# SubTide

A native Android client for [SubWave](https://github.com/perminder-klair/subwave), a
self-hosted internet radio server. The official client is React Native/Expo — SubTide is a
ground-up Kotlin/Jetpack Compose rewrite of the same product, built to talk to any SubWave
server's public HTTP API, with full native Android Auto support the official client doesn't have.

## Style

SubTide reproduces SubWave's visual identity pixel-for-pixel rather than inventing its own look:
a vintage radio fascia reinterpreted as brutalist newsprint — cream paper background, near-black
ink, a single warm vermillion accent, serif display type for titles, monospace for anything
numeric (BPM, duration, latency, LLM token counts), zero border radius anywhere, and hairline
dividers instead of Material elevation. Every color in the app comes from a themable 7-token
palette (`bg`, `ink`, `muted`, `accent`, `overlay`, `soft-border`, `field`) fetched from the
server, not hardcoded — see `design-system.md` for the full spec this app was built against.

## Features

- **Live player** — now-playing track with cover art, artist/album, genre/BPM/key, mood tags,
  and a seamless elapsed/duration clock that ticks locally between server polls instead of
  jumping in fixed steps.
- **Hand-drawn transport console** — power ring, signal/listener/latency meter with a graduated
  scale, and a rotary volume knob, all custom-drawn (no stock Material sliders or icon packs).
- **Android Auto** — a `MediaLibraryService`-backed `MediaSession` drives playback, the lock
  screen, the notification, and the car's media browser, so the stream is fully controllable
  from a car head unit.
- **Timeline** — scrollable history of previously played tracks.
- **Booth** — live feed of DJ chatter/segments from the station.
- **Song requests** — submit a free-text track request and follow its pending/resolved/rejected
  status.
- **Multi-theme support** — switch between server-provided palettes (e.g. classic light,
  midnight dark) without restarting the app.
- **Admin skip** — authenticated DJs can skip the current track; credentials are stored via
  Jetpack Security's `EncryptedSharedPreferences`, never in plaintext.
- **Point-at-any-server onboarding** — no server is baked into the app; you type in your own
  SubWave instance's URL on first launch.

## Stack

Kotlin, Jetpack Compose (Material 3 as a technical skeleton only — the visual theme is fully
overridden), Media3 ExoPlayer + MediaSession, Retrofit + kotlinx.serialization, Coil, DataStore,
and Jetpack Security.
