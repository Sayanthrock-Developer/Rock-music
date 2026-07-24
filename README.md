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

## Compliance boundary

Rock Music does not bypass advertising, DRM, subscriptions, geographic restrictions, or provider-controlled download rules. YouTube and YouTube Music content must use official playback or deep-link integrations. Offline storage is enabled only for local files, downloadable podcasts, user-owned cloud files, and licensed catalogues that explicitly grant download permission.

## Build

Install JDK 17 and Android SDK 36, then run:

```bash
gradle :app:assembleDebug
```

The CI workflow installs Gradle automatically. A Gradle wrapper can be generated later with `gradle wrapper` and committed with the wrapper JAR.

## Delivery plan

See [docs/ROADMAP.md](docs/ROADMAP.md) and [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).
