# Provider configuration

Rock Music exposes ten provider adapters through generated `BuildConfig` fields and a runtime Connections screen. Empty values are valid for pull-request and FOSS builds: the corresponding adapter reports `Unconfigured` and its actions remain disabled.

## Supplying configuration

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

## Configuration matrix

| Adapter | Required properties | Runtime status after configuration | Capability boundary |
| --- | --- | --- | --- |
| Spotify PKCE | `ROCK_SPOTIFY_CLIENT_ID`, `ROCK_SPOTIFY_REDIRECT_URI` | `AuthenticationRequired` until the user authorises | Playlist metadata import and official playback only |
| Echo Find | `ROCK_ECHO_FIND_BASE_URL`, `ROCK_ECHO_FIND_API_KEY` | `Available` for consent and connectivity checks | Short-sample recognition through a licensed provider |
| Listen Together | `ROCK_LISTEN_TOGETHER_REST_URL`, `ROCK_LISTEN_TOGETHER_WS_URL` | `AuthenticationRequired` | REST room lifecycle and authenticated WebSocket events |
| Discord | `ROCK_DISCORD_CLIENT_ID`, `ROCK_DISCORD_REDIRECT_URI`, `ROCK_DISCORD_ACTIVITY_BACKEND_URL` | `AuthenticationRequired` | User-enabled activity sharing through an official supported API and controlled backend |
| Licensed catalogue | `ROCK_CATALOGUE_BASE_URL`, `ROCK_CATALOGUE_API_KEY` | `AuthenticationRequired` | Search and provider-authorised streaming or official handoff |
| Lyrics | `ROCK_LYRICS_BASE_URL`, `ROCK_LYRICS_API_KEY` | `Available` | Synchronized lyrics only under provider terms |
| Podcast search | `ROCK_PODCAST_SEARCH_BASE_URL`, `ROCK_PODCAST_SEARCH_API_KEY` | `Available` | Directory search; direct RSS import remains independent |
| Permitted downloads | `ROCK_DOWNLOADS_BASE_URL`, `ROCK_DOWNLOADS_API_KEY` | `Available` | Item-level grant and revalidation; denied by default |
| Cloud storage | `ROCK_CLOUD_CLIENT_ID`, `ROCK_CLOUD_REDIRECT_URI` | `AuthenticationRequired` | User-authorised files only |
| YouTube / YouTube Music | none | `Available` | Validated official Android app or browser routes only |

## Android security model

An Android APK cannot keep a private secret. Anything placed in `BuildConfig` can be recovered from the installed package.

Allowed in the app:

- public OAuth client IDs;
- redirect URIs;
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

## OAuth state and verifier storage

OAuth state and PKCE verifiers are temporary credentials. Production implementations must encrypt them at rest, bind them to the expected redirect and provider, consume them exactly once, never log them, and clear expired records. Redirect callbacks must validate the exact scheme, authority, path, state, and request lifetime before exchanging a code.

## Status rules

The runtime registry reports one of the shared states:

- `Available`: public configuration is complete and no account sign-in is required before the next runtime check;
- `Unconfigured`: one or more required Gradle properties are blank;
- `AuthenticationRequired`: public app configuration is present but the user has not authorised the account;
- `Offline`: a configured provider cannot be reached;
- `Unsupported`: the device or provider cannot perform the capability;
- `Error`: the provider returned a recoverable or permanent failure.

Configuration status is not treated as proof that a remote service is healthy. Provider adapters must still perform connectivity, authentication, entitlement, regional-access and item-capability checks through `MediaActionPolicyEngine` before executing an action.

See [`PROVIDER_CONTRACTS.md`](PROVIDER_CONTRACTS.md) for the request, response, entitlement, and routing contracts.
