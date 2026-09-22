# Dockie signing migration (Play App Signing)

**Goal:** Keep Google Play updates installable on top of the existing GitHub-distributed APK without forcing users to uninstall.

**Do not generate a new production signing key.**

## Existing production identity (verified)

| Item | Value |
|---|---|
| Keystore (local, outside VCS) | `%USERPROFILE%\.dockie-signing\dockie-release.keystore` |
| Alias | `dockie` |
| Certificate DN | `CN=Dockie, OU=Dockie, O=Dockie` |
| SHA-1 | `05:5A:CF:3F:B6:C7:72:96:AF:6D:F0:F1:06:13:5F:8D:4E:E3:9A:6B` |
| SHA-256 | `CD:1B:E0:58:57:CA:97:AC:94:CE:A9:4F:A9:D7:EC:D7:25:C5:B2:A9:4D:41:7C:2E:A1:84:6A:24:71:D4:3E:F9` |
| Valid | 2026-09-21 → 2051-09-15 |
| Algorithm | SHA384withRSA, 4096-bit |

### Artifact certificate match (verified)

| Artifact | Certificate SHA-256 |
|---|---|
| `Dockie.apk` (repo / GitHub release) | Matches keystore above |
| Previous `app-release.aab` | Matches keystore above |
| Keystore alias `dockie` | Matches both |

APK and AAB use the **same** certificate. Safe for Play upload with this identity.

### Prepared local files (never commit)

| File | Purpose |
|---|---|
| `%USERPROFILE%\.dockie-signing\dockie-release.keystore` | Production keystore |
| `%USERPROFILE%\.dockie-signing\dockie-upload-certificate.pem` | Exported X.509 cert (upload / verification) |
| `%USERPROFILE%\.dockie-signing\pepk.jar` | Official Google PEPK tool (downloaded) |
| `%USERPROFILE%\.dockie-signing\play-service-account.json` | Play Publisher API credentials |

Gradle reads keystore via `~/.gradle/gradle.properties` (`dockie.keystore.*`) or env vars `DOCKIE_KEYSTORE_*`.

## Play certificate status

No Play release has been published yet for `com.dockie.app`, so Play does **not** yet hold a different app-signing certificate.

**They must be enrolled to match** — choose *use / upload existing app signing key*, not “Let Google generate a new key”.

If Google generates a new app signing key instead, existing GitHub APK installs will **not** update from Play.

## Exact safe enrollment procedure

1. Open [Play Console](https://play.google.com/console) → app **Dockie** (`com.dockie.app`).
2. Go to **Test and release → App integrity → App signing** (wording may be *Setup → App signing*).
3. Accept Play App Signing terms if prompted.
4. Choose the option to **use / upload an existing app signing key** from a Java Keystore (not “Google generates a key”).
5. Download Google’s current `encryption_public_key.pem` from that page into:
   `%USERPROFILE%\.dockie-signing\encryption_public_key.pem`
6. On this machine, run (passwords prompted; do not paste into git):

```bat
cd %USERPROFILE%\.dockie-signing
"C:\Program Files\Android\Android Studio\jbr\bin\java.exe" -jar pepk.jar ^
  --keystore=dockie-release.keystore ^
  --alias=dockie ^
  --output=encrypted_dockie_signing_key.zip ^
  --include-cert ^
  --rsa-aes-encryption ^
  --encryption-key-path=encryption_public_key.pem
```

7. Upload `encrypted_dockie_signing_key.zip` in Play Console.
8. Confirm the Play Console app-signing certificate SHA-256 equals:
   `CD:1B:E0:58:57:CA:97:AC:94:CE:A9:4F:A9:D7:EC:D7:25:C5:B2:A9:4D:41:7C:2E:A1:84:6A:24:71:D4:3E:F9`
9. Optional later: register a separate upload key. For the first closed test it is safe to keep using this same key as upload key so tooling stays simple.

## First AAB to upload

After enrollment (or if Console allows first upload then enroll — follow on-screen order):

- Path: `app\build\outputs\bundle\release\app-release.aab` (rebuild after version bumps)
- Package: `com.dockie.app`
- Upload location: **Test and release → Testing → Closed testing → Create new release → Upload**

## ONE remaining manual Play action

1. In Play Console App signing, download `encryption_public_key.pem`, run the PEPK command above, upload the generated ZIP, and confirm the SHA-256 matches.

After that succeeds, say so in chat — automated `./gradlew publishReleaseBundle` can then be tested against the closed (`alpha`) track.
