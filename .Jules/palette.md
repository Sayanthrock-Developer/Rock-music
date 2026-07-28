## 2024-05-24 - [Added Clear Button to Search Inputs]
**Learning:** Adding a trailing clear button to text fields, conditional on `query.isNotBlank()`, is a high-value, low-effort micro-UX pattern in Jetpack Compose that significantly improves ease of navigation and search resets. Remember to add `contentDescription` for accessibility.
**Action:** Use `trailingIcon` on `TextField` and `OutlinedTextField` whenever dealing with filter/search queries, paired with an `IconButton` to reset the state.
