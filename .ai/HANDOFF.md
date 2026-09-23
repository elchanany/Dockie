# Handoff — Dockie

## Verified current state (2026-09-22)

- Branch: `main`
- Version: **1.3.1** (`versionCode` **5**) after lint-safe splash / Crossfade / WRITE_SETTINGS tools:ignore fixes
- Package: `com.dockie.app`
- Production signing SHA-256: `CD:1B:E0:58:57:CA:97:AC:94:CE:A9:4F:A9:D7:EC:D7:25:C5:B2:A9:4D:41:7C:2E:A1:84:6A:24:71:D4:3E:F9` (APK/AAB/keystore match)
- Play publisher SA: `dockie-play-publisher@dockie-play-publisher.iam.gserviceaccount.com` (GCP project `dockie-play-publisher`, Android Publisher API enabled). JSON at `%USERPROFILE%\.dockie-signing\play-service-account.json` and gitignored repo copy.
- Docs ready: `SIGNING_MIGRATION.md`, `PLAY_CONSOLE_SUBMISSION.md`, `CLOSED_TEST_PLAN.md`, `TESTERS.md`, `PLAY_FGS_VIDEO_SCRIPT.md`, `GEMINI_IMAGE_REQUESTS.md`
- Privacy policy page added on EYC landing: `/privacy/dockie/`
- Play icon: `play-store-assets/icon-512.png`
- Feature graphic: needs Gemini (see `GEMINI_IMAGE_REQUESTS.md`)
- Screenshots: emulator available (`Pixel_9_Pro_XL`) but not captured in this handoff yet if time-boxed

## Exact next steps

1. Play Console → App signing → download `encryption_public_key.pem` → run PEPK → upload ZIP → confirm SHA-256 match (`SIGNING_MIGRATION.md`)
2. Play Console → Users and permissions → invite `dockie-play-publisher@dockie-play-publisher.iam.gserviceaccount.com` with release/admin rights for Dockie
3. Upload `app/build/outputs/bundle/release/app-release.aab` to **Closed testing** (first upload may be manual)
4. Paste App content answers from `PLAY_CONSOLE_SUBMISSION.md`
5. Provide 12 tester Google emails; create opt-in link; start 14-day clock
6. After SA invited: `./gradlew publishReleaseBundle`

## PAUSED 2026-09-23 - waiting on Gemini visuals (do NOT roll out)
- Visual work delegated to Gemini: app/launcher/mono/Play-512 icons, feature graphic, screenshots. Do not duplicate.
- Code 8 AAB (target 36) staged as alpha DRAFT via API; internal has 1.4.0/code 7 live. Next unused versionCode is 9+.
- On group email: wire to closed track via edits.testers API only. On visual approval: sync, inspect, bump code, rebuild AAB, replace closed draft.
