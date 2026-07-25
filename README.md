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

Rock Music uses a single source-aware policy engine for playback, downloads, sharing, recognition, official-provider handoff, and listening rooms.

Every action identifies the media origin, resolves provider access and permissions, and receives one decision:

- execute inside Rock Music;
- open the official provider;
- request configuration;
- request authentication;
- report offline;
- block with a clear reason.

The current local-library playback path already uses this policy. Provider adapters, download workers, Echo Find, and Listen Together must use the same boundary and may not call services directly from UI code.

See [docs/OPERATING_MODEL.md](docs/OPERATING_MODEL.md) and [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## Provider adapters

Rock Music now includes typed adapter definitions, configuration keys, capability contracts, service interfaces, registry status, tests, and an in-app Connections screen for:

- Spotify OAuth PKCE playlist-metadata import;
- licensed Echo Find recognition;
- Listen Together REST and WebSocket rooms;
- optional Discord listening activity;
- licensed music-service catalogues;
- synchronized lyrics providers;
- podcast-directory search;
- provider-permitted downloads;
- user-authorised cloud storage;
- official YouTube and YouTube Music playback or deep links.

Open the Connections button in the app to see which adapters are available, require sign-in, or are missing configuration.

Empty configuration is supported for CI and FOSS builds. A missing provider displays `Unconfigured`; an OAuth provider with public app configuration displays `AuthenticationRequired` until the user authorises it. Rock Music never fabricates a connection, recognition result, room, import, download, or sharing success.

The adapter foundations do not include provider credentials or hosted backend services. Real remote operations become available only after legitimate public client configuration, user authorisation, licensed endpoints, connectivity checks, entitlement checks and policy-engine approval.

See:

- [docs/PROVIDER_CONFIGURATION.md](docs/PROVIDER_CONFIGURATION.md)
- [docs/provider-config.properties.example](docs/provider-config.properties.example)
- [docs/INTEGRATIONS.md](docs/INTEGRATIONS.md)

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

Provider values can be supplied through `~/.gradle/gradle.properties`, CI variables/secrets, or `-P` command-line properties. Private provider secrets must stay on a licensed backend and must not be embedded in the Android APK.

The CI workflow installs Gradle automatically. A Gradle wrapper can be generated later with `gradle wrapper` and committed with the wrapper JAR.

## Delivery plan

The native foundation, source-aware action policy, provider definitions, configuration registry, Connections UI, and service contracts are implemented. Real provider clients, hosted backends, OAuth callback completion, download workers, synchronized room transport, and production credentials remain tracked work and are not considered complete until their integration tests and fallback states pass.

See [docs/ROADMAP.md](docs/ROADMAP.md).

## Licence

Rock Music is licensed under [GPL-3.0-only](LICENSE). Binary releases must provide the corresponding source and preserve applicable third-party notices.
