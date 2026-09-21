package com.dockie.app.power

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** What the phone is currently drawing power from, as reported by Android. */
enum class PowerSource {
    NONE,
    WIRELESS,
    WIRED_AC,
    WIRED_USB,
    UNKNOWN,
}

data class PowerSnapshot(
    val source: PowerSource,
    val batteryPercent: Int?,
    val isWireless: Boolean,
)

/**
 * Wireless-charging detection.
 *
 * The ONLY trigger is [BatteryManager.BATTERY_PLUGGED_WIRELESS], read from the
 * sticky ACTION_BATTERY_CHANGED broadcast. We deliberately ignore
 * [BatteryManager.isCharging] / STATUS_FULL on their own: at 100%, or when
 * Samsung battery protection pauses charging, Android may report "not
 * charging" while still reporting wireless as the plugged source — and that
 * still counts as docked.
 */
object ChargingStateObserver {

    fun readSnapshot(context: Context): PowerSnapshot {
        val intent: Intent? = context.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
        )
        if (intent == null) return PowerSnapshot(PowerSource.UNKNOWN, null, false)

        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else null

        val source = when {
            plugged and BatteryManager.BATTERY_PLUGGED_WIRELESS != 0 -> PowerSource.WIRELESS
            plugged and BatteryManager.BATTERY_PLUGGED_AC != 0 -> PowerSource.WIRED_AC
            plugged and BatteryManager.BATTERY_PLUGGED_USB != 0 -> PowerSource.WIRED_USB
            plugged == 0 -> PowerSource.NONE
            else -> PowerSource.UNKNOWN
        }
        return PowerSnapshot(source, percent, source == PowerSource.WIRELESS)
    }

    fun label(source: PowerSource): String = when (source) {
        PowerSource.WIRELESS -> "Wireless charging"
        PowerSource.WIRED_AC -> "Wired charging"
        PowerSource.WIRED_USB -> "USB charging"
        PowerSource.NONE -> "On battery"
        PowerSource.UNKNOWN -> "Unknown power source"
    }

    suspend fun readSnapshotIO(context: Context): PowerSnapshot =
        withContext(Dispatchers.Default) { readSnapshot(context) }
}
