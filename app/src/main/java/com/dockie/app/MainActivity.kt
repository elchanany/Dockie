package com.dockie.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.dockie.app.model.AppState
import com.dockie.app.power.AwakeFormat
import com.dockie.app.power.PermissionManager
import com.dockie.app.ui.MainViewModel
import com.dockie.app.ui.screens.MainScreen
import com.dockie.app.ui.screens.OnboardingScreen
import com.dockie.app.ui.screens.PermissionScreen
import com.dockie.app.ui.screens.SettingsScreen
import com.dockie.app.ui.theme.DockieTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val notifPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            // Result is best-effort: without it the quiet status notification
            // still works and the one-time alert is simply skipped.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appState by viewModel.state.collectAsState()
            val themePref by viewModel.themePreference.collectAsState()
            val startAfterRestart by viewModel.startAfterRestart.collectAsState()
            val advanced by viewModel.advancedInfo.collectAsState()
            val alertOnDock by viewModel.alertOnDock.collectAsState()
            val statusIcon by viewModel.statusIcon.collectAsState()
            val awakeMinutes by viewModel.awakeMinutes.collectAsState()
            val screenOffMode by viewModel.screenOffMode.collectAsState()
            val remainingMs by viewModel.awakeRemainingMs.collectAsState()
            var showSettings by remember { mutableStateOf(false) }
            var showPermissionExplainer by remember { mutableStateOf(false) }
            var showCelebration by remember { mutableStateOf(false) }
            val haptics = LocalHapticFeedback.current

            // The permission screen is only a stepping stone: the moment the
            // system reports the permission as granted, leave it and confirm.
            // (This also covers granting while the app was in the background.)
            LaunchedEffect(appState) {
                val granted = appState !is AppState.PermissionRequired &&
                    appState !is AppState.Onboarding
                if (showPermissionExplainer && granted) {
                    showPermissionExplainer = false
                    showCelebration = true
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                }
            }

            DockieTheme(themePreference = themePref) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    Crossfade(
                        targetState = when {
                            showSettings -> "settings"
                            appState is AppState.Onboarding -> "onboarding"
                            showPermissionExplainer -> "permission"
                            else -> "main"
                        },
                        animationSpec = tween(300),
                        label = "nav",
                    ) { route ->
                        when (route) {
                            "settings" -> SettingsScreen(
                                themePreference = themePref,
                                startAfterRestart = startAfterRestart,
                                alertOnDock = alertOnDock,
                                statusIcon = statusIcon,
                                awakeMinutes = awakeMinutes,
                                screenOffMode = screenOffMode,
                                advanced = advanced,
                                onThemeChange = viewModel::setTheme,
                                onStartAfterRestartChange = viewModel::setStartAfterRestart,
                                onAlertOnDockChange = viewModel::setAlertOnDock,
                                onStatusIconChange = viewModel::setStatusIcon,
                                onAwakeMinutesChange = viewModel::setAwakeMinutes,
                                onScreenOffModeChange = viewModel::setScreenOffMode,
                                onBack = { showSettings = false },
                                onRefreshAdvanced = viewModel::refreshAdvanced,
                            )
                            "onboarding" -> OnboardingScreen(
                                onGetStarted = {
                                    viewModel.completeOnboarding()
                                    if (!PermissionManager.hasWriteSettings(this@MainActivity)) {
                                        showPermissionExplainer = true
                                    }
                                },
                            )
                            "permission" -> PermissionScreen(
                                onAllow = {
                                    startActivity(PermissionManager.writeSettingsIntent(this@MainActivity))
                                },
                                onBack = { showPermissionExplainer = false },
                            )
                            else -> MainScreen(
                                appState = appState,
                                remainingText = remainingMs?.let { AwakeFormat.minutesLeft(it) },
                                showCelebration = showCelebration,
                                onCelebrationDismiss = { showCelebration = false },
                                onToggle = {
                                    when (appState) {
                                        is AppState.Disabled -> viewModel.onEnabledWithAlertCheck(
                                            requestPermission = {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                                    ContextCompat.checkSelfPermission(
                                                        this@MainActivity,
                                                        Manifest.permission.POST_NOTIFICATIONS,
                                                    ) != PackageManager.PERMISSION_GRANTED
                                                ) {
                                                    notifPermissionLauncher.launch(
                                                        Manifest.permission.POST_NOTIFICATIONS,
                                                    )
                                                }
                                            },
                                        )
                                        is AppState.Monitoring, is AppState.Docked ->
                                            viewModel.setEnabled(false)
                                        else -> Unit
                                    }
                                },
                                onRequestPermission = { showPermissionExplainer = true },
                                onOpenSettings = { showSettings = true },
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }
}
