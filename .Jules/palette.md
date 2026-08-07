## 2024-05-24 - [Added Clear Button to Search Inputs]
**Learning:** Adding a trailing clear button to text fields, conditional on `query.isNotBlank()`, is a high-value, low-effort micro-UX pattern in Jetpack Compose that significantly improves ease of navigation and search resets. Remember to add `contentDescription` for accessibility.
**Action:** Use `trailingIcon` on `TextField` and `OutlinedTextField` whenever dealing with filter/search queries, paired with an `IconButton` to reset the state.

## 2024-07-28 - [Simulating 3D Glass in Jetpack Compose]
**Learning:** Simulating 3D glassmorphism in Compose without relying on Android 12+ `RenderEffect` (blur) involves layering properties to fake depth and lighting. A translucent base color (`alpha = 0.2f`), a linear gradient border (highlighting top/left), a subtle gradient overlay (sheen/texture), and an increased `shadowElevation` (e.g., `14.dp`) can approximate complex CSS shadows and backdrop-filters.
**Action:** When implementing glass effects, adjust `Surface` color alpha, use `Brush.linearGradient` for `BorderStroke` to create directional highlights, and leverage `Modifier.drawWithCache` for texture/sheens, keeping the code robust across API levels without heavy blurring logic.
## 2024-07-29 - [Search View Aesthetics]
**Learning:** `OutlinedTextField` provides a cleaner, more consistent look across search inputs compared to the default filled `TextField`, especially in minimal or dark themes where filled fields can feel heavy.
**Action:** Default to `OutlinedTextField` for primary search fields across the application to maintain a "neat and clean" standard look.

## 2024-08-01 - [Add Alt Text to Artwork Images]
**Learning:** `AsyncImage` components used for displaying track/album artwork often have `contentDescription = null` by default in lists. Adding dynamic alt text like `contentDescription = "Artwork for ${track.name}"` makes list traversal with screen readers significantly more informative.
**Action:** When adding images that provide visual context to list items (like album art), always provide a dynamic `contentDescription` based on the item's title rather than `null`.
## 2026-08-07 - Refactored Blur Implementation
**Learning:** In Compose, applying `Modifier.blur` with `BlurredEdgeTreatment.Unbounded` to a Surface modifier efficiently creates a glassmorphism blur effect on underlying content, whereas standard surface alpha properties alone do not blur the background.
**Action:** When asked to improve blur layers/glass aesthetics in Compose UIs, reach for `Modifier.blur` alongside transparency rather than just tweaking colour alpha channels.
