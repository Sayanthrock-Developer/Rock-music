🔒 [security fix description]

🎯 **What:** The `AppearancePreferences` class was using standard `SharedPreferences` (with `MODE_PRIVATE`) to store appearance settings such as `theme_mode`, `system_color`, and `blur_frames`. This has been updated to use `EncryptedSharedPreferences`.

⚠️ **Risk:** While the current stored preferences (theme, color, blur) are low-risk hygiene settings, using unencrypted storage mechanisms for preferences sets a weak security posture. Unencrypted shared preferences could expose sensitive information to other apps with elevated privileges (like on rooted devices) or physical access. Adopting `EncryptedSharedPreferences` ensures future configurations added to this store remain secure by default.

🛡️ **Solution:** Replaced `Context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)` with `EncryptedSharedPreferences.create(...)`, utilizing `MasterKey` with `AES256_GCM` scheme, and specifying `AES256_SIV` for keys and `AES256_GCM` for values.
