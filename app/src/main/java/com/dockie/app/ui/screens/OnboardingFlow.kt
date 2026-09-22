package com.dockie.app.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Two lightweight first-run screens: the core idea, then the safety promise.
 * Cute looping Canvas illustrations, soft pager motion, minimal text.
 */
@Composable
fun OnboardingFlow(onFinish: () -> Unit, modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(pageCount = { 2 })
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false),
            verticalAlignment = Alignment.CenterVertically,
        ) { page ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (page == 0) DockArt() else RestoreArt()
                Spacer(Modifier.height(28.dp))
                if (page == 0) {
                    Text(
                        text = "Your screen, always awake on its dock.",
                        style = MaterialTheme.typography.displaySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Place your phone on any wireless charger and Dockie keeps the screen on for you.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        text = "Nothing changes permanently.",
                        style = MaterialTheme.typography.displaySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Dockie only borrows your screen timeout while docked, then puts your exact setting back when you lift the phone.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        DotsRow(current = pagerState.currentPage, total = 2)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                if (pagerState.currentPage == 0) {
                    scope.launch { pagerState.animateScrollToPage(1) }
                } else {
                    onFinish()
                }
            },
        ) {
            Text(
                if (pagerState.currentPage == 0) "Next" else "Get started",
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun DotsRow(current: Int, total: Int) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { i ->
            val width by animateDpAsState(
                targetValue = if (i == current) 24.dp else 8.dp,
                animationSpec = tween(300),
                label = "dot",
            )
            Surface(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(width = width, height = 8.dp)
                    .clip(CircleShape),
                color = if (i == current) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                },
            ) {}
            if (i < total - 1) Spacer(Modifier.width(0.dp))
        }
    }
}

/** Page 1: a little glowing phone resting on its dock, waves rising. */
@Composable
private fun DockArt(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "dock")
    val glow by transition.animateFloat(
        initialValue = 0.55f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "glow",
    )
    val wave0 by transition.animateFloat(
        initialValue = 0.15f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2100), RepeatMode.Restart, initialStartOffset = StartOffset(0),
        ),
        label = "w0",
    )
    val wave1 by transition.animateFloat(
        initialValue = 0.15f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2100), RepeatMode.Restart, initialStartOffset = StartOffset(700),
        ),
        label = "w1",
    )
    val wave2 by transition.animateFloat(
        initialValue = 0.15f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(2100), RepeatMode.Restart, initialStartOffset = StartOffset(1400),
        ),
        label = "w2",
    )

    val ink = MaterialTheme.colorScheme.onBackground
    val glowColor = MaterialTheme.colorScheme.primary
    val glowSoft = MaterialTheme.colorScheme.primaryContainer
    val body = MaterialTheme.colorScheme.surface

    Canvas(modifier = modifier.size(220.dp)) {
        val cx = size.width / 2f
        val baseY = size.height - 28f
        // Dock dish.
        val dish = Path().apply {
            moveTo(cx - 88f, baseY - 12f)
            quadraticTo(cx, baseY + 24f, cx + 88f, baseY - 12f)
        }
        drawPath(dish, ink, style = Stroke(width = 11f, cap = StrokeCap.Round))
        // Phone body.
        drawRoundRect(
            color = body,
            topLeft = Offset(cx - 54f, baseY - 196f),
            size = androidx.compose.ui.geometry.Size(108f, 172f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
        )
        drawRoundRect(
            color = ink,
            topLeft = Offset(cx - 54f, baseY - 196f),
            size = androidx.compose.ui.geometry.Size(108f, 172f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
            style = Stroke(width = 7f),
        )
        // Glowing screen.
        drawRoundRect(
            color = glowSoft.copy(alpha = 0.45f * glow),
            topLeft = Offset(cx - 44f, baseY - 186f),
            size = androidx.compose.ui.geometry.Size(88f, 152f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16f, 16f),
        )
        drawRoundRect(
            color = glowColor.copy(alpha = 0.9f),
            topLeft = Offset(cx - 34f, baseY - 176f),
            size = androidx.compose.ui.geometry.Size(68f, 112f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
        )
        // Rising wireless waves.
        val alphas = listOf(wave0, wave1, wave2)
        val radii = listOf(34f, 58f, 82f)
        for (i in 0..2) {
            drawArc(
                color = glowColor.copy(alpha = alphas[i] * 0.9f),
                startAngle = 200f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(cx - radii[i], baseY - 196f - radii[i]),
                size = androidx.compose.ui.geometry.Size(radii[i] * 2f, radii[i] * 2f),
                style = Stroke(width = 8f, cap = StrokeCap.Round),
            )
        }
    }
}

/** Page 2: a clock embraced by a slowly circling arrow, check badge pulsing. */
@Composable
private fun RestoreArt(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "restore")
    val rotation by transition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing)),
        label = "spin",
    )
    val pop by transition.animateFloat(
        initialValue = 1f, targetValue = 1.12f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "pop",
    )

    val ink = MaterialTheme.colorScheme.onBackground
    val accent = MaterialTheme.colorScheme.primary
    val soft = MaterialTheme.colorScheme.primaryContainer
    val onAccent = MaterialTheme.colorScheme.onPrimary

    Canvas(modifier = modifier.size(220.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f - 8f
        // Circling arrow.
        rotate(rotation, Offset(cx, cy)) {
            drawArc(
                color = accent,
                startAngle = -50f,
                sweepAngle = 295f,
                useCenter = false,
                topLeft = Offset(cx - 84f, cy - 84f),
                size = androidx.compose.ui.geometry.Size(168f, 168f),
                style = Stroke(width = 9f, cap = StrokeCap.Round),
            )
            // Arrowhead at the arc end (angle ~245°).
            val endAngle = Math.toRadians(245.0)
            val tip = Offset(
                cx + 84f * kotlin.math.cos(endAngle).toFloat(),
                cy + 84f * kotlin.math.sin(endAngle).toFloat(),
            )
            val head = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(tip.x - 22f, tip.y - 2f)
                lineTo(tip.x - 8f, tip.y + 20f)
                close()
            }
            drawPath(head, accent)
        }
        // Clock face.
        drawCircle(soft, radius = 56f, center = Offset(cx, cy))
        drawCircle(
            ink, radius = 56f, center = Offset(cx, cy),
            style = Stroke(width = 7f),
        )
        drawLine(
            ink, Offset(cx, cy), Offset(cx, cy - 30f),
            strokeWidth = 8f, cap = StrokeCap.Round,
        )
        drawLine(
            ink, Offset(cx, cy), Offset(cx + 20f, cy + 8f),
            strokeWidth = 8f, cap = StrokeCap.Round,
        )
        drawCircle(ink, radius = 6f, center = Offset(cx, cy))
        // Check badge.
        val bx = cx + 52f
        val by = cy + 52f
        drawCircle(accent, radius = 21f * pop, center = Offset(bx, by))
        val check = Path().apply {
            moveTo(bx - 9f * pop, by + 1f)
            lineTo(bx - 2f * pop, by + 8f * pop)
            lineTo(bx + 10f * pop, by - 7f * pop)
        }
        drawPath(check, onAccent, style = Stroke(width = 5f * pop, cap = StrokeCap.Round))
    }
}
