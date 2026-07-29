## 2024-05-24 - [Added Clear Button to Search Inputs]
**Learning:** Adding a trailing clear button to text fields, conditional on `query.isNotBlank()`, is a high-value, low-effort micro-UX pattern in Jetpack Compose that significantly improves ease of navigation and search resets. Remember to add `contentDescription` for accessibility.
**Action:** Use `trailingIcon` on `TextField` and `OutlinedTextField` whenever dealing with filter/search queries, paired with an `IconButton` to reset the state.

## 2024-07-28 - [Simulating 3D Glass in Jetpack Compose]
**Learning:** Simulating 3D glassmorphism in Compose without relying on Android 12+ `RenderEffect` (blur) involves layering properties to fake depth and lighting. A translucent base color (`alpha = 0.2f`), a linear gradient border (highlighting top/left), a subtle gradient overlay (sheen/texture), and an increased `shadowElevation` (e.g., `14.dp`) can approximate complex CSS shadows and backdrop-filters.
**Action:** When implementing glass effects, adjust `Surface` color alpha, use `Brush.linearGradient` for `BorderStroke` to create directional highlights, and leverage `Modifier.drawWithCache` for texture/sheens, keeping the code robust across API levels without heavy blurring logic.
## 2024-07-29 - [Search View Aesthetics]
**Learning:** `OutlinedTextField` provides a cleaner, more consistent look across search inputs compared to the default filled `TextField`, especially in minimal or dark themes where filled fields can feel heavy.
**Action:** Default to `OutlinedTextField` for primary search fields across the application to maintain a "neat and clean" standard look.
