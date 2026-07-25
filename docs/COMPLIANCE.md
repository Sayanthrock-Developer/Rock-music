# Compliance and provider-use policy

Rock Music is designed for lawful playback and user-authorised media access. No feature may bypass provider controls or make an unsupported action appear successful.

## Prohibited implementations

Rock Music must not:

- remove, suppress, skip, or bypass provider advertisements;
- extract protected stream URLs, signatures, ciphers, tokens, or media segments;
- circumvent DRM, subscriptions, paywalls, geographic restrictions, age gates, or provider authentication;
- impersonate an official client or defeat provider integrity checks;
- download or export media when the source has not explicitly granted download permission;
- copy protected Spotify, YouTube, YouTube Music, or other licensed-service audio;
- upload local audio, microphone recordings, listening activity, or account data without clear user consent;
- show fake connection, room, recognition, import, download, or sharing success states.

## Permitted sources

Rock Music may play or download content from:

- user-owned local audio files;
- user-authorised cloud files;
- podcast RSS enclosures whose publisher permits downloading;
- public-domain and Creative Commons catalogues according to their licence terms;
- licensed music-service APIs according to the user’s account and provider capabilities;
- official YouTube or YouTube Music playback and deep-link integrations;
- any other source that explicitly grants the requested playback or download right.

## Provider capability checks

Provider adapters must report capabilities before actions are enabled. Downloads default to denied. An item-level permission result must be checked before starting, resuming, retrying, exporting, or sharing a downloaded file.

The app must preserve the provider’s denial reason and surface it to the user in plain language.

## Privacy requirements

- Microphone permission is requested only when Echo Find is activated.
- Raw recognition samples are temporary unless the user explicitly chooses to retain them.
- Discord listening activity is off by default and fully revocable.
- Spotify access is limited to user-authorised playlist metadata scopes.
- Listen Together shares only the room data required for synchronization and social controls.
- Local audio is never uploaded without explicit consent.
- Users can delete listening history, search history, recognition history, downloaded data and connected-account tokens.

## External-service failures

When a service is missing or unavailable, Rock Music must show one of the shared integration states: unconfigured, authentication required, offline, unsupported, or error. Retry actions must only appear for recoverable failures.

A placeholder response, generated room code, sample OAuth code, empty audio upload, or fabricated match must never be presented as a successful result.

## Licensing

Rock Music is licensed under GPL-3.0-only. Source distributions and binary releases must provide the Corresponding Source required by the licence and retain third-party notices.
