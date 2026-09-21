package com.dockie.app.service

import android.Manifest
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.ServiceCompat
import com.dockie.app.DockieApp
import com.dockie.app.data.DockieRepository
import com.dockie.app.data.DockieRepository.Companion.MODE_PAUSE
import com.dockie.app.notify.NotificationController
import com.dockie.app.power.AwakeFormat
import com.dockie.app.power.ChargingStateObserver
import com.dockie.app.power.ScreenTimeoutController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Lightweight foreground service that owns the docked/undocked transitions.
 *
 * Event-driven: battery broadcasts plus screen on/off events. No polling,
 * no wake locks, no network. Near-zero idle cost.
 *
 * Timed sessions: when the user picks a finite "stay awake" duration, the
 * override automatically ends after that window even if the phone is still
 * docked. The deadline is persisted, so process death mid-session still
 * expires correctly on reconcile.
 *
 * Screen-off behavior is user configurable:
 * - resume (default): a manual screen-off changes nothing; unlocking while
 *   still docked keeps the long timeout.
 * - pause: a manual screen-off restores the saved timeout and pauses Dockie
 *   until the phone is lifted and re-docked.
 *
 * Startup reconciliation (process restart / reboot / crash):
 * - If persisted state says an override is owned, check the live power source.
 * - Wireless still present -> keep the override (re-apply defensively),
 *   unless the timed window already expired or the session is paused.
 * - Otherwise -> restore the saved timeout immediately and clear ownership.
 */
