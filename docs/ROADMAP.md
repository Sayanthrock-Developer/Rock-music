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

## Phase 2 — Local music and library

- [ ] Folder browsing and exclusion
- [ ] Embedded artwork and metadata editing
- [ ] Duplicate detection
- [ ] Playlists, favourites, history, sorting, filtering, and bulk selection
- [ ] Gapless playback, ReplayGain, crossfade, equalizer, and loudness controls
- [ ] Media notification custom commands, Android Auto, and audio output selector

## Phase 3 — Podcasts and permitted offline downloads

- [ ] RSS discovery and feed parser
- [ ] Chapters, speed, silence skipping, queue, and listening progress
- [ ] WorkManager download queue with pause, resume, retry, Wi-Fi policy, cleanup, and storage dashboard
- [ ] Capability checks that block downloads when a source does not grant permission

## Phase 4 — Lyrics and visuals

- [ ] Line and word timing parser
- [ ] Tap-to-seek, translation, text scaling, offline cache, and unsynchronized fallback
- [ ] Battery-aware gradients, artwork motion, particles, canvas, and waveform modes
- [ ] Reduced-motion and battery-saver behaviour

## Phase 5 — Connected services

- [ ] OAuth connection framework and account revocation
- [ ] Licensed catalogue provider interface
- [ ] Official YouTube/YouTube Music playback or deep links
- [ ] Spotify playlist metadata import with matched, uncertain, unavailable, and manual-correction states
- [ ] User-authorized cloud storage adapters
- [ ] Optional Discord activity sharing

## Phase 6 — Echo Find and Listen Together

- [ ] Licensed recognition-provider adapter and encrypted recognition history
- [ ] Result confidence, external-service actions, and playlist insertion
- [ ] Room backend, invite links/codes, host transfer, chat, reactions, voting, and reconnect
- [ ] Clock-offset estimation and adjustable drift correction
- [ ] Per-participant legal-access validation

## Definition of done

A feature is complete only when it has loading, empty, offline, error, retry, permission, accessibility, unit-test, UI-test, privacy, analytics-boundary, and external-service fallback coverage.
