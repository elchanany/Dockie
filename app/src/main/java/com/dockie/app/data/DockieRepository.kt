package com.dockie.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dockieDataStore by preferencesDataStore(name = "dockie_prefs")

/**
 * Persistent Dockie state (DataStore).
 *
 * Ownership model:
 * - [overrideActive] is true only while Dockie itself currently owns the
 *   temporary SCREEN_OFF_TIMEOUT override.
 * - [savedTimeoutMs] is the exact value read immediately before Dockie
 *   applied its override, restored verbatim on undock / disable.
 * - We never restore blindly: restore happens only when [overrideActive]
 *   is true, and afterwards ownership is cleared first.
 */
class DockieRepository(private val appContext: Context) {

    private object Keys {
        val ENABLED = booleanPreferencesKey("enabled")
        val FIRST_RUN_DONE = booleanPreferencesKey("first_run_done")
        val OVERRIDE_ACTIVE = booleanPreferencesKey("override_active")
        val SAVED_TIMEOUT = longPreferencesKey("saved_timeout_ms")
        val START_AFTER_RESTART = booleanPreferencesKey("start_after_restart")
        val THEME = stringPreferencesKey("theme") // system | light | dark
        val ALERT_ON_DOCK = booleanPreferencesKey("alert_on_dock")
        val STATUS_ICON = booleanPreferencesKey("status_icon")
        val AWAKE_MINUTES = intPreferencesKey("awake_minutes") // 0 = until removed
        val OVERRIDE_UNTIL_ELAPSED = longPreferencesKey("override_until_elapsed") // 0 = none
        val SCREEN_OFF_MODE = stringPreferencesKey("screen_off_mode") // resume | pause
        val SCREEN_PAUSED = booleanPreferencesKey("screen_paused")
    }

    val enabled: Flow<Boolean> =
        appContext.dockieDataStore.data.map { it[Keys.ENABLED] ?: false }

    val firstRunDone: Flow<Boolean> =
        appContext.dockieDataStore.data.map { it[Keys.FIRST_RUN_DONE] ?: false }

    val overrideActive: Flow<Boolean> =
        appContext.dockieDataStore.data.map { it[Keys.OVERRIDE_ACTIVE] ?: false }

    val savedTimeoutMs: Flow<Long?> =
        appContext.dockieDataStore.data.map { it[Keys.SAVED_TIMEOUT] }

    val startAfterRestart: Flow<Boolean> =
        appContext.dockieDataStore.data.map { it[Keys.START_AFTER_RESTART] ?: true }

    val theme: Flow<String> =
        appContext.dockieDataStore.data.map { it[Keys.THEME] ?: "system" }

    val alertOnDock: Flow<Boolean> =
        appContext.dockieDataStore.data.map { it[Keys.ALERT_ON_DOCK] ?: true }

    val statusIcon: Flow<Boolean> =
        appContext.dockieDataStore.data.map { it[Keys.STATUS_ICON] ?: true }

    val awakeMinutes: Flow<Int> =
        appContext.dockieDataStore.data.map { it[Keys.AWAKE_MINUTES] ?: 0 }

    val screenOffMode: Flow<String> =
        appContext.dockieDataStore.data.map { it[Keys.SCREEN_OFF_MODE] ?: MODE_RESUME }

    suspend fun setAlertOnDock(value: Boolean) {
        appContext.dockieDataStore.edit { it[Keys.ALERT_ON_DOCK] = value }
    }

    suspend fun setStatusIcon(value: Boolean) {
        appContext.dockieDataStore.edit { it[Keys.STATUS_ICON] = value }
    }

    suspend fun setAwakeMinutes(value: Int) {
        appContext.dockieDataStore.edit { it[Keys.AWAKE_MINUTES] = value }
    }

    suspend fun setScreenOffMode(value: String) {
        appContext.dockieDataStore.edit { it[Keys.SCREEN_OFF_MODE] = value }
    }

    suspend fun setOverrideUntil(elapsedRealtimeMs: Long) {
        appContext.dockieDataStore.edit { it[Keys.OVERRIDE_UNTIL_ELAPSED] = elapsedRealtimeMs }
    }

    suspend fun clearOverrideUntil() {
        appContext.dockieDataStore.edit { it.remove(Keys.OVERRIDE_UNTIL_ELAPSED) }
    }

    suspend fun getOverrideUntilNow(): Long =
        appContext.dockieDataStore.data.map { it[Keys.OVERRIDE_UNTIL_ELAPSED] ?: 0L }.first()

    suspend fun setScreenPaused(value: Boolean) {
        appContext.dockieDataStore.edit { it[Keys.SCREEN_PAUSED] = value }
    }

    suspend fun isScreenPausedNow(): Boolean =
        appContext.dockieDataStore.data.map { it[Keys.SCREEN_PAUSED] ?: false }.first()

    suspend fun isAlertOnDockNow(): Boolean =
        appContext.dockieDataStore.data.map { it[Keys.ALERT_ON_DOCK] ?: true }.first()

    suspend fun isStatusIconNow(): Boolean =
        appContext.dockieDataStore.data.map { it[Keys.STATUS_ICON] ?: true }.first()

    suspend fun getAwakeMinutesNow(): Int =
        appContext.dockieDataStore.data.map { it[Keys.AWAKE_MINUTES] ?: 0 }.first()

    suspend fun getScreenOffModeNow(): String =
        appContext.dockieDataStore.data.map { it[Keys.SCREEN_OFF_MODE] ?: MODE_RESUME }.first()

    suspend fun setEnabled(value: Boolean) {
        appContext.dockieDataStore.edit { it[Keys.ENABLED] = value }
    }

    suspend fun setFirstRunDone(value: Boolean) {
        appContext.dockieDataStore.edit { it[Keys.FIRST_RUN_DONE] = value }
    }

    suspend fun setStartAfterRestart(value: Boolean) {
        appContext.dockieDataStore.edit { it[Keys.START_AFTER_RESTART] = value }
    }

    suspend fun setTheme(value: String) {
        appContext.dockieDataStore.edit { it[Keys.THEME] = value }
    }

    /** Atomically claim ownership together with the saved original value. */
    suspend fun claimOverride(savedTimeoutMs: Long) {
        appContext.dockieDataStore.edit {
            it[Keys.SAVED_TIMEOUT] = savedTimeoutMs
            it[Keys.OVERRIDE_ACTIVE] = true
        }
    }

    /** Release ownership after a successful (or best-effort) restore. */
    suspend fun clearOverride() {
        appContext.dockieDataStore.edit {
            it[Keys.OVERRIDE_ACTIVE] = false
        }
    }

    suspend fun isOverrideActiveNow(): Boolean =
        appContext.dockieDataStore.data.map { it[Keys.OVERRIDE_ACTIVE] ?: false }.first()

    suspend fun getSavedTimeoutNow(): Long? =
        appContext.dockieDataStore.data.map { it[Keys.SAVED_TIMEOUT] }.first()

    suspend fun isEnabledNow(): Boolean =
        appContext.dockieDataStore.data.map { it[Keys.ENABLED] ?: false }.first()

    suspend fun isFirstRunDoneNow(): Boolean =
        appContext.dockieDataStore.data.map { it[Keys.FIRST_RUN_DONE] ?: false }.first()

    companion object {
        const val MODE_RESUME = "resume"
        const val MODE_PAUSE = "pause"
    }
}
