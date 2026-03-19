# iOS Keychain Migration for Token Storage

**Status:** Recommended remediation (security audit).  
**Current:** Tokens stored in `NSUserDefaults` (plaintext) in `TokenStorage.ios.kt`.  
**Target:** Store access/refresh tokens in iOS Keychain so they are encrypted at rest and only available when the device is unlocked.

## Options

### 1. Multiplatform Settings with Keychain (recommended)

Use [russhwolf/multiplatform-settings](https://github.com/russhwolf/multiplatform-settings) which provides `KeychainSettings` for iOS:

- Add dependency in `composeApp/build.gradle.kts` (e.g. `russhwolf:multiplatform-settings` and `multiplatform-settings-keychain` for iOS).
- In `iosMain`, create a `TokenStorage` implementation that wraps `KeychainSettings(service = "com.tamixa.tokens")` and maps keys `access_token`, `refresh_token`, `token_expiry_ms` to the existing `TokenStorage` interface.
- Remove use of `NSUserDefaults` for these keys in `IosTokenStorage`.

### 2. Swift helper in the host app

If you prefer not to add a KMP dependency:

- In the iOS app target (e.g. `iosApp` or the Xcode project that consumes the shared framework), add a Swift file that implements Keychain read/write using `Security` framework (`SecItemAdd`, `SecItemCopyMatching`, `SecItemDelete`).
- Expose a minimal C/Obj-C API (e.g. `keychain_set(key, value)`, `keychain_get(key)`, `keychain_delete(key)`) and add a Kotlin/Native cinterop `.def` file that links to this implementation.
- From `TokenStorage.ios.kt`, call through the cinterop to read/write tokens.

### 3. KSecureStorage or similar KMP library

Use a dedicated KMP secure-storage library (e.g. [ksecurestorage](https://github.com/AlexanderEggers/ksecurestorage)) that already implements Android Keystore and iOS Keychain. Integrate and replace `IosTokenStorage` with the library’s iOS implementation.

## Keychain configuration

- Use a dedicated service name (e.g. `com.tamixa.tokens`) and account keys (`access_token`, `refresh_token`, `token_expiry_ms`).
- Prefer `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` (or equivalent) so tokens are not synced to iCloud and are only available when the device is unlocked.
- On first run after migration, read from NSUserDefaults once and migrate existing tokens to Keychain, then remove them from NSUserDefaults.

## References

- [MOBILE_APP_AUDIT.md](MOBILE_APP_AUDIT.md) — iOS token storage finding.
- [SECURITY_COMPLIANCE_AUDIT_REPORT.md](../SECURITY_COMPLIANCE_AUDIT_REPORT.md) — High finding: iOS tokens in plaintext.
