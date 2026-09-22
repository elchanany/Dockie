# Foreground service demo video — Dockie

Google Play may require a short video demonstrating the `specialUse` foreground service.

## Target

- Duration: **45–90 seconds**
- Format: MP4, landscape or portrait, clear UI + notification shade
- Language: English narration or burnt-in captions

## Recording steps (device or emulator)

1. Start with Dockie installed from Play (or signed release), fresh or after clearing app data.
2. Screen-record the whole flow.
3. Open Dockie → finish 2-screen onboarding if shown.
4. Grant WRITE_SETTINGS; grant notifications if prompted.
5. Tap enable → pull notification shade → show quiet “Waiting for wireless charging”.
6. Place on wireless charger (or emulator wireless plug if available) → show Docked + optional heads-up “Dockie is active / Screen will stay awake”.
7. Leave idle briefly to show screen stays on.
8. Lift / unplug wireless → show restore to waiting; alert gone.
9. Plug USB only → show Dockie does **not** go Docked.
10. Disable from notification → service/notification clears.

## What the reviewer must see

- User explicitly enables monitoring
- Persistent notification while running
- Activation only on wireless charging
- Immediate restore behavior on undock
- Clear disable path

## Caption / narration (ready to read)

“Dockie is a local utility. The user enables monitoring. A quiet foreground notification stays visible while Dockie watches for wireless charging. On wireless dock, Dockie temporarily extends screen timeout and can show a one-time alert. On undock, the exact previous timeout is restored. USB charging does not activate Dockie. The user can disable anytime from the notification or the app.”

## Manual remaining

Capture and upload the MP4 in Play Console under the foreground service / special-use declaration when the form requests a video.
