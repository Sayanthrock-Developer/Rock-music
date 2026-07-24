# Architecture

Rock Music uses feature-oriented Clean Architecture with a single Android app module for the initial foundation. As service integrations mature, features can move into independent Gradle modules without changing domain contracts.

## Layers

- **Presentation:** Jetpack Compose screens, responsive navigation, permission flows, and ViewModels.
- **Player:** Media3 `MediaSessionService` plus a UI-facing `MediaController` connection.
- **Domain:** Provider-neutral tracks, lyrics, room state, podcast, and download policies.
- **Data:** MediaStore, Room, DataStore, Retrofit APIs, RSS, WebSockets, and provider adapters.
- **Security:** Android Keystore-backed token vault and explicit privacy controls.

## Source policy

Every playable item must carry a source type and capability policy. Download, sharing, room synchronization, and external playback actions are enabled only when that source explicitly allows them.

## Service boundaries

- YouTube/YouTube Music: official playback or deep-link integration only.
- Spotify: OAuth playlist metadata import only; protected audio is never copied.
- Echo Find: short samples go only to a licensed recognition provider after explicit microphone activation.
- Listen Together: each participant resolves and plays a track through a source they can legally access.
- Discord: optional activity sharing with a visible off switch and token revocation.

## State requirements

Every feature owns loading, content, empty, offline, permission-denied, service-unavailable, error, and retry states. Analytics events must never include raw local filenames, microphone audio, authentication tokens, private room messages, or sensitive search content.
