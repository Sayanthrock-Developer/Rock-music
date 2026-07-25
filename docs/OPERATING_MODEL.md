# Rock Music operating model

Rock Music uses one source-aware action pipeline for playback, downloads, sharing, recognition and social listening. Screens do not call provider SDKs, download managers or playback services directly.

## The new flow

1. **Describe the media source**
   - local file
   - podcast RSS enclosure
   - user-authorised cloud file
   - licensed catalogue item
   - official provider link
   - public-domain item
   - Creative Commons item
   - unknown source

2. **Describe the requested operation**
   - play
   - download
   - share
   - open official provider
   - recognise
   - create or join a listening room

3. **Resolve access and capability context**
   - provider configuration
   - authentication
   - connectivity
   - local cache availability
   - regional availability
   - playback entitlement
   - provider capability
   - official provider destination
   - item-level download permission
   - microphone consent
   - participant legal-access confirmation

4. **Ask `MediaActionPolicyEngine` for a decision**
   - execute in Rock Music
   - open the official provider
   - request configuration
   - request authentication
   - report offline
   - block with a clear reason

5. **Execute only the returned plan**

A screen, ViewModel, worker, Media3 service or provider adapter must not perform an action that was not approved by the policy engine.

## Two playback paths

### In-app playback

Rock Music may play media directly when the source is local, user-owned, openly licensed, a permitted podcast enclosure, or a licensed catalogue explicitly grants in-app playback.

### Official-provider handoff

When a provider controls playback, DRM, advertisements, subscription access or offline storage, Rock Music opens the official provider destination. It does not extract or rebuild the protected stream.

## Download behaviour

Downloads are denied by default. `DownloadPermission.allowed` and the provider capability must both be true.

Examples:

- Local file: already offline; no download action is offered.
- Podcast enclosure: allowed only when the publisher/source permits it.
- User cloud file: allowed only when the user is authorised and the provider allows export/offline use.
- Licensed catalogue: allowed only through the provider’s licensed download capability.
- Official provider link: hand off to the official app when it controls offline storage.
- Unknown source: blocked.

Permission is re-evaluated before start, resume and retry.

## Offline behaviour

A remote item can play offline only when `locallyAvailable` is true. Source type alone never implies that bytes are cached.

## Echo Find

Recognition requires all of the following:

- licensed provider configuration;
- network access;
- explicit microphone consent;
- recognition capability from the provider.

Without these conditions, recording does not begin.

## Listen Together

Room creation and joining require:

- configured REST/WebSocket service;
- supported room capability;
- authenticated participants where required;
- legal source access confirmed for every participant.

The backend acknowledges playback changes, chat, reactions, voting and host transfer. The client does not fabricate success.

## UI rules

The UI must render the policy decision directly:

- `ExecuteInApp`: continue with the action.
- `OpenOfficialProvider`: show the provider handoff and open the official URI.
- `RequireConfiguration`: show which settings are missing.
- `RequireAuthentication`: offer a provider sign-in action.
- `Offline`: show offline state and cached alternatives.
- `Blocked`: show the exact reason and do not retry unless the underlying state changes.

## Migration rule

Every existing and new feature must move to this pipeline. Direct calls from Compose screens to provider APIs, download URLs, recognition services or room sockets are not permitted.
