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

## Phase 2 — Local music and library

- [ ] Folder browsing and exclusion
- [ ] Embedded artwork and metadata editing
- [ ] Duplicate detection
- [ ] Playlists, favourites, history, sorting, filtering, folders, and bulk selection
- [ ] Gapless playback, ReplayGain, crossfade, equalizer, bass boost, and loudness controls
- [ ] Media notification custom commands, Android Auto, audio output selector, and headset controls
- [ ] Persistent queue, shuffle/repeat memory, sleep timer, mono audio, and fade controls
- [ ] Loading, empty, offline, error, retry, accessibility, unit-test, and UI-test coverage

## Phase 3 — Podcasts and permitted offline downloads

- [ ] Direct RSS import, discovery adapter, and feed parser
- [ ] Follow/unfollow, chapters, speed, silence skipping, volume boost, queue, and progress
- [ ] Podcast-directory provider configuration and explicit unavailable state
- [ ] WorkManager download queue with pause, resume, retry, Wi-Fi policy, cleanup, and storage dashboard
- [ ] Smart downloads, offline-only mode, quality selection, and storage location
- [ ] Item-level capability checks that block downloads when a source does not grant permission
- [ ] Revalidation before download resume/retry and clear provider denial reasons
- [ ] Loading, empty, offline, error, retry, accessibility, unit-test, and UI-test coverage

## Phase 4 — Lyrics and visuals

- [ ] Embedded/local lyrics fallback
- [ ] Configurable synchronized-lyrics provider adapter
- [ ] Line and word timing parser
- [ ] Tap-to-seek, translation, romanization, text scaling, offline cache, and unsynchronized fallback
- [ ] Provider priority and explicit unavailable/error states
- [ ] Battery-aware gradients, artwork motion, particles, artist canvas, and waveform modes
- [ ] Reduced-motion, battery-saver, and high-refresh-rate behaviour
- [ ] Loading, empty, offline, error, retry, accessibility, unit-test, and UI-test coverage

## Phase 5 — Connected services

- [ ] OAuth connection framework, token refresh, revocation, and Keystore storage
- [ ] Spotify Authorization Code with PKCE
- [ ] Spotify playlist metadata import with matched, uncertain, unavailable, and manual-correction states
- [ ] Licensed catalogue provider interface with per-item capabilities
- [ ] Official YouTube/YouTube Music playback or deep links only
- [ ] User-authorised cloud storage adapters
- [ ] Optional Discord activity sharing through an official supported SDK/API
- [ ] Public client-ID and redirect-URI build configuration; no mobile client secrets
- [ ] Explicit unconfigured, authentication-required, offline, unsupported, and error states
- [ ] Loading, empty, offline, error, retry, accessibility, unit-test, and UI-test coverage

## Phase 6 — Echo Find and Listen Together

- [ ] Licensed recognition-provider adapter
- [ ] Consent-first short audio recording and temporary sample handling
- [ ] Result confidence, service links, playlist insertion, and encrypted recognition history
- [ ] Recognition-history and retained-sample deletion controls
- [ ] Listen Together REST API and authenticated WebSocket client
- [ ] Create/join rooms, invite links/codes, host controls, host transfer, chat, reactions, voting, and reconnect
- [ ] Server acknowledgement for every social or playback action; no fabricated success states
- [ ] Clock-offset estimation and bounded adjustable drift correction
- [ ] Per-participant legal-access validation
- [ ] Explicit unconfigured and unavailable states when backend URLs or credentials are absent
- [ ] Loading, empty, offline, error, retry, accessibility, unit-test, and UI-test coverage

## Phase 7 — Privacy, accessibility, release hardening

- [ ] Discord, Spotify, Echo Find, cloud, and room privacy controls
- [ ] Delete listening, search, recognition, download, and account data
- [ ] Backup and restore with sensitive-token exclusion
- [ ] High contrast, scalable text, reduced motion, colour-safe status, and tablet keyboard navigation
- [ ] Phone, tablet, foldable, landscape, and Android Auto validation
- [ ] Dependency, licence, privacy, and source-availability review for GPL releases
- [ ] Signed release workflow, APK artifact validation, and reproducible release notes

## Definition of done

A feature is complete only when its real implementation is connected and it has loading, empty, offline, error, retry, permission, accessibility, unit-test, UI-test, privacy, analytics-boundary, provider-capability, and external-service fallback coverage.

A screen, button, placeholder, sample response, generated room code, fake OAuth code, or stubbed provider result does not count as implementation.
