package com.dockie.app.service

import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.dockie.app.DockieApp
import com.dockie.app.data.DockieRepository
import com.dockie.app.notify.NotificationController
import com.dockie.app.power.ChargingStateObserver
import com.dockie.app.power.ScreenTimeoutController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Lightweight foreground service that owns the docked/undocked transitions.
 *
 * Event-driven: a single ACTION_BATTERY_CHANGED receiver evaluates the
 * plugged source (wireless vs anything else). No polling, no wake locks,
 * no network. Near-zero idle cost.
 *
 * Startup reconciliation (process restart / reboot / crash):
 * - If persisted state says an override is owned, check the live power source.
 * - Wireless still present -> keep the override (re-apply defensively).
 * - Otherwise -> restore the saved timeout immediately and clear ownership.
 */
class DockMonitoringService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private lateinit var repo: DockieRepository

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return
            scope.launch { handlePowerIntent() }
        }
    }

    override fun onCreate() {
        super.onCreate()
        repo = (application as DockieApp).repository
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        Log.d(TAG, "service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_DISABLE -> {
                scope.launch {
                    DockController.disableEverywhere(applicationContext)
                    stopSelf()
                }
                return START_NOT_STICKY
            }
        }
        scope.launch {
            startForegroundQuietly(docked = false)
            reconcileOnStartup()
            handlePowerIntent()
        }
        return START_STICKY
    }

    private fun startForegroundQuietly(docked: Boolean) {
        val notification = NotificationController.build(this, docked)
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                ServiceCompat.startForeground(
                    this,
                    NotificationController.NOTIFICATION_ID,
                    notification,
                    if (Build.VERSION.SDK_INT >= 34) {
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    } else {
                        0
                    },
                )
            } else {
                startForeground(NotificationController.NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            Log.w(TAG, "startForeground failed", e)
        }
        _isRunning.value = true
    }

    private fun refreshNotification(docked: Boolean) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as? android.app.NotificationManager
            ?: return
        try {
            manager.notify(
                NotificationController.NOTIFICATION_ID,
                NotificationController.build(this, docked),
            )
        } catch (e: Exception) {
            Log.w(TAG, "notify failed", e)
        }
    }

    /** Reconcile persisted ownership against the live power source. */
    private suspend fun reconcileOnStartup() {
        mutex.withLock {
            val ownsOverride = repo.isOverrideActiveNow()
            if (!ownsOverride) return
            val snapshot = ChargingStateObserver.readSnapshot(applicationContext)
            Log.d(TAG, "reconcile: ownsOverride=true wireless=${snapshot.isWireless}")
            if (snapshot.isWireless) {
                // Still docked (e.g. process restarted at 100%): keep override.
                ensureOverrideLocked()
            } else {
                // Charger left while we were dead: restore and clear.
                restoreLocked()
            }
        }
        _isRunning.value = true
    }

    private suspend fun handlePowerIntent() {
        mutex.withLock {
            val enabled = repo.isEnabledNow()
            if (!enabled) {
                // Safety: if somehow running while disabled, restore + stop.
                if (repo.isOverrideActiveNow()) restoreLocked()
                stopSelf()
                return
            }
            val snapshot = ChargingStateObserver.readSnapshot(applicationContext)
            _wireless.value = snapshot.isWireless
            _batteryPercent.value = snapshot.batteryPercent
            Log.d(TAG, "power: source=${snapshot.source} pct=${snapshot.batteryPercent}")

            if (snapshot.isWireless) {
                ensureOverrideLocked()
            } else {
                restoreLocked()
            }
        }
    }

    /** Must hold [mutex]. Captures the original timeout once, then applies override. */
    private suspend fun ensureOverrideLocked() {
        if (!ScreenTimeoutController.canWrite(this)) {
            Log.w(TAG, "no WRITE_SETTINGS; cannot apply override")
            refreshNotification(docked = false)
            _docked.value = false
            return
        }
        val alreadyOwned = repo.isOverrideActiveNow()
        if (!alreadyOwned) {
            val current = ScreenTimeoutController.readCurrentTimeoutMs(this)
            // Guard: never persist our own sentinel as the "original".
            val sane = when {
                current == null -> DEFAULT_FALLBACK_TIMEOUT_MS
                current == ScreenTimeoutController.DOCKED_TIMEOUT_MS -> DEFAULT_FALLBACK_TIMEOUT_MS
                else -> current
            }
            Log.d(TAG, "saving original timeout=$sane")
            repo.claimOverride(sane)
        }
        val ok = ScreenTimeoutController.applyDockedOverride(this)
        _docked.value = ok
        refreshNotification(docked = ok)
        val saved = repo.getSavedTimeoutNow()
        Log.d(TAG, "override acquired saved=$saved ok=$ok")
    }

    /** Must hold [mutex]. Restores only when Dockie owns the override. */
    private suspend fun restoreLocked() {
        val owns = repo.isOverrideActiveNow()
        if (!owns) {
            _docked.value = false
            refreshNotification(docked = false)
            return
        }
        val saved = repo.getSavedTimeoutNow()
        // Clear ownership FIRST so a crash mid-restore can't double-restore later.
        repo.clearOverride()
        val ok = ScreenTimeoutController.restoreTimeout(this, saved)
        Log.d(TAG, "override released restored=$saved ok=$ok")
        _docked.value = false
        refreshNotification(docked = false)
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {
        }
        _isRunning.value = false
        scope.cancel()
        super.onDestroy()
        Log.d(TAG, "service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    /** Notification "Disable" action (manifest-declared receiver class). */
    class DisableActionReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent?) {
            if (intent?.action != NotificationController.ACTION_DISABLE) return
            CoroutineScope(Dispatchers.Default).launch {
                DockController.disableEverywhere(context.applicationContext)
            }
        }
    }

    companion object {
        private const val TAG = "DockieService"
        const val ACTION_DISABLE = "com.dockie.app.ACTION_DISABLE_SERVICE"
        const val DEFAULT_FALLBACK_TIMEOUT_MS = 60_000L

        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
        private val _docked = MutableStateFlow(false)
        val docked: StateFlow<Boolean> = _docked.asStateFlow()
        private val _wireless = MutableStateFlow(false)
        val wireless: StateFlow<Boolean> = _wireless.asStateFlow()
        private val _batteryPercent = MutableStateFlow<Int?>(null)
        val batteryPercent: StateFlow<Int?> = _batteryPercent.asStateFlow()

        private var reconcileJob: Job? = null

        fun start(context: Context) {
            val intent = Intent(context, DockMonitoringService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= 26) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "start failed", e)
            }
        }

        fun stop(context: Context) {
            try {
                context.stopService(Intent(context, DockMonitoringService::class.java))
            } catch (e: Exception) {
                Log.w(TAG, "stop failed", e)
            }
            _isRunning.value = false
        }
    }
}

