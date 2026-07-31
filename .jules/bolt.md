## 2026-07-31 - Safe Launching of Intents Outside Activities
**Learning:** When starting an Activity from an Application context or non-Activity component (such as from a global object like `EqualizerLauncher`), Android requires `Intent.FLAG_ACTIVITY_NEW_TASK` to be appended to the intent.
**Action:** Always add `.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)` to intent configurations launched globally out of context.

## 2026-07-31 - Cross-Process Media3 audioSessionId Extraction
**Learning:** Retrieving `audioSessionId` directly through Media3's `MediaController` is restricted and often falls back to `0`. However, equalizers gracefully fallback to standard global mix applying properly when value is `0` or appropriately extracted locally via the direct `ExoPlayer` instance before publishing StateFlow.
**Action:** In Media3 implementations, safely extract session IDs directly at the Player instance logic or gracefully fallback to `0`.
