**What:**
Replaced the filled `TextField` in the main search screen (`RockMusicRoot.kt`) with an `OutlinedTextField`.

**Why:**
The application previously used a mix of `OutlinedTextField` (in `RockMusicExperience` and settings) and standard `TextField` (in `RockMusicRoot`). A standard `TextField` with a solid background can look heavy and less refined, especially in modern apps aiming for a clean aesthetic. `OutlinedTextField` provides a neater and more consistent visual style across all screens.

**Impact:**
- **Visuals:** The primary search input now matches the cleaner aesthetic of other search bars in the app.
- **Consistency:** Search fields now uniformly use `OutlinedTextField` (with trailing clear buttons as per existing patterns).

**How to Measure:**
Navigate to the "Search" tab and verify the input field is an outlined text field instead of a filled text field. Verify it functions exactly the same (text input, clear button visibility, layout constraints).