/**
 * Single place that performs enable/disable transitions so the UI, the
 * notification action, and boot handling all behave identically:
 * disable -> persist disabled, restore timeout if owned, stop service.
 */
object DockController {
    private const val TAG = "DockieController"
    private val mutex = Mutex()

    suspend fun setEnabled(context: Context, enabled: Boolean) {
        val app = context.applicationContext as DockieApp
        mutex.withLock {
            if (enabled) {
                app.repository.setEnabled(true)
                DockMonitoringService.start(context.applicationContext)
            } else {
                awaitDisableLocked(context.applicationContext, app.repository)
            }
        }
    }

    suspend fun disableEverywhere(context: Context) {
        val app = context.applicationContext as? DockieApp ?: return
        mutex.withLock {
            awaitDisableLocked(context.applicationContext, app.repository)
        }
    }

    private suspend fun awaitDisableLocked(context: Context, repo: DockieRepository) {
        repo.setEnabled(false)
        if (repo.isOverrideActiveNow()) {
            val saved = repo.getSavedTimeoutNow()
            repo.clearOverride()
            val ok = ScreenTimeoutController.restoreTimeout(context, saved)
            Log.d(TAG, "disabled while docked: restored=$saved ok=$ok")
        }
        DockMonitoringService.stop(context)
    }
}
