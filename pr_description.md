**What:**
The application was using standard cleartext `SharedPreferences` to store tokens. Although the tokens themselves were encrypted using AES/GCM before being stored, the underlying file could theoretically be accessed or modified maliciously, and any keys or additional future data stored in the same preference file would be in cleartext.

**Risk:**
If an attacker gained read/write access to the application's data directory (e.g., via a rooted device or another exploit), they could access the raw encrypted payload or modify it. While AES/GCM provides encryption and authentication for the payload, standard SharedPreferences offers no defense-in-depth. Future additions to the SharedPreferences file might be inadvertently stored in cleartext if developers forget to encrypt them.

**Solution:**
Migrated from standard `SharedPreferences` to AndroidX's `EncryptedSharedPreferences`.
- Added the `androidx.security:security-crypto` dependency.
- Updated `TokenVault` to initialize `EncryptedSharedPreferences` using a `MasterKey` configured with `AES256_GCM`.
- The keys are now encrypted using `AES256_SIV` and values using `AES256_GCM` automatically, providing a robust defense-in-depth layer on top of the existing manual token encryption/decryption logic.
