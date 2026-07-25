# External integrations

Rock Music includes every requested integration behind a common availability and capability contract. Provider-backed options must fail closed: when configuration, authentication, connectivity, legal access, or provider support is missing, the app displays an explicit unavailable state and never reports success.

Secrets must not be committed to the repository or bundled as plain text. Public client identifiers and redirect URIs may be supplied through local Gradle properties or CI secrets. Tokens are stored with Android Keystore-backed encryption.

## Availability states

Every integration must expose one of these states before its UI becomes actionable:

- `Available`
- `Unconfigured(missingKeys)`
- `AuthenticationRequired`
- `Offline`
- `Unsupported(reason)`
- `Error(message, retryable)`

The reusable Compose component `IntegrationUnavailableCard` renders the unavailable states with screen-reader output and an optional recovery action.

## Configuration matrix

| Integration | Required configuration | Supported behaviour | Unconfigured behaviour |
| --- | --- | --- | --- |
| Spotify playlist import | `ROCK_SPOTIFY_CLIENT_ID`, `ROCK_SPOTIFY_REDIRECT_URI` | OAuth PKCE, read authorised playlist metadata, match tracks, manual correction, save imported playlist | Show “Spotify configuration required”; do not start OAuth |
| Echo Find | `ROCK_ECHO_FIND_BASE_URL`, `ROCK_ECHO_FIND_API_KEY` | Record only after microphone consent, submit a short sample to a licensed recognition provider, show confidence, save history | Show “Recognition provider unavailable”; do not record or upload audio |
| Listen Together | `ROCK_LISTEN_TOGETHER_REST_URL`, `ROCK_LISTEN_TOGETHER_WS_URL` | Create/join rooms, sync play/pause/seek/queue, chat, reactions, voting, host transfer, reconnect and drift correction | Show “Room service unavailable”; do not generate fake room codes |
| Discord activity | `ROCK_DISCORD_CLIENT_ID` plus an official supported SDK/API | Optional title, artist, album, playback state and elapsed-time sharing | Keep sharing disabled and explain that Discord is not configured or unsupported on the device |
| Licensed music services | Provider-specific OAuth client IDs, redirect URIs and catalogue endpoints | Search, stream and open official playback only where the provider grants access | Hide provider results or show an unavailable provider card |
| Lyrics provider | `ROCK_LYRICS_BASE_URL`, optional `ROCK_LYRICS_API_KEY` | Synchronized/unsynchronized lyrics, translation where licensed, caching and tap-to-seek | Fall back to embedded/local lyrics; otherwise show “Lyrics unavailable” |
| Podcast search | `ROCK_PODCAST_SEARCH_BASE_URL`, optional `ROCK_PODCAST_SEARCH_API_KEY` | Search a licensed directory, open RSS feeds, follow podcasts and refresh episodes | Keep direct RSS import available; show “Podcast search unavailable” |
| Permitted downloads | Provider capability response and item-level permission | Download only local files, authorised cloud files, downloadable podcast enclosures, or licensed catalogue items with explicit permission | Disable Download and show the provider’s denial reason |
| User-authorised cloud storage | Provider OAuth client configuration | Browse and play files the user owns or is authorised to access | Show “Cloud provider not connected” |
| YouTube/YouTube Music | Official Android intent/deep-link or provider-approved playback integration | Open or control official playback within provider rules | Show “Official playback unavailable”; never extract media URLs |

## Spotify requirements

- Use Authorization Code with PKCE.
- Never ship a Spotify client secret in the Android application.
- Request only the scopes required to read user-authorised playlist metadata.
- Never copy, cache, export, or download protected Spotify audio.
- Matching results must be grouped as matched, uncertain, and unavailable.

## Echo Find requirements

- Ask for microphone permission only after the user activates Echo Find.
- Display a visible recording indicator and stop control.
- Upload only the minimum sample needed by the licensed provider.
- Do not retain raw microphone audio unless the user explicitly opts in.
- Provide recognition-history deletion controls.

## Listen Together requirements

- Every participant must authenticate where required and have legal access to the selected source.
- The backend must reject playback commands for participants without provider access.
- Room state must use server timestamps, reconnect safely, and apply bounded drift correction.
- Chat, reactions, voting and host transfer require server acknowledgement; the UI must not fabricate success.

## Download permission contract

Downloads are denied by default. A provider must explicitly return permission for the selected item. The app records the permission reason and optional expiry time and revalidates permission before retrying or resuming.

A download must be blocked when any of these conditions applies:

- The source does not grant download rights.
- The item is protected by DRM or provider-controlled offline rules not available through the official API.
- The user’s subscription or geographic access does not permit the action.
- The authorisation token is missing, expired, or revoked.
- The app is offline and no valid cached permission exists.

## CI and release configuration

CI may receive public client IDs and non-secret service URLs from repository variables. API keys, signing material and private credentials must use encrypted repository or environment secrets. Pull-request builds must work without secrets by compiling provider adapters in `Unconfigured` mode.
