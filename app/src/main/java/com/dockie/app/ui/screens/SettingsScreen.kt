package com.dockie.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.dockie.app.BuildConfig
import com.dockie.app.data.DockieRepository.Companion.MODE_PAUSE
import com.dockie.app.data.DockieRepository.Companion.MODE_RESUME
import com.dockie.app.power.AwakeFormat
import com.dockie.app.ui.AdvancedInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themePreference: String,
    startAfterRestart: Boolean,
    alertOnDock: Boolean,
    statusIcon: Boolean,
    awakeMinutes: Int,
    screenOffMode: String,
    advanced: AdvancedInfo,
    onThemeChange: (String) -> Unit,
    onStartAfterRestartChange: (Boolean) -> Unit,
    onAlertOnDockChange: (Boolean) -> Unit,
    onStatusIconChange: (Boolean) -> Unit,
    onAwakeMinutesChange: (Int) -> Unit,
    onScreenOffModeChange: (String) -> Unit,
    onBack: () -> Unit,
    onRefreshAdvanced: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(Unit) { onRefreshAdvanced() }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Settings", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Section("While docked") {
                Text(
                    "How long should the screen stay awake?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Column(Modifier.selectableGroup()) {
                    AwakeFormat.PRESETS_MINUTES.forEach { minutes ->
                        val selected = awakeMinutes == minutes
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = selected,
                                    role = Role.RadioButton,
                                    onClick = { onAwakeMinutesChange(minutes) },
                                )
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = { onAwakeMinutesChange(minutes) },
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                AwakeFormat.label(minutes),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
                Text(
                    "A new duration applies to the next dock session.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                )
            }

            Section("Notifications") {
                SwitchRow(
                    title = "Docking alert",
                    body = "One-time heads-up when Dockie starts keeping the screen awake. " +
                        "Needs system notifications allowed for Dockie.",
                    checked = alertOnDock,
                    onCheckedChange = onAlertOnDockChange,
                )
                Spacer(Modifier.height(12.dp))
                SwitchRow(
                    title = "Status bar icon",
                    body = "Show the small Dockie icon at the top while monitoring. " +
                        "Off keeps a silent shade-only entry. Android always requires " +
                        "a silent entry while Dockie is active.",
                    checked = statusIcon,
                    onCheckedChange = onStatusIconChange,
                )
            }

            Section("Screen off") {
                Text(
                    "When you turn the screen off with the power button while docked:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Column(Modifier.selectableGroup()) {
                    listOf(
                        MODE_RESUME to "Stay active",
                        MODE_PAUSE to "Pause until re-docked",
                    ).forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = screenOffMode == value,
                                    role = Role.RadioButton,
                                    onClick = { onScreenOffModeChange(value) },
                                )
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = screenOffMode == value,
                                onClick = { onScreenOffModeChange(value) },
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(label, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    if (value == MODE_RESUME) {
                                        "Unlock and the screen keeps staying awake."
                                    } else {
                                        "Timeout is restored; lift and re-dock to resume."
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            Section("General") {
                SwitchRow(
                    title = "Start after restart",
                    body = "Resume watching for your dock after a reboot.",
                    checked = startAfterRestart,
                    onCheckedChange = onStartAfterRestartChange,
                )
            }

            Section("Appearance") {
                Column(Modifier.selectableGroup()) {
                    listOf("system" to "System", "light" to "Light", "dark" to "Dark").forEach { (value, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = themePreference == value,
                                    role = Role.RadioButton,
                                    onClick = { onThemeChange(value) },
                                )
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = themePreference == value,
                                onClick = { onThemeChange(value) },
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            Section("About") {
                SettingsRow("Version", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                Spacer(Modifier.height(8.dp))
                Text(
                    "Privacy",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "Dockie works completely offline. No accounts, no analytics, no network access. All settings stay on this phone.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Open-source licenses: AndroidX, Kotlin coroutines, Material 3 (Apache 2.0).",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Section("Advanced", technical = true) {
                SettingsRow("Current power source", advanced.powerSource)
                SettingsRow("Current timeout", advanced.currentTimeout)
                SettingsRow("Saved timeout", advanced.savedTimeout)
                SettingsRow("Override active", if (advanced.overrideActive) "Yes" else "No")
                SettingsRow("Awake mode", advanced.awakeMode)
                SettingsRow("Override ends in", advanced.overrideEnds)
                SettingsRow("Service status", if (advanced.serviceRunning) "Running" else "Stopped")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Technical diagnostics. Dockie restores the saved timeout only when it owns the override.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    body: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}

@Composable
private fun Section(
    title: String,
    technical: Boolean = false,
    content: @Composable () -> Unit,
) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = if (technical) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            else MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp, start = 4.dp),
        )
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            shadowElevation = 2.dp,
        ) {
            Column(Modifier.padding(20.dp)) { content() }
        }
    }
}

@Composable
private fun SettingsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
