# Dockie

Stay awake while docked.

## Download

Download the latest Android APK:

[Download Dockie.apk](https://github.com/elchanany/Dockie/releases/latest/download/Dockie.apk)

Latest releases:

https://github.com/elchanany/Dockie/releases

Dockie is distributed outside Google Play, so Android will ask you to allow
"install unknown apps" for your browser/file manager on first install. That is
expected — Dockie contains no network code at all (it does not even request
the `INTERNET` permission), so there is nothing for it to send anywhere.

## What Dockie does

Dockie keeps your screen on while your phone sits on a **wireless charger**,
and restores your exact previous screen-timeout the moment you take it off.

That's the entire product:

- Put phone on wireless charger → screen stays awake.
- Take phone off → everything automatically returns to normal.

## How it works

1. When you tap the big control, Dockie starts a lightweight foreground
   service that listens for Android's `ACTION_BATTERY_CHANGED` broadcasts.
2. It checks `BatteryManager.EXTRA_PLUGGED` for
   `BATTERY_PLUGGED_WIRELESS` specifically. USB / wired AC charging never
   triggers Dockie.
3. On wireless dock: Dockie reads the current
   `Settings.System.SCREEN_OFF_TIMEOUT`, saves that exact value in DataStore,
   marks itself as owning an override, and writes a very long timeout
   (`Int.MAX_VALUE` ms ≈ 24 days — a valid int that Samsung/AOSP accept and
   that effectively means "never time out automatically").
4. On undock: Dockie immediately restores the saved value verbatim and clears
   ownership. It only ever restores when it owns the override.

No wake locks are held: the user can always turn the screen off with the
physical power button. Dockie prevents *automatic* timeout only — it never
forces the screen on against explicit user intent. Unlocking while still
docked keeps the long timeout active.

## Required permission

**Modify system settings** (`android.permission.WRITE_SETTINGS`).

Why: Android only lets apps change `SCREEN_OFF_TIMEOUT` through this
user-granted settings permission. Dockie requests it once via
`Settings.ACTION_MANAGE_WRITE_SETTINGS`, uses it solely to extend/restore the
timeout around wireless-charging sessions, and fails gracefully (no crash,
clear recovery UI) if it is ever revoked.

## Background-service design

- One foreground service (`DockMonitoringService`, `specialUse` type on
  Android 14+ with the documented subtype *"Continuously monitors wireless
  charging state to temporarily manage screen timeout at the user's
  request."*). No abused service types.
- Fully event-driven: a single battery broadcast receiver. No polling, no
  timers, no wakelocks, no network — negligible battery impact.
- Quiet `IMPORTANCE_LOW` notification while enabled ("Waiting for wireless
  charging" / "Screen staying awake") with Disable and Open actions.
- `RECEIVE_BOOT_COMPLETED`: if Dockie was enabled before a reboot, monitoring
  resumes automatically (with the compliant fallback of reconciling on next
  launch/event where the OS forbids direct FGS start from boot).

## Wireless-charging detection

Primary and only trigger: `BatteryManager.BATTERY_PLUGGED_WIRELESS` from the
sticky `ACTION_BATTERY_CHANGED` broadcast. `isCharging()` alone is never used.

## 100% battery behavior

`STATUS_FULL` is never treated as "undocked". As long as Android keeps
reporting `BATTERY_PLUGGED_WIRELESS` as the plugged source — including at
100% or when Samsung battery protection pauses charging at a threshold —
Dockie stays active.

## State-restoration strategy

Persisted in DataStore: enabled flag, saved original timeout, override
ownership flag, first-run flag, theme, start-after-restart.

- Restore happens only when Dockie owns the override; ownership is cleared
  before writing back, so a crash mid-restore can't double-restore.
- On service/process start with owned override: if wireless power is still
  present, the override continues (defensively re-applied); otherwise the
  saved value is restored immediately. This covers kills, crashes, reboots,
  force-stops, and chargers removed while dead.
- Disabling Dockie while docked restores immediately and stops the service.
- If the user changes the timeout manually elsewhere while Dockie is active,
  Dockie does not fight it — it restores the single value captured before its
  own override, once, on undock.
- Rapid dock/undock cycles are serialized through a mutex: no races, no
  timeout corruption.

## Known Android limitations

- If Dockie is **uninstalled** while its override is active, Android provides
  no reliable uninstall callback, so the long timeout could remain until the
  user changes it in system settings. Mitigated by restoring immediately
  whenever monitoring stops, but it cannot be solved completely — reinstalling
  Dockie and toggling it, or adjusting the timeout in Settings, fixes it.
- On some OEM skins the system may clamp extreme timeout values; Dockie uses
  the largest valid int precisely to stay within accepted range.
- Charger identity is not exposed by Android for ordinary Qi charging, so v1
  works with **all** wireless chargers (no per-dock allow-list). The detection
  layer is isolated (`ChargingStateObserver`) so a future trusted-dock feature
  (NFC/BLE) can plug in later.

## Permissions (all of them)

| Permission | Reason |
|---|---|
| `WRITE_SETTINGS` | Extend/restore screen timeout around dock sessions |
| `RECEIVE_BOOT_COMPLETED` | Resume monitoring after reboot when enabled |
| `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` | Required for the monitoring service |
| `POST_NOTIFICATIONS` | Quiet status notification (Android 13+) |

No location, camera, microphone, contacts, storage, or accessibility access.
No `INTERNET`.

## Build instructions

Requirements: JDK 17+, Android SDK with platform 35.

```sh
./gradlew assembleDebug      # debug APK
./gradlew assembleRelease    # release APK (unsigned unless signing configured)
```

Release signing (never committed): provide a keystore outside the repo via
environment variables or `~/.gradle/gradle.properties`:

```properties
dockie.keystore.path=/secure/path/dockie-release.keystore
dockie.keystore.password=...
dockie.key.alias=dockie
dockie.key.password=...
```

Tagging `v*` triggers `.github/workflows/release.yml`, which builds, signs
(from `DOCKIE_KEYSTORE_BASE64` / `DOCKIE_KEYSTORE_PASSWORD` /
`DOCKIE_KEY_ALIAS` / `DOCKIE_KEY_PASSWORD` secrets), and attaches `Dockie.apk`
to the GitHub Release.

## Project structure

```
app/src/main/java/com/dockie/app/
  MainActivity.kt            single activity (Compose)
  DockieApp.kt               Application (owns repository)
  model/AppState.kt          Disabled / PermissionRequired / Monitoring / Docked / …
  data/DockieRepository.kt   DataStore ownership model
  power/ChargingStateObserver.kt   wireless-only detection
  power/ScreenTimeoutController.kt read/extend/restore SCREEN_OFF_TIMEOUT
  power/PermissionManager.kt WRITE_SETTINGS helpers
  service/DockMonitoringService.kt foreground service + DockController
  notify/NotificationController.kt quiet status notification
  receiver/BootReceiver.kt   resume monitoring after reboot
  ui/MainViewModel.kt        declarative UI state
  ui/theme/                  warm light / OLED dark palettes, system type
  ui/components/             animated master control + status card
  ui/screens/                main / onboarding / permission / settings
```
