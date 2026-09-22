# Decisions — Dockie

## D1 — Wireless-only trigger
Use `BatteryManager.BATTERY_PLUGGED_WIRELESS` only. Never activate on USB/AC.

## D2 — Timeout ownership model
Save exact prior `SCREEN_OFF_TIMEOUT`, set override only while owning, restore only when owning, clear ownership before restore write-back.

## D3 — Foreground service type
`specialUse` with documented subtype for wireless-charge monitoring. No misuse of `connectedDevice` / `dataSync` / etc.

## D4 — Signing continuity
Existing GitHub APK signing key remains the production identity for Play App Signing enrollment so sideloaded users can update.

## D5 — Privacy
No INTERNET permission; local-first; no analytics/ads until an explicit future product decision.
