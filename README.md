# Rock Music

Rock Music is a premium native Android music platform built with Kotlin, Jetpack Compose, Material 3, Media3, and Clean Architecture principles.

## Current foundation

- Android 10+ (`minSdk 29`) native Compose application
- Media3 `MediaSessionService` for background playback, lock-screen controls, notifications, audio focus, Bluetooth, and headset controls
- Responsive Home, Search, Library, Echo Find, and Profile navigation
- Persistent mini-player and full-screen Now Playing experience
- Local music discovery through `MediaStore`
- Runtime-only local-audio and microphone permission flows
- Android Keystore-backed token vault
- Room, DataStore, WorkManager, Paging, Retrofit, OkHttp, Coil, Hilt, and Kotlin Serialization foundations
- Dark, light, AMOLED-ready, dynamic-colour, and Rock Red theme support
- Pull-request CI for lint, unit tests, and debug APK assembly

## How actions work

Rock Music now uses a single source-aware policy engine for playback, downloads, sharing, recognition, official-provider handoff, and listening rooms.

Every action identifies the media origin, resolves provider access and permissions, and receives one decision:

- execute inside Rock Music;
- open the official provider;
- request configuration;
- request authentication;
- report offline;
- block with a clear reason.

The current local-library playback path already uses this policy. New provider integrations, download workers, Echo Find, and Listen Together must use the same boundary and may not call services directly from UI code.

See [docs/OPERATING_MODEL.md](docs/OPERATING_MODEL.md) and [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Integration contract

Rock Music will include all requested provider-backed options:

- Spotify OAuth playlist-metadata import
- Licensed Echo Find recognition
- Listen Together REST and WebSocket rooms
- Optional Discord listening activity
- Licensed music-service adapters
- Synchronized lyrics providers
- Podcast-directory search
- Provider-permitted offline downloads
- User-authorised cloud storage
- Official YouTube and YouTube Music playback or deep links

Every integration must report an explicit state before its controls become actionable: available, unconfigured, authentication required, offline, unsupported, or error. Missing credentials and services are shown as unavailable; Rock Music never fabricates a connection, recognition result, room, import, download, or sharing success.

See [docs/INTEGRATIONS.md](docs/INTEGRATIONS.md) for configuration keys, capability contracts, privacy requirements, and fallback behaviour.

## Compliance boundary

Rock Music does not bypass advertising, DRM, subscriptions, geographic restrictions, integrity checks, or provider-controlled download rules. It does not extract protected streams or copy protected Spotify, YouTube, YouTube Music, or other licensed-service audio.

Offline storage is enabled only for local files, downloadable podcast enclosures, user-authorised cloud files, public-domain or appropriately licensed catalogues, and licensed provider content that explicitly grants download permission.

Provider-controlled playback or offline access is handed to the official provider application rather than reconstructed inside Rock Music.

See [docs/COMPLIANCE.md](docs/COMPLIANCE.md) for the complete policy.

## Build

Install JDK 17 and Android SDK 36, then run:

```bash
gradle :app:assembleDebug
```

The CI workflow installs Gradle automatically. A Gradle wrapper can be generated later with `gradle wrapper` and committed with the wrapper JAR.

## Delivery plan

The native foundation and source-aware action policy are implemented. Provider-backed services and the complete local, podcast, lyrics, download, Echo Find, and Listen Together experiences remain tracked work and are not considered complete until their tests and fallback states pass.

See [docs/ROADMAP.md](docs/ROADMAP.md).

## Licence

Rock Music is licensed under [GPL-3.0-only](LICENSE). Binary releases must provide the corresponding source and preserve applicable third-party notices.
