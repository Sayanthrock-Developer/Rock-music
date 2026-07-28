**What:**
Fixed the test failures regarding `IntegrationAuthorizationStoreTest` due to `java.security.NoSuchAlgorithmException`.
The failures occurred on the `testDebugUnitTest` task, and they were caused because `FakeTokenVault` was instantiating `TokenVault` which directly tried to create `MasterKey`, triggering `java.security.NoSuchAlgorithmException` within the test environment (`RobolectricTestRunner`).

**Why:**
We removed the hard dependency on Android context at instantiation to separate construction from initialization logic, enabling tests (using `FakeTokenVault`) to bypass encryption setup while preserving actual runtime behavior by providing a separate `@Inject` constructor for actual initialization. Also fixed the CI failure by removing the duplicate `robolectric` definition in the `libs.versions.toml`.

**How to verify:**
Run `./gradlew testDebugUnitTest` and observe the tests pass.
