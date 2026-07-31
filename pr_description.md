🎯 What: Added tests for `EncryptedSpotifyPkceRequestStore` to address the untested timing edge case in `consume` and ensure proper behavior when the request is expired, valid, or has a mismatched state.
📊 Coverage: Added coverage for `consume` (valid request, expired request, incorrect state) and `clearExpired` (valid request, expired request).
✨ Result: Improved reliability of the Spotify PKCE request handling by explicitly validating the `System.currentTimeMillis()` based expiration logic.
