# Provider configuration

Rock Music exposes ten provider adapters through generated `BuildConfig` fields and the runtime **Connections & Unlock** screen. Empty values are valid for pull-request and FOSS builds: the corresponding adapter remains locked until valid public configuration is supplied.

## Supplying managed configuration

Add public mobile configuration to `~/.gradle/gradle.properties`, pass it with `-P`, or inject it through CI repository variables/secrets.

Do not commit credentials to this repository.

```properties
ROCK_SPOTIFY_CLIENT_ID=
ROCK_SPOTIFY_REDIRECT_URI=
ROCK_ECHO_FIND_BASE_URL=
ROCK_ECHO_FIND_API_KEY=
ROCK_LISTEN_TOGETHER_REST_URL=
ROCK_LISTEN_TOGETHER_WS_URL=
ROCK_DISCORD_CLIENT_ID=
ROCK_DISCORD_REDIRECT_URI=
ROCK_DISCORD_ACTIVITY_BACKEND_URL=
ROCK_CATALOGUE_BASE_URL=
ROCK_CATALOGUE_API_KEY=
ROCK_LYRICS_BASE_URL=
ROCK_LYRICS_API_KEY=
ROCK_PODCAST_SEARCH_BASE_URL=
ROCK_PODCAST_SEARCH_API_KEY=
ROCK_DOWNLOADS_BASE_URL=
ROCK_DOWNLOADS_API_KEY=
ROCK_CLOUD_CLIENT_ID=
ROCK_CLOUD_REDIRECT_URI=
```

Then build normally:

```bash
gradle :app:assembleDebug
```

A one-off command can provide a value without changing a Gradle properties file:

```bash
gradle :app:assembleDebug \
  -PROCK_SPOTIFY_CLIENT_ID=your-public-client-id \
  -PROCK_SPOTIFY_REDIRECT_URI=rockmusic://oauth/spotify
```

## Runtime unlock

The Connections & Unlock screen accepts the same public values at runtime. Values entered by the user are encrypted through Android Keystore before storage.

- **Unlock** enables a provider after all required values pass validation.
- **Lock** disables the provider while retaining its encrypted values.
- **Reset** removes encrypted overrides and provider authorization, then falls back to managed build configuration.
- Managed nonblank configuration is not treated as ready until it passes the same validation rules as runtime input.
- Changing a provider identity value, such as an OAuth client ID or redirect URI, invalidates the previous authorization state.

## Configuration matrix

| Adapter | Required properties | Runtime status after configuration | Capability boundary |
| --- | --- | --- | --- |
| Spotify PKCE | `ROCK_SPOTIFY_CLIENT_ID`, `ROCK_SPOTIFY_REDIRECT_URI` | `AuthenticationRequired` until the user authorises; `Available` after callback and token exchange | Playlist metadata import and official playback only |
| Echo Find | `ROCK_ECHO_FIND_BASE_URL`, `ROCK_ECHO_FIND_API_KEY` | `Available` for consent and connectivity checks | Short-sample recognition through a licensed provider |
| Listen Together | `ROCK_LISTEN_TOGETHER_REST_URL`, `ROCK_LISTEN_TOGETHER_WS_URL` | `AuthenticationRequired` | REST room lifecycle and authenticated WebSocket events |
| Discord | `ROCK_DISCORD_CLIENT_ID`, `ROCK_DISCORD_REDIRECT_URI`, `ROCK_DISCORD_ACTIVITY_BACKEND_URL` | `AuthenticationRequired` | User-enabled activity sharing through an official supported API and controlled backend |
| Licensed catalogue | `ROCK_CATALOGUE_BASE_URL`, `ROCK_CATALOGUE_API_KEY` | `AuthenticationRequired` | Search and provider-authorised streaming or official handoff |
| Lyrics | `ROCK_LYRICS_BASE_URL`, `ROCK_LYRICS_API_KEY` | `Available` | Synchronized lyrics only under provider terms |
| Podcast search | `ROCK_PODCAST_SEARCH_BASE_URL`, `ROCK_PODCAST_SEARCH_API_KEY` | `Available` | Directory search; direct RSS import remains independent |
| Permitted downloads | `ROCK_DOWNLOADS_BASE_URL`, `ROCK_DOWNLOADS_API_KEY` | `Available` | Item-level grant and revalidation; denied by default |
| Cloud storage | `ROCK_CLOUD_CLIENT_ID`, `ROCK_CLOUD_REDIRECT_URI` | `AuthenticationRequired` | User-authorised files only |
| YouTube / YouTube Music | none | `Available` | Validated official Android app or browser routes only |

## Registered callback routes

Rock Music validates provider callbacks exactly. The built-in private callbacks are:

```text
rockmusic://oauth/spotify
rockmusic://oauth/discord
rockmusic://oauth/cloud
```

Spotify authorization callbacks are registered in the Android manifest and are validated against the encrypted pending PKCE request before the authorization code is exchanged. A provider dashboard must allowlist the exact callback used in the request. HTTPS app links may be used only when the corresponding Android manifest host and Digital Asset Links association are configured for the release.

## Android security model

An Android APK cannot keep a private secret. Anything placed in `BuildConfig` or entered into the app must be treated as recoverable from the installed package or device.

Allowed in the app:

- public OAuth client IDs;
- registered redirect URIs;
- public service URLs;
- provider-issued publishable and Android-restricted mobile keys.

Must remain on the backend:

- OAuth client secrets;
- signing secrets;
- privileged catalogue credentials;
- unrestricted recognition, lyrics, podcast, download, storage, or Discord credentials;
- webhook verification secrets;
- database and infrastructure credentials.

When a provider requires a private secret, Rock Music calls a controlled backend that holds the secret and returns only the minimum user-authorised result.

## OAuth state, verifier, and token storage

OAuth state and PKCE verifiers are temporary credentials. Rock Music stores the pending Spotify PKCE request as one encrypted record, binds it to the expected redirect and provider, verifies its lifetime and state, consumes it exactly once, and never logs it. The returned Spotify token response is also stored as one encrypted record.

## Status rules

The runtime registry reports one of the shared states:

- `Locked`: the provider has not been explicitly unlocked or its managed configuration is invalid;
- `Available`: public configuration is valid and all required authorization checks have passed;
- `Unconfigured`: one or more required values are blank;
- `AuthenticationRequired`: public app configuration is present but the user has not authorised the account;
- `Offline`: a configured provider cannot be reached;
- `Unsupported`: the device or provider cannot perform the capability;
- `Error`: the provider returned a recoverable or permanent failure.

Configuration and authorization status are not proof that a remote service is healthy. Provider adapters must still perform connectivity, entitlement, regional-access and item-capability checks through `MediaActionPolicyEngine` before executing an action.

See [`PROVIDER_CONTRACTS.md`](PROVIDER_CONTRACTS.md) for the request, response, entitlement, and routing contracts.
