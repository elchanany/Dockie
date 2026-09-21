package com.dockie.app.ui

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dockie.app.DockieApp
import com.dockie.app.model.AppState
import com.dockie.app.notify.NotificationController
import com.dockie.app.power.AwakeFormat
import com.dockie.app.power.ChargingStateObserver
import com.dockie.app.power.PermissionManager
import com.dockie.app.power.PowerSource
import com.dockie.app.power.ScreenTimeoutController
import com.dockie.app.service.DockController
import com.dockie.app.service.DockMonitoringService
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private data class DockieFlags(
    val enabled: Boolean,
    val firstRun: Boolean,
    val permission: Boolean,
    val docked: Boolean,
    val paused: Boolean,
)

private data class PowerInfo(
    val source: PowerSource,
    val pct: Int?,
)

data class AdvancedInfo(
    val powerSource: String = "—",
    val currentTimeout: String = "—",
    val savedTimeout: String = "—",
    val overrideActive: Boolean = false,
    val serviceRunning: Boolean = false,
    val awakeMode: String = "—",
    val overrideEnds: String = "—",
)

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = (app as DockieApp).repository

    private val hasPermission = MutableStateFlow(
        PermissionManager.hasWriteSettings(app),
    )
    private val powerSource = MutableStateFlow(PowerSource.UNKNOWN)
    private val batteryPercent = MutableStateFlow<Int?>(null)
    private val advanced = MutableStateFlow(AdvancedInfo())
    private var pollJob: Job? = null

    val themePreference: StateFlow<String> = repository.theme.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), "system",
    )
    val startAfterRestart: StateFlow<Boolean> = repository.startAfterRestart.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), true,
    )
    val alertOnDock: StateFlow<Boolean> = repository.alertOnDock.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), true,
    )
    val statusIcon: StateFlow<Boolean> = repository.statusIcon.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), true,
    )
    val awakeMinutes: StateFlow<Int> = repository.awakeMinutes.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), 0,
    )
    val screenOffMode: StateFlow<String> = repository.screenOffMode.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), "resume",
    )

    /** Milliseconds left in a timed session, or null when infinite/inactive. */
    val awakeRemainingMs = MutableStateFlow<Long?>(null)

    val state: StateFlow<AppState> = combine(
        combine(
            repository.enabled,
            repository.firstRunDone,
            hasPermission,
            DockMonitoringService.docked,
            DockMonitoringService.paused,
        ) { enabled, firstRun, permission, docked, paused ->
            DockieFlags(enabled, firstRun, permission, docked, paused)
        },
        combine(powerSource, batteryPercent) { source, pct -> PowerInfo(source, pct) },
    ) { flags, power ->
        when {
            !flags.firstRun -> AppState.Onboarding
            !flags.permission -> AppState.PermissionRequired
            !flags.enabled -> AppState.Disabled
            flags.docked || power.source == PowerSource.WIRELESS ->
                AppState.Docked(power.pct, paused = flags.paused)
            else -> AppState.Monitoring(
                batteryPercent = power.pct,
                powerSourceLabel = ChargingStateObserver.label(power.source),
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppState.Onboarding)

    val advancedInfo: StateFlow<AdvancedInfo> = advanced

    init {
        viewModelScope.launch {
            repository.enabled.collect { enabled ->
                pollJob?.cancel()
                if (enabled) {
                    pollJob = launch {
                        while (true) {
                            refreshPower()
                            refreshRemaining()
                            delay(2_000)
                        }
                    }
                } else {
                    refreshPower()
                    awakeRemainingMs.value = null
                }
            }
        }
        viewModelScope.launch {
            DockMonitoringService.wireless.collect { wireless ->
                if (wireless) refreshPower()
            }
        }
        viewModelScope.launch {
            DockMonitoringService.docked.collect { docked ->
                if (!docked) awakeRemainingMs.value = null else refreshRemaining()
            }
        }
    }

    fun onResume() {
        hasPermission.value = PermissionManager.hasWriteSettings(getApplication())
        viewModelScope.launch {
            refreshPower()
            refreshRemaining()
        }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            DockController.setEnabled(getApplication(), enabled)
            refreshPower()
            refreshRemaining()
        }
    }

    /**
     * Called when the user enables Dockie. If the one-time docking alert is
     * on but the system notification permission (Android 13+) is missing,
     * [requestPermission] is invoked so the alert can actually appear.
     */
    fun onEnabledWithAlertCheck(requestPermission: () -> Unit) {
        setEnabled(true)
        viewModelScope.launch {
            if (!repository.isAlertOnDockNow()) return@launch
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@launch
            val ctx = getApplication<Application>()
            val granted = ContextCompat.checkSelfPermission(
                ctx, Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) requestPermission()
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch { repository.setFirstRunDone(true) }
    }

    fun setStartAfterRestart(value: Boolean) {
        viewModelScope.launch { repository.setStartAfterRestart(value) }
    }

    fun setTheme(value: String) {
        viewModelScope.launch { repository.setTheme(value) }
    }

    fun setAlertOnDock(value: Boolean) {
        viewModelScope.launch { repository.setAlertOnDock(value) }
    }

    fun setStatusIcon(value: Boolean) {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            repository.setStatusIcon(value)
            NotificationController.syncStatusChannel(ctx, value)
            if (DockMonitoringService.isRunning.value) {
                try {
                    val manager =
                        ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                    manager?.notify(
                        NotificationController.NOTIFICATION_ID,
                        NotificationController.build(
                            ctx,
                            DockMonitoringService.docked.value,
                            value,
                        ),
                    )
                } catch (_: Exception) {
                }
            }
        }
    }

    fun setAwakeMinutes(value: Int) {
        viewModelScope.launch {
            repository.setAwakeMinutes(value)
            // A new duration applies to the next dock session; an active
            // infinite session keeps running, an active timed one keeps its
            // already-promised deadline.
            refreshRemaining()
        }
    }

    fun setScreenOffMode(value: String) {
        viewModelScope.launch { repository.setScreenOffMode(value) }
    }

    fun refreshAdvanced() {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val snap = ChargingStateObserver.readSnapshotIO(ctx)
            val current = ScreenTimeoutController.readCurrentTimeoutMs(ctx)
            val saved = repository.getSavedTimeoutNow()
            val owns = repository.isOverrideActiveNow()
            val minutes = repository.getAwakeMinutesNow()
            val until = repository.getOverrideUntilNow()
            val ends = if (owns && minutes > 0 && until > 0) {
                AwakeFormat.minutesLeft(until - SystemClock.elapsedRealtime())
            } else {
                "—"
            }
            advanced.value = AdvancedInfo(
                powerSource = ChargingStateObserver.label(snap.source),
                currentTimeout = current?.let { formatTimeout(it) } ?: "—",
                savedTimeout = saved?.let { formatTimeout(it) } ?: "—",
                overrideActive = owns,
                serviceRunning = DockMonitoringService.isRunning.value,
                awakeMode = AwakeFormat.label(minutes),
                overrideEnds = ends,
            )
        }
    }

    private suspend fun refreshPower() {
        val ctx = getApplication<Application>()
        val snap = ChargingStateObserver.readSnapshotIO(ctx)
        powerSource.value = snap.source
        batteryPercent.value = snap.batteryPercent
    }

    private suspend fun refreshRemaining() {
        if (!DockMonitoringService.docked.value) {
            awakeRemainingMs.value = null
            return
        }
        val minutes = repository.getAwakeMinutesNow()
        if (minutes <= 0) {
            awakeRemainingMs.value = null
            return
        }
        val until = repository.getOverrideUntilNow()
        if (until <= 0) {
            awakeRemainingMs.value = null
            return
        }
        awakeRemainingMs.value = (until - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
    }

    companion object {
        fun formatTimeout(ms: Long): String {
            if (ms >= Int.MAX_VALUE.toLong() - 60_000L) return "Never (Dockie)"
            val seconds = ms / 1_000
            return when {
                seconds < 60 -> "$seconds sec"
                seconds % 60 == 0L && seconds < 3600 -> "${seconds / 60} min"
                seconds < 3600 -> "${seconds / 60} min ${seconds % 60} sec"
                else -> "${seconds / 3600} hr"
            }
        }
    }
}
