package com.dockie.app.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dockie.app.DockieApp
import com.dockie.app.model.AppState
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
)

private data class PowerInfo(
    val source: PowerSource,
    val pct: Int?,
)

data class AdvancedInfo(    val powerSource: String = "—",
    val currentTimeout: String = "—",
    val savedTimeout: String = "—",
    val overrideActive: Boolean = false,
    val serviceRunning: Boolean = false,
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

    val state: StateFlow<AppState> = combine(
        combine(
            repository.enabled,
            repository.firstRunDone,
            hasPermission,
            DockMonitoringService.docked,
        ) { enabled, firstRun, permission, docked ->
            DockieFlags(enabled, firstRun, permission, docked)
        },
        combine(powerSource, batteryPercent) { source, pct -> PowerInfo(source, pct) },
    ) { flags, power ->
        when {
            !flags.firstRun -> AppState.Onboarding
            !flags.permission -> AppState.PermissionRequired
            !flags.enabled -> AppState.Disabled
            flags.docked || power.source == PowerSource.WIRELESS ->
                AppState.Docked(power.pct)
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
                            delay(3_000)
                        }
                    }
                } else {
                    refreshPower()
                }
            }
        }
        viewModelScope.launch {
            DockMonitoringService.wireless.collect { wireless ->
                if (wireless) refreshPower()
            }
        }
    }

    fun onResume() {
        hasPermission.value = PermissionManager.hasWriteSettings(getApplication())
        viewModelScope.launch { refreshPower() }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch {
            DockController.setEnabled(getApplication(), enabled)
            refreshPower()
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

    fun refreshAdvanced() {
        viewModelScope.launch {
            val ctx = getApplication<Application>()
            val snap = ChargingStateObserver.readSnapshotIO(ctx)
            val current = ScreenTimeoutController.readCurrentTimeoutMs(ctx)
            val saved = repository.getSavedTimeoutNow()
            val owns = repository.isOverrideActiveNow()
            advanced.value = AdvancedInfo(
                powerSource = ChargingStateObserver.label(snap.source),
                currentTimeout = current?.let { formatTimeout(it) } ?: "—",
                savedTimeout = saved?.let { formatTimeout(it) } ?: "—",
                overrideActive = owns,
                serviceRunning = DockMonitoringService.isRunning.value,
            )
        }
    }

    private suspend fun refreshPower() {
        val ctx = getApplication<Application>()
        val snap = ChargingStateObserver.readSnapshotIO(ctx)
        powerSource.value = snap.source
        batteryPercent.value = snap.batteryPercent
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
