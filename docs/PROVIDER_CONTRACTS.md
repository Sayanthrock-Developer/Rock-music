# Provider contracts and security boundaries

Rock Music treats every remote integration as an explicit capability contract. A configured URL or client ID is not proof that an operation is authorised. Runtime clients must still verify authentication, consent, entitlement, item capability, region, expiry, and network state before doing work.

These contracts intentionally do not contain private credentials or fake success implementations. Empty public configuration remains valid for CI and FOSS builds.

## Spotify OAuth PKCE

`SpotifyPkceRequestFactory` generates a fresh RFC 7636 S256 verifier, challenge, and CSRF state for every request. It validates the public client ID, rejects insecure redirect URIs, limits scopes to those Rock Music uses, and gives each request a ten-minute lifetime.

Callbacks are accepted only when:

- the scheme, authority, and path exactly match the configured redirect URI;
- the request has not expired;
- the returned state matches in constant time;
- callback parameters are not duplicated;
- either one authorization code or one provider error is present.

`SpotifyPkceRequestStore` is the consume-once persistence contract. A production implementation must encrypt the verifier and state at rest, never log either value, delete the record after callback consumption, and clear expired requests.

### Spotify playlist preview

`SpotifyPlaylistReferenceParser` accepts only a 22-character Spotify playlist ID from either an exact `spotify:playlist:` URI or an HTTPS `open.spotify.com/playlist/` link. Share and tracking query parameters are discarded when the canonical URL is produced. Track, album, artist, HTTP, credential-bearing, malformed, and non-Spotify links are rejected.

`SpotifyPlaylistClient` uses the user-authorised PKCE token stored in Android Keystore-backed storage. It refreshes an expiring token with the public mobile client ID and refresh token, requests playlist metadata from the official Spotify Web API, skips unavailable or non-track entries safely, and exposes only display metadata to the UI. Bearer and refresh tokens are never embedded in source, displayed, or logged.

Rock Music may display playlist artwork, owner, track count, and a short track preview. Playback, saving, and full playlist interaction continue through Spotify's official application or web route. The preview does not provide audio extraction, protected downloads, ad bypass, or subscription bypass.

## Licensed Echo Find

`EchoFindRecognitionRequest` carries the short audio sample, MIME type, duration, SHA-256 digest, locale, and an explicit `RecognitionConsent` record. A production adapter must enforce sample-size and duration limits before upload, use HTTPS, and avoid provider history unless the consent flag allows it.

Recognition results include provider identity, confidence, terms attribution, and an optional history identifier so deletion can be routed to the licensed provider.

## Listen Together

The contract is split into two boundaries:

- `ListenTogetherRestContract` manages create, join, and leave operations;
- `ListenTogetherRealtimeContract` sends and observes ordered WebSocket events.

Realtime events use an event ID, monotonically increasing sequence, room revision, sender, and server timestamp. Clients must reject stale revisions, deduplicate event IDs, resume after the last accepted sequence, and let the server remain authoritative for host transfer and playback time.

## Discord activity

Discord configuration requires:

- `ROCK_DISCORD_CLIENT_ID`;
- `ROCK_DISCORD_REDIRECT_URI`;
- `ROCK_DISCORD_ACTIVITY_BACKEND_URL`.

`DiscordActivityConfigurationFactory` validates a Discord application ID, a secure redirect target, and an HTTPS backend URL. Activity sharing remains disabled until the user enables it. Private OAuth credentials and any privileged Discord token exchange must stay on the backend.

## Licensed catalogue

`CatalogueSearchRequest` adds paging, market, explicit-content policy, and result limits. `CataloguePlaybackRequest` produces a `CataloguePlaybackGrant` containing an entitlement ID, expiry, playback route, and whether official handoff is mandatory.

A provider track is metadata only. It must never be treated as a playable stream until the item-level playback grant succeeds.

## Synchronized lyrics

`LyricsRequest` identifies the licensed provider track, requested language, optional translation, and whether word timing is needed. `SynchronizedLyricsDocument` preserves provider attribution and terms alongside line and word timing.

Clients must preserve attribution, obey caching limits, and not synthesize provider success when lyrics are unavailable.

## Podcast search

`PodcastSearchRequest` supports bounded page size, continuation token, language, and explicit-content policy. `PodcastSearchPage` returns directory metadata only. Playback and downloads continue to use the enclosure URL and permission rules from the podcast feed.

## Permitted downloads

A download is denied by default. `DownloadGrant` is usable only when its `DownloadGrantPermission.permitted` value is true and `DownloadGrantValidator` confirms:

- provider and media IDs are non-blank and match the requested item;
- the grant is within its not-before and expiry window;
- the URL is HTTPS;
- any SHA-256 value is correctly formed.

Production workers must revalidate the exact item immediately before transfer and again before retrying an expired or interrupted transfer. A grant for one account, item, or provider must never be reused for another.

## User-authorised cloud storage

`CloudAuthorizationRequest` carries state, redirect URI, lifetime, and optional PKCE values. File listings, playback access, and download grants operate only on files visible to the authorising user.

A production adapter must request the minimum read-only scope, store refresh tokens in Android Keystore-backed storage, revoke access on disconnect, and never upload or modify user files unless a separate capability is added and explicitly authorised.

## Official YouTube and YouTube Music routing

`YouTubeOfficialPlaybackProvider` never extracts media streams. It validates exact official hosts, HTTPS transport, supported watch/Shorts/playlist paths, single video or playlist identifiers, and bounded search text. It returns an `OfficialProviderRoute` containing a canonical web fallback, optional Android app URI, preferred official packages, route kind, and provider media ID.

Deceptive subdomains, unknown official-site paths, duplicate identifiers, embedded credentials, custom ports, fragments, and unsupported query parameters are rejected.
