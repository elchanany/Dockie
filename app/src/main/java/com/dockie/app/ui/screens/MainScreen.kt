package com.dockie.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ElectricalServices
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dockie.app.model.AppState
import com.dockie.app.ui.components.DockieControl
import com.dockie.app.ui.components.StatusCard
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    appState: AppState,
    remainingText: String?,
    showCelebration: Boolean,
    onCelebrationDismiss: () -> Unit,
    onToggle: () -> Unit,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // One-time "you're all set" confirmation after granting permission.
    LaunchedEffect(showCelebration) {
        if (showCelebration) {
            delay(3_500)
            onCelebrationDismiss()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    Icon(
                        imageVector = Icons.Outlined.ElectricalServices,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(26.dp),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                },
                actions = {
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.padding(end = 4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Dockie",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = "Stay awake while docked",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            AnimatedVisibility(
                visible = showCelebration,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300)),
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(top = 12.dp),
                ) {
                    Text(
                        text = "✓ You're all set",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            Spacer(Modifier.height(28.dp))

            Crossfade(
                targetState = appState::class.simpleName,
                animationSpec = tween(300),
                label = "state",
            ) {
                DockieControl(
                    appState = appState,
                    onToggle = onToggle,
                    onRequestPermission = onRequestPermission,
                )
            }

            // Timed session countdown, shown only while actively docked.
            AnimatedVisibility(
                visible = appState is AppState.Docked &&
                    !(appState as AppState.Docked).paused &&
                    remainingText != null,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(200)),
            ) {
                Text(
                    text = remainingText ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            AnimatedVisibility(
                visible = appState is AppState.PermissionRequired,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(200)),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(onClick = onRequestPermission) {
                        Text("Allow access", modifier = Modifier.padding(horizontal = 12.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            // Gentle enable/disable affordance under the control for clarity.
            if (appState is AppState.Monitoring || appState is AppState.Docked) {
                OutlinedButton(
                    onClick = onToggle,
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Text("Turn off")
                }
                Spacer(Modifier.height(12.dp))
            }

            StatusCard(appState = appState, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(24.dp))
            Text(
                text = when (appState) {
                    is AppState.Docked -> if (appState.paused) {
                        "Dockie is paused. Lift the phone and put it back to resume."
                    } else if (remainingText != null) {
                        "Screen stays awake for $remainingText."
                    } else {
                        "Take your phone off the charger and everything returns to normal."
                    }
                    is AppState.Monitoring -> "Put your phone on any wireless charger."
                    else -> ""
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}
