# Closed test plan — Dockie

**Track:** Closed testing (Play Console “Closed testing”, Gradle Play Publisher track `alpha`)  
**Package:** `com.dockie.app`  
**Release name suggestion:** `Closed test 1.3.1`  
**Release notes:** `app/src/main/play/release-notes/en-US/alpha.txt`

## Preconditions

1. App content / Data safety / privacy policy URL completed (see `PLAY_CONSOLE_SUBMISSION.md`)
2. Existing signing key enrolled (see `SIGNING_MIGRATION.md`)
3. AAB uploaded to closed track
4. ≥12 tester Google accounts opted in (see `TESTERS.md`)
5. Testers remain opted in for **14 continuous days** before production access request

## Test matrix

| # | Case | Steps | Expected |
|---|---|---|---|
| 1 | First install from Play | Install from closed-test opt-in link | App installs; package `com.dockie.app` |
| 2 | Onboarding | Fresh install, open app | Two intro screens then permission setup; first-run only |
| 3 | WRITE_SETTINGS | Grant via system settings screen | App returns to Ready; can enable |
| 4 | Notifications | Grant / deny on Android 13+ | Monitoring works; alert skipped if denied |
| 5 | Enable Dockie | Tap main control | Quiet notification “Waiting for wireless charging” |
| 6 | Wireless dock | Place on Qi/wireless charger | State Docked; timeout extended; optional heads-up once |
| 7 | Stay awake | Leave idle on dock | Screen does not auto-timeout |
| 8 | Undock restore | Lift phone | Exact previous timeout restored; notification returns to waiting; icon gone |
| 9 | USB does not activate | Connect USB/wired AC only | Remains Waiting / not Docked |
| 10 | Power button | Press power while docked | Screen turns off; no forced wake |
| 11 | Unlock while docked | Unlock after power-off (default “Stay active”) | Long timeout continues |
| 12 | Battery 100% | Charge to full on wireless | Stays Docked while still `BATTERY_PLUGGED_WIRELESS` |
| 13 | Samsung protect | Device pauses charge at 80–85% if applicable | Stays Docked if still wireless plugged |
| 14 | Custom duration | Settings → Custom wheel | Hours 0–12, minutes 0–59; applies next dock |
| 15 | Timeout expiry | Short custom duration, stay docked | Restores timeout; shows Time’s up until re-dock |
| 16 | Heads-up once | Dock with alert enabled | One “Dockie is active” / “Screen will stay awake”; no spam |
| 17 | Notification cleanup | Undock / Disable / expiry | Alert cancelled; no stale Dockie alerts |
| 18 | Process death | Force-stop while docked, reopen | Reconciles: still wireless → keep; not wireless → restore |
| 19 | Reboot | Reboot with Start after restart on | Monitoring resumes when OS allows |
| 20 | Disable while docked | Disable from app or notification | Restore immediately; service stops |
| 21 | Themes | Light / Dark / System | UI readable; no crash |
| 22 | Accessibility | Large font / TalkBack smoke | Controls reachable; labels present |

## Pass criteria for closed-test start

- Cases 1–11, 16–17, 20 pass on ≥2 devices (ideally one Pixel, one Samsung)
- No crash on enable/disable/dock/undock
- No activation on USB-only charging

## Feedback format for testers

Device model + Android version + steps + expected vs actual + screenshot if UI.
