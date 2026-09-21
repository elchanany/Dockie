package com.dockie.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.ElectricalServices
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.dockie.app.model.AppState
import com.dockie.app.ui.theme.DarkGlow
import com.dockie.app.ui.theme.LightGlow

/**
 * The visual centerpiece: one large circular master control.
 *
 * Four calm visual states (off / monitoring w/ breathing / docked w/ glow /
 * permission) with spring-free soft tweens (200–400ms) and a gentle glow
 * pulse only while docked.
 */
@Composable
fun DockieControl(
    appState: AppState,
    onToggle: () -> Unit,
    onRequestPermission: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = LocalHapticFeedback.current
    val isDark = androidx.compose.foundation.isSystemInDarkTheme()

    val needsPermission = appState is AppState.PermissionRequired
    val dockedState = appState as? AppState.Docked
    val isDocked = dockedState != null && !dockedState.paused && !dockedState.timedOut
    val isPaused = dockedState?.paused == true
    val isMonitoring = appState is AppState.Monitoring
    val isOff = appState is AppState.Disabled

    val (title, subtitle, icon) = when (appState) {
        is AppState.Disabled -> Triple("Dockie is off", "Tap to enable", Icons.Outlined.Bedtime)
        is AppState.Monitoring -> Triple("Ready", "Waiting for your dock", Icons.Outlined.ElectricalServices)
        is AppState.Docked -> when {
            appState.paused -> Triple("Paused", "Lift and re-dock to resume", Icons.Outlined.Bedtime)
            appState.timedOut -> Triple("Time's up", "Lift and re-dock for more", Icons.Outlined.Bedtime)
            else -> Triple("Docked", "Screen will stay awake", Icons.Outlined.Bolt)
        }
        is AppState.PermissionRequired -> Triple("One quick setup", "Allow Dockie to control screen timeout", Icons.Outlined.Settings)
        is AppState.Onboarding -> Triple("Dockie", "Stay awake while docked", Icons.Outlined.ElectricalServices)
        is AppState.Error -> Triple("Something paused", (appState as AppState.Error).message, Icons.Outlined.Bedtime)
    }

    // Subtle breathing while monitoring.
    val breath by rememberInfiniteTransition(label = "breath").animateFloat(
        initialValue = 1f,
        targetValue = if (isMonitoring) 1.035f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathScale",
    )

    // Gentle glow pulse while docked.
    val glowPulse by rememberInfiniteTransition(label = "glow").animateFloat(
        initialValue = 0.55f,
        targetValue = if (isDocked) 1f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )

    val glowColor: Color = if (isDark) DarkGlow else LightGlow

    Box(
        modifier = modifier.size(280.dp),
        contentAlignment = Alignment.Center,
    ) {
        // Soft glow halo behind the control when docked.
        if (isDocked || glowPulse > 0.02f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(breath)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                glowColor.copy(alpha = 0.35f * glowPulse),
                                Color.Transparent,
                            ),
                        ),
                        shape = CircleShape,
                    ),
            )
        }

        val surfaceColor = when {
            isDocked -> MaterialTheme.colorScheme.primaryContainer
            isMonitoring -> MaterialTheme.colorScheme.surface
            isOff -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.75f)
            else -> MaterialTheme.colorScheme.surface
        }

        Box(
            modifier = Modifier
                .size(232.dp)
                .scale(if (isMonitoring || isDocked) breath else 1f)
                .shadow(
                    elevation = if (isDocked) 14.dp else 6.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = if (isDocked) glowColor.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.08f),
                    spotColor = if (isDocked) glowColor.copy(alpha = 0.4f) else Color.Black.copy(alpha = 0.08f),
                )
                .clip(CircleShape)
                .background(surfaceColor)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = true),
                    role = Role.Switch,
                    onClickLabel = if (isOff) "Enable Dockie" else "Disable Dockie",
                ) {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (needsPermission) onRequestPermission() else onToggle()
                },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                ControlIcon(icon = icon, active = isDocked, dimmed = isOff)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun ControlIcon(icon: ImageVector, active: Boolean, dimmed: Boolean) {
    val tint = when {
        active -> MaterialTheme.colorScheme.primary
        dimmed -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.onSurface
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(52.dp),
        tint = tint,
    )
}
