package com.dockie.app.model

/** Single source of truth for what the UI should show. */
sealed interface AppState {
    /** First launch, onboarding not finished. */
    data object Onboarding : AppState

    /** WRITE_SETTINGS not granted. */
    data object PermissionRequired : AppState

    /** User turned Dockie off. */
    data object Disabled : AppState

    /** Enabled, waiting for a wireless dock. */
    data class Monitoring(
        val batteryPercent: Int? = null,
        val powerSourceLabel: String? = null,
    ) : AppState

    /** Enabled and wireless power present — timeout override active. */
    data class Docked(
        val batteryPercent: Int? = null,
    ) : AppState

    /** Something unexpected; UI shows a calm retry state. */
    data class Error(val message: String) : AppState
}
