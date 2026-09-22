# Project state — Dockie

- **Product:** Lightweight Android utility — stay awake on wireless charging only; restore exact screen timeout on undock
- **Package:** `com.dockie.app` (immutable)
- **Brand / publisher:** EYC Digital
- **Repo:** https://github.com/elchanany/Dockie
- **Stack:** Kotlin, Jetpack Compose, Material 3, DataStore, foreground `specialUse` service
- **Current version:** see `app/build.gradle.kts` (`versionName` / `versionCode`)
- **minSdk 29 / targetSdk 35 / compileSdk 35**
- **No INTERNET permission; no ads/analytics/Firebase**
- **Signing:** existing keystore outside VCS — see `SIGNING_MIGRATION.md`
- **Play goal:** Closed testing ready (≥12 testers × 14 days)
