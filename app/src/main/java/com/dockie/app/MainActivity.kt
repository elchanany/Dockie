package com.dockie.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.dockie.app.model.AppState
import com.dockie.app.power.PermissionManager
import com.dockie.app.ui.MainViewModel
import com.dockie.app.ui.screens.MainScreen
import com.dockie.app.ui.screens.OnboardingScreen
import com.dockie.app.ui.screens.PermissionScreen
import com.dockie.app.ui.screens.SettingsScreen
import com.dockie.app.ui.theme.DockieTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appState by viewModel.state.collectAsState()
            val themePref by viewModel.themePreference.collectAsState()
            val startAfterRestart by viewModel.startAfterRestart.collectAsState()
            val advanced by viewModel.advancedInfo.collectAsState()
            var showSettings by remember { mutableStateOf(false) }
            var showPermissionExplainer by remember { mutableStateOf(false) }

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
                                advanced = advanced,
                                onThemeChange = viewModel::setTheme,
                                onStartAfterRestartChange = viewModel::setStartAfterRestart,
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
                                onToggle = {
                                    val enabled = when (appState) {
                                        is AppState.Disabled -> true
                                        is AppState.Monitoring, is AppState.Docked -> false
                                        else -> return@MainScreen
                                    }
                                    viewModel.setEnabled(enabled)
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
