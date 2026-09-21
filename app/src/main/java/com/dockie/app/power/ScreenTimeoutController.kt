package com.dockie.app.power

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import android.util.Log

/**
 * Reads and writes Settings.System.SCREEN_OFF_TIMEOUT.
 *
 * Strategy:
 * - Right before applying the override, read the CURRENT value and persist it
 *   as the exact value to restore later. Never hardcode a "default".
 * - While docked, write [DOCKED_TIMEOUT_MS]. This is a very long but valid
 *   timeout (Int.MAX_VALUE ms ≈ 24 days). Samsung / AOSP clamp out-of-range
 *   values, and this value stays within int range so it is accepted and
 *   effectively means "never time out automatically". The user can still turn
 *   the screen off with the power button — we never hold a wake lock.
 * - On undock / disable, restore the saved value verbatim, but only if Dockie
 *   owns the override (see DockieRepository).
 *
 * If the user changes the timeout manually elsewhere while Dockie is active,
 * we intentionally do NOT chase it: we restore the single value captured
 * before our override. This avoids restore loops and keeps one clear owner.
 */
object ScreenTimeoutController {
    private const val TAG = "DockieTimeout"

    /** ~24.8 days in ms; accepted as a valid int timeout, effectively "never". */
    const val DOCKED_TIMEOUT_MS: Long = Int.MAX_VALUE.toLong()

    /** Sanity bounds for values we are willing to restore (1s .. 24.8 days). */
    private const val MIN_SANE_TIMEOUT_MS = 1_000L

    fun canWrite(context: Context): Boolean = Settings.System.canWrite(context)

    fun readCurrentTimeoutMs(resolver: ContentResolver): Long? {
        return try {
            Settings.System.getLong(resolver, Settings.System.SCREEN_OFF_TIMEOUT).also {
                Log.d(TAG, "read timeout=$it")
            }
        } catch (e: Settings.SettingNotFoundException) {
            Log.w(TAG, "timeout setting not found", e)
            null
        }
    }

    fun readCurrentTimeoutMs(context: Context): Long? =
        readCurrentTimeoutMs(context.contentResolver)

    /** Best-effort write; returns false when permission is missing or write fails. */
    fun writeTimeoutMs(context: Context, valueMs: Long): Boolean {
        if (!canWrite(context)) {
            Log.w(TAG, "write denied: no WRITE_SETTINGS permission")
            return false
        }
        return try {
            val clamped = valueMs.coerceIn(MIN_SANE_TIMEOUT_MS, DOCKED_TIMEOUT_MS)
            val ok = Settings.System.putLong(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                clamped,
            )
            Log.d(TAG, "write timeout=$clamped ok=$ok")
            ok
        } catch (e: Exception) {
            Log.w(TAG, "write failed", e)
            false
        }
    }

    fun applyDockedOverride(context: Context): Boolean {
        Log.d(TAG, "applying docked override")
        return writeTimeoutMs(context, DOCKED_TIMEOUT_MS)
    }

    fun restoreTimeout(context: Context, savedMs: Long?): Boolean {
        if (savedMs == null) {
            Log.w(TAG, "restore skipped: no saved value")
            return false
        }
        Log.d(TAG, "restoring timeout=$savedMs")
        return writeTimeoutMs(context, savedMs)
    }
}