class DockMonitoringService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()
    private lateinit var repo: DockieRepository
    private var timerJob: Job? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return
            scope.launch { handlePowerIntent() }
        }
    }

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            scope.launch {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> handleScreenOff()
                    Intent.ACTION_SCREEN_ON -> handleScreenOn()
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        repo = (application as DockieApp).repository
        registerCompat(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        registerCompat(
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            },
        )
        Log.d(TAG, "service created")
    }

    private fun registerCompat(receiver: BroadcastReceiver, filter: IntentFilter) {
        // Android 14+ requires an explicit export flag for dynamic receivers.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            registerReceiver(receiver, filter)
        }
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

    private suspend fun startForegroundQuietly(docked: Boolean) {
        val notification = if (docked) {
            NotificationController.buildDocked(this, repo.isStatusIconNow())
        } else {
            NotificationController.buildMonitoring(this)
        }
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

    private suspend fun refreshNotification(docked: Boolean, statusExtra: String? = null) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as? android.app.NotificationManager
            ?: return
        // Cancel-then-notify under one ID so the other channel's entry can
        // never linger next to it — exactly one Dockie entry ever exists.
        try {
            manager.cancel(NotificationController.NOTIFICATION_ID)
            manager.notify(
                NotificationController.NOTIFICATION_ID,
                if (docked) {
                    NotificationController.buildDocked(
                        this, repo.isStatusIconNow(), statusExtra,
                    )
                } else {
                    NotificationController.buildMonitoring(this)
                },
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
                // ensureOverrideLocked re-applies, reschedules the timer, and
                // honors pause/expiry markers — but never re-posts the alert.
                ensureOverrideLocked(postAlert = false)
            } else {
                // Charger left while we were dead: restore and start fresh.
                fullClearLocked()
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
                ensureOverrideLocked(postAlert = true)
            } else {
                fullClearLocked()
            }
        }
    }

    private suspend fun handleScreenOff() {
        mutex.withLock {
            if (!repo.isOverrideActiveNow()) return
            if (repo.getScreenOffModeNow() != MODE_PAUSE) {
                Log.d(TAG, "screen off: staying active (resume mode)")
                return
            }
            restoreLocked()
            repo.setScreenPaused(true)
            _paused.value = true
            refreshNotification(docked = false)
            Log.d(TAG, "screen off: paused until re-docked")
        }
    }

    private suspend fun handleScreenOn() {
        mutex.withLock {
            if (!repo.isEnabledNow()) return
            val snapshot = ChargingStateObserver.readSnapshot(applicationContext)
            _wireless.value = snapshot.isWireless
            _batteryPercent.value = snapshot.batteryPercent
            if (!snapshot.isWireless) {
                fullClearLocked()
                return
            }
            if (repo.getScreenOffModeNow() == MODE_PAUSE && repo.isScreenPausedNow()) {
                _docked.value = false
                _paused.value = true
                refreshNotification(docked = false)
                Log.d(TAG, "screen on: still paused until re-docked")
                return
            }
            // Resume mode (or never paused): make sure the override is applied.
            ensureOverrideLocked(postAlert = false)
        }
    }

    /**
     * Must hold [mutex]. Captures the original timeout once, then applies
     * the override. Honors timed-window expiry and the paused-session marker.
     */
    private suspend fun ensureOverrideLocked(postAlert: Boolean) {
        if (!ScreenTimeoutController.canWrite(this)) {
            Log.w(TAG, "no WRITE_SETTINGS; cannot apply override")
            refreshNotification(docked = false)
            _docked.value = false
            return
        }
        if (repo.getScreenOffModeNow() == MODE_PAUSE && repo.isScreenPausedNow()) {
            _docked.value = false
            _paused.value = true
            refreshNotification(docked = false)
            return
        }
        val alreadyOwned = repo.isOverrideActiveNow()
        val minutes = repo.getAwakeMinutesNow()
        if (!alreadyOwned) {
            if (minutes > 0) {
                val until = repo.getOverrideUntilNow()
                val now = SystemClock.elapsedRealtime()
                if (until != 0L && now >= until) {
                    // This dock session already used up its timed window.
                    _docked.value = false
                    _paused.value = false
                    refreshNotification(docked = false)
                    Log.d(TAG, "timed window already exhausted for this session")
                    return
                }
                if (until == 0L) {
                    repo.setOverrideUntil(now + minutes * 60_000L)
                }
            }
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
        _paused.value = false
        refreshNotification(docked = ok)
        if (ok && !alreadyOwned && postAlert && repo.isAlertOnDockNow() && canPostAlert()) {
            NotificationController.postDockedAlert(this)
        }
        scheduleTimerLocked()
        val saved = repo.getSavedTimeoutNow()
        Log.d(TAG, "override acquired saved=$saved ok=$ok")
    }

    /** Must hold [mutex]. Restores only when Dockie owns the override. */
    private suspend fun restoreLocked() {
        timerJob?.cancel()
        timerJob = null
        // The session is over: the "staying awake" alert must not linger.
        NotificationController.cancelAlert(this)
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

    /**
     * Must hold [mutex]. Full session teardown for undock/disable:
     * restore if owned, drop the timed deadline and the paused marker.
     */
    private suspend fun fullClearLocked() {
        restoreLocked()
        repo.clearOverrideUntil()
        repo.setScreenPaused(false)
        _paused.value = false
    }

    /**
     * Must hold [mutex] when called. (Re)starts the expiry/countdown job for
     * finite sessions. The deadline is absolute and persisted, so it survives
     * process death; reconcile reschedules via [ensureOverrideLocked].
     */
    private suspend fun scheduleTimerLocked() {
        timerJob?.cancel()
        timerJob = null
        val minutes = repo.getAwakeMinutesNow()
        if (minutes <= 0 || !_docked.value) return
        timerJob = scope.launch {
            while (true) {
                val until = repo.getOverrideUntilNow()
                val remaining = until - SystemClock.elapsedRealtime()
                if (remaining <= 0) {
                    mutex.withLock {
                        if (repo.isOverrideActiveNow()) {
                            Log.d(TAG, "timed window elapsed: restoring")
                            restoreLocked()
                        }
                    }
                    break
                }
                refreshNotification(
                    docked = true,
                    statusExtra = AwakeFormat.minutesLeft(remaining),
                )
                delay(minOf(30_000L, remaining))
            }
        }
    }

    private fun canPostAlert(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(batteryReceiver)
        } catch (_: Exception) {
        }
        try {
            unregisterReceiver(screenReceiver)
        } catch (_: Exception) {
        }
        timerJob?.cancel()
        timerJob = null
        NotificationController.cancelAlert(this)
        resetState()
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
        private val _paused = MutableStateFlow(false)
        val paused: StateFlow<Boolean> = _paused.asStateFlow()

        internal fun resetState() {
            _isRunning.value = false
            _docked.value = false
            _paused.value = false
        }

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
            resetState()
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
        NotificationController.cancelAlert(context)
        if (repo.isOverrideActiveNow()) {
            val saved = repo.getSavedTimeoutNow()
            repo.clearOverride()
            val ok = ScreenTimeoutController.restoreTimeout(context, saved)
            Log.d(TAG, "disabled while docked: restored=$saved ok=$ok")
        }
        repo.clearOverrideUntil()
        repo.setScreenPaused(false)
        DockMonitoringService.stop(context)
    }
}
