# Rock Music delivery roadmap

## Phase 1 — Native foundation

- [x] Android 10+ Compose project
- [x] Media3 background playback service
- [x] Mini-player and Now Playing vertical slice
- [x] Responsive bottom navigation and tablet rail
- [x] MediaStore local-audio scan
- [x] Runtime local-audio permission handling
- [x] Echo Find microphone permission boundary
- [x] Room database and Keystore token vault
- [x] CI lint, tests, and debug APK
- [x] Shared provider capability and explicit availability-state contract
- [x] Accessible unavailable-state Compose component
- [x] GPL-3.0-only and compliance documentation
- [x] Central `MediaActionPolicyEngine`
- [x] Source-aware play/download/share/recognition/room decisions
- [x] Official-provider handoff decision path
- [x] Cached-versus-remote offline distinction
- [x] Existing local playback routed through the policy engine
- [x] Shared media-action decision UI and unit tests
- [x] Public provider configuration exposed through Gradle properties and generated `BuildConfig`
- [x] Typed definitions and service contracts for all ten requested integrations
- [x] Runtime provider registry with explicit setup/sign-in readiness states
- [x] In-app Connections screen and provider-definition tests
- [x] Provider configuration template and Android secret-handling guidance

## Phase 2 — Local music and library

- [ ] Folder browsing and exclusion
- [ ] Embedded artwork and metadata editing
- [ ] Duplicate detection
- [ ] Playlists, favourites, history, sorting, filtering, folders, and bulk selection
- [ ] Gapless playback, ReplayGain, crossfade, equalizer, bass boost, and loudness controls
- [ ] Media notification custom commands, Android Auto, audio output selector, and headset controls
- [ ] Persistent queue, shuffle/repeat memory, sleep timer, mono audio, and fade controls
- [ ] Route every local playback, share, export, ringtone, and file action through `MediaActionPolicyEngine`
- [ ] Loading, empty, offline, error, retry, accessibility, unit-test, and UI-test coverage

## Phase 3 — Podcasts and permitted offline downloads

- [ ] Direct RSS import, discovery client, and feed parser
- [ ] Follow/unfollow, chapters, speed, silence skipping, volume boost, queue, and progress
- [x] Podcast-directory and permitted-download configuration definitions and explicit unconfigured states
- [ ] Real licensed podcast-directory client
- [ ] Real item-level download-grant and revalidation client
- [ ] WorkManager download queue with pause, resume, retry, Wi-Fi policy, cleanup, and storage dashboard
- [ ] Smart downloads, offline-only mode, quality selection, and storage location
- [ ] Item-level capability checks that block downloads when a source does not grant permission
- [ ] Revalidation before download start, resume, and retry with clear provider denial reasons
- [ ] Route podcast playback and every download command through `MediaActionPolicyEngine`
- [ ] Loading, empty, offline, error, retry, accessibility, unit-test, and UI-test coverage

## Phase 4 — Lyrics and visuals

- [ ] Embedded/local lyrics fallback
- [x] Typed synchronized-lyrics configuration and service contract
- [ ] Real licensed synchronized-lyrics client
- [ ] Line and word timing parser
- [ ] Tap-to-seek, translation, romanization, text scaling, offline cache, and unsynchronized fallback
- [ ] Provider priority and explicit unavailable/error states
- [ ] Battery-aware gradients, artwork motion, particles, artist canvas, and waveform modes
- [ ] Reduced-motion, battery-saver, and high-refresh-rate behaviour
- [ ] Route provider lyric fetches and translations through configured integration availability
- [ ] Loading, empty, offline, error, retry, accessibility, unit-test, and UI-test coverage

## Phase 5 — Connected services

- [ ] OAuth connection framework, token refresh, revocation, and Keystore storage
- [x] Spotify PKCE configuration and typed service contract
- [ ] Spotify Authorization Code with PKCE browser/callback implementation
- [ ] Spotify playlist metadata import with matched, uncertain, unavailable, and manual-correction states
- [x] Licensed catalogue configuration, capabilities, and typed service contract
- [ ] Real licensed catalogue client with per-item entitlement checks
- [x] Official YouTube/YouTube Music handoff definition with no in-app protected streaming capability
- [ ] Official Android intent/deep-link builder and device-resolution tests
- [x] User-authorised cloud storage configuration and typed service contract
- [ ] Real cloud OAuth and file-access client
- [x] Discord configuration and typed activity-sharing contract
- [ ] Real Discord connection through an official supported SDK/API
- [x] Public client-ID, redirect-URI, endpoint and publishable-key build configuration; no private mobile secrets
- [x] Explicit unconfigured, authentication-required, offline, unsupported, and error state model
- [ ] Route catalogue playback, official handoff, cloud downloads, sharing, and Discord activity through `MediaActionPolicyEngine`
- [ ] Loading, empty, offline, error, retry, accessibility, unit-test, and UI-test coverage

## Phase 6 — Echo Find and Listen Together

- [x] Echo Find and Listen Together configuration definitions and typed service contracts
- [ ] Real licensed recognition-provider client
- [ ] Consent-first short audio recording and temporary sample handling
- [ ] Result confidence, service links, playlist insertion, and encrypted recognition history
- [ ] Recognition-history and retained-sample deletion controls
- [ ] Real Listen Together REST API and authenticated WebSocket client
- [ ] Create/join rooms, invite links/codes, host controls, host transfer, chat, reactions, voting, and reconnect
- [ ] Server acknowledgement for every social or playback action; no fabricated success states
- [ ] Clock-offset estimation and bounded adjustable drift correction
- [ ] Per-participant legal-access validation
- [x] Explicit unconfigured states when backend URLs or public configuration are absent
- [ ] Runtime offline, authentication, server-error and reconnect states
- [ ] Route recognition start, result opening, room creation, room join, and synced playback through `MediaActionPolicyEngine`
- [ ] Loading, empty, offline, error, retry, accessibility, unit-test, and UI-test coverage

## Phase 7 — Privacy, accessibility, release hardening

- [ ] Discord, Spotify, Echo Find, cloud, and room privacy controls
- [ ] Delete listening, search, recognition, download, and account data
- [ ] Backup and restore with sensitive-token exclusion
- [ ] High contrast, scalable text, reduced motion, colour-safe status, and tablet keyboard navigation
- [ ] Phone, tablet, foldable, landscape, and Android Auto validation
- [ ] Dependency, licence, privacy, and source-availability review for GPL releases
- [ ] Policy-bypass static checks and architecture tests
- [ ] Signed release workflow, APK artifact validation, and reproducible release notes

## Definition of done

A feature is complete only when its real implementation is connected and it has loading, empty, offline, error, retry, permission, accessibility, unit-test, UI-test, privacy, analytics-boundary, provider-capability, action-policy, and external-service fallback coverage.

A screen, button, configuration key, interface, placeholder, sample response, generated room code, fake OAuth code, stubbed provider result, or direct provider call that bypasses `MediaActionPolicyEngine` does not count as a completed remote integration.
