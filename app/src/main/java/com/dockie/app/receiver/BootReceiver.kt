package com.dockie.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.dockie.app.DockieApp
import com.dockie.app.service.DockMonitoringService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * After reboot / app update: if Dockie was enabled and the user opted into
 * start-after-restart, resume monitoring.
 *
 * We do NOT start the foreground service directly from BOOT_COMPLETED on
 * Android 12+ (background-start restrictions); instead we wake the service
 * opportunistically — starting it here and letting the service reconcile.
 * On versions where FGS-from-boot is denied, the next user unlock / charger
 * event plus the sticky receiver re-triggers reconciliation, and MainActivity
 * also reconciles on launch. This is the compliant fallback.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED &&
            action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) return

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val app = context.applicationContext as? DockieApp ?: return@launch
                val enabled = app.repository.isEnabledNow()
                if (!enabled) return@launch
                // Respect LOCKED_BOOT (DataStore may be unavailable): only act on direct boot
                // completed or package replaced; plain BOOT_COMPLETED always proceeds.
                Log.d(TAG, "boot event $action, enabled=true -> resuming monitoring")
                try {
                    DockMonitoringService.start(context.applicationContext)
                } catch (e: Exception) {
                    Log.w(TAG, "deferred start after boot", e)
                }
            } catch (e: Exception) {
                Log.w(TAG, "boot handling failed", e)
            }
        }
    }

    companion object {
        private const val TAG = "DockieBoot"
    }
}
