# Architecture

Rock Music uses feature-oriented Clean Architecture with a single Android app module for the initial foundation. As service integrations mature, features can move into independent Gradle modules without changing domain contracts.

## Layers

- **Presentation:** Jetpack Compose screens, responsive navigation, permission flows, ViewModels, and consistent action-decision UI.
- **Policy:** `MediaActionPolicyEngine` is the mandatory decision point for playback, downloads, sharing, recognition, official-provider handoff, and listening rooms.
- **Player:** Media3 `MediaSessionService` plus a UI-facing `MediaController` connection.
- **Domain:** Provider-neutral tracks, lyrics, room state, podcast, integration, capability, and download contracts.
- **Data:** MediaStore, Room, DataStore, Retrofit APIs, RSS, WebSockets, and provider adapters.
- **Security:** Android Keystore-backed token vault and explicit privacy controls.

## Mandatory action pipeline

Every user action follows this sequence:

1. Identify the media origin.
2. Identify the requested operation.
3. Resolve provider configuration, authentication, connectivity, local availability, region, entitlement, capability, permission, consent, and participant access.
4. Ask `MediaActionPolicyEngine` for a decision.
5. Execute only that decision.

Screens, ViewModels, workers, provider adapters, and playback services must not bypass this pipeline.

## Decision types

The policy returns one of:

- execute in Rock Music;
- open the official provider;
- require configuration;
- require authentication;
- offline;
- blocked with an exact reason.

The shared `MediaActionDecisionCard` renders all non-success decisions consistently and accessibly.

## Source policy

Every playable item carries an origin and capability policy. The supported origins are local file, podcast RSS, user cloud, licensed catalogue, official provider link, public domain, Creative Commons, and unknown.

Remote items are playable offline only when `locallyAvailable` is true. Source type alone never proves that media bytes are cached.

Downloads are denied by default. Item-level permission and provider capability must both be granted before start, resume, or retry.

## Playback paths

- **In-app playback:** local, user-owned, openly licensed, permitted podcast, or explicitly licensed in-app playback.
- **Official-provider handoff:** provider-controlled DRM, advertisements, subscriptions, offline storage, or playback.

Rock Music never reconstructs a protected provider stream.

## Service boundaries

- YouTube/YouTube Music: official playback or deep-link integration only.
- Spotify: OAuth playlist metadata import only; protected audio is never copied.
- Echo Find: short samples go only to a licensed recognition provider after explicit microphone activation and policy approval.
- Listen Together: each participant resolves and plays a track through a source they can legally access; room actions require server acknowledgement.
- Discord: optional activity sharing with a visible off switch and token revocation.

## State requirements

Every feature owns loading, content, empty, offline, permission-denied, service-unavailable, error, and retry states. Analytics events must never include raw local filenames, microphone audio, authentication tokens, private room messages, or sensitive search content.

See [OPERATING_MODEL.md](OPERATING_MODEL.md) for the complete execution rules.
