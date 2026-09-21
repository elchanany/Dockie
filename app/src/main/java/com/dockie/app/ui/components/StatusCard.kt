package com.dockie.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.dockie.app.model.AppState

/** One small secondary card under the main control. Deliberately minimal. */
@Composable
fun StatusCard(appState: AppState, modifier: Modifier = Modifier) {
    val (line1, showBolt) = when (appState) {
        is AppState.Docked -> if (appState.paused) {
            ("Paused") to false
        } else {
            val pct = appState.batteryPercent?.let { "  ·  $it%" } ?: ""
            ("Wireless charging$pct") to true
        }
        is AppState.Monitoring -> {
            val pct = appState.batteryPercent?.let { "$it%" } ?: "—"
            ("Battery  ·  $pct") to false
        }
        is AppState.Disabled -> ("Battery" to false)
        else -> (null to false)
    }
    val line2 = when {
        appState is AppState.Docked && appState.paused -> "Lift and re-dock to resume"
        appState is AppState.Docked -> "Dockie is keeping your screen awake"
        else -> null
    }
    if (line1 == null) return

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (showBolt) Icons.Outlined.Bolt else Icons.Outlined.BatteryStd,
                    contentDescription = null,
                    tint = if (showBolt) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = line1,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            if (line2 != null) {
                Text(
                    text = line2,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, start = 34.dp),
                )
            }
        }
    }
}
