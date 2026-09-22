# Play Console submission answers (Dockie)

Verified against source on branch `main` for package `com.dockie.app` (versionName from `app/build.gradle.kts`).

Contact for store forms: **EYCEYCEYC139@gmail.com**  
Developer name: **EYC Digital**  
Website: **https://eyc-landing-page.vercel.app**  
Privacy policy URL: **https://eyc-landing-page.vercel.app/privacy/dockie**

---

## App access

- **All functionality available without special access:** Yes
- **Login required:** No
- **Instructions / credentials:** Not applicable — no accounts

## Ads

- **Contains ads:** No

## Content rating (IARC questionnaire — expected answers)

Answer honestly in the console questionnaire. Based on the current app:

| Topic | Answer |
|---|---|
| Violence | No |
| Sexual content | No |
| Profanity | No |
| Controlled substances | No |
| Gambling | No |
| User-generated content | No |
| Online interaction / social | No |
| Shares location | No |
| Purchases | No |

Expected rating: **Everyone** / PEGI 3 equivalent.

## Target audience

- **Target age groups:** 18 and over (utility; not designed for children)
- **Appeals to children:** No
- **Store presence / Designed for Families:** No

## News app

- No

## COVID-19 contact tracing / status apps

- No

## Data safety

### Overview (verified from code)

| Question | Answer | Evidence |
|---|---|---|
| Does the app collect or share user data? | **No** | No INTERNET permission; no analytics/ads/Firebase SDKs in `gradle/libs.versions.toml` / `app/build.gradle.kts` |
| Is all user data encrypted in transit? | N/A (no collection / no network) | Manifest has no INTERNET |
| Do you provide a way for users to request data deletion? | N/A (no account data collected) | Local DataStore only; uninstall clears prefs |

### Data types

Declare **no data collected** and **no data shared** for:

- Location, personal info, financial, health, messages, photos, files, audio, calendar, contacts, app activity (beyond on-device prefs), web browsing, app info/performance telemetry, device IDs

### Sensitive permissions used on-device only (not “collected” for Data safety)

- `WRITE_SETTINGS` — temporary screen-timeout change; value stored locally in DataStore while Dockie owns the override
- `POST_NOTIFICATIONS` — local status / one-time alert
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_SPECIAL_USE` — local monitoring
- `RECEIVE_BOOT_COMPLETED` — resume local monitoring

Do **not** claim these as “collected and sent off device”.

## Government apps

- **Is this a government app?** No

## Financial features

- None (no banking, payments, crypto wallets, lending, etc.)

## Health

- No health / medical features

## Privacy policy

- URL: https://eyc-landing-page.vercel.app/privacy/dockie
- Must be publicly reachable before production; preferred before closed testing as well

## Permissions declarations

### WRITE_SETTINGS / Modify system settings

**Why needed (paste):**  
Dockie temporarily changes `Settings.System.SCREEN_OFF_TIMEOUT` only while the phone is on wireless charging, then restores the user’s exact previous value. Android requires the user-granted `WRITE_SETTINGS` permission for this. Dockie requests it via `Settings.ACTION_MANAGE_WRITE_SETTINGS`, never silently. Wired USB/AC charging does not trigger the override.

### Notifications (`POST_NOTIFICATIONS`)

**Why needed:**  
Android 13+ requires this for the quiet ongoing monitoring notification (Play policy for foreground services) and an optional one-time heads-up when Dockie activates. If denied, monitoring can still run where the OS allows; the alert is skipped.

## Foreground service declaration

| Field | Paste |
|---|---|
| Service type | **Special use** (`specialUse`) |
| Manifest subtype | Continuously monitors wireless charging state to temporarily manage screen timeout at the user's request. |
| Why FGS is required | Wireless charging can start/stop while Dockie is in the background. The foreground service listens to `ACTION_BATTERY_CHANGED` (event-driven, no polling) so Dockie can apply/restore screen timeout at the exact dock/undock moment. Deferred/job-only work cannot reliably catch undock while the process is idle. |
| Why not deferrable | Undock must restore the user’s exact previous timeout immediately; delaying restoration leaves an unintended long timeout. |
| User starts it | User opens Dockie, completes onboarding/permission if needed, taps the main control to enable monitoring. |
| User stops it | Tap Disable in the notification, or turn Dockie off in the app. Disabling while docked restores timeout immediately and stops the service. |
| How user knows it is running | Quiet ongoing notification (“Waiting for wireless charging” / “Screen staying awake”); optional heads-up “Dockie is active”. Status-bar icon only while docked (if enabled in Settings). |

Video script: see `PLAY_FGS_VIDEO_SCRIPT.md`.

## Other declarations

- **App category suggestion:** Tools / Productivity
- **Tags (if offered):** wireless charging, screen timeout, dock, stay awake
- **Free:** Yes
- **Contains in-app purchases:** No (currently)
- **Ads:** No

## Store listing text (en-US) — ready to paste

**App name:** Dockie

**Short description (≤80 chars):**  
Stay awake on wireless charge. Exact timeout restored when you lift.

**Full description:**

```
Dockie keeps your screen awake while your phone sits on a wireless charger — then restores your exact previous screen timeout the moment you take it off.

How it works
• Enable Dockie and grant the one-time “modify system settings” permission
• Place your phone on any wireless charger — the screen stays awake
• Lift the phone — your previous timeout returns automatically
• USB / wired charging never activates Dockie
• The power button still turns the screen off whenever you want

Stay-awake options
• Until you remove the phone (default)
• Presets from a few minutes to several hours
• Custom duration on a simple hours/minutes wheel

Designed to stay out of the way
• Lightweight foreground monitor (event-driven, no polling)
• Quiet status notification while enabled
• Optional one-time alert when Dockie activates
• Works after reboot if you leave “Start after restart” on
• Offline by design — no accounts, ads, analytics, or internet permission

Made by EYC Digital.
```

**Release notes (closed test / 1.3.1):**  
See `app/src/main/play/release-notes/en-US/alpha.txt`.

**Support email:** EYCEYCEYC139@gmail.com  
**Website:** https://eyc-landing-page.vercel.app  
**Privacy policy:** https://eyc-landing-page.vercel.app/privacy/dockie
