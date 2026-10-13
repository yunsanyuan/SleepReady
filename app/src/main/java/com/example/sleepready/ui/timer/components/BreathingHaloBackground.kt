package com.example.sleepready.ui.timer.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.sleepready.ui.theme.DeepNight
import com.example.sleepready.ui.theme.MoonlightYellow
import com.example.sleepready.ui.theme.WarmOrange

/**
 * Warm dark background with a slow breathing halo behind the countdown digits.
 * Validates Requirements 10.3 (breathing halo) and 24.1 (warm-toned palette).
 */
@Composable
fun BreathingHaloBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "breathing-halo")
    val scale by transition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo-scale"
    )
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "halo-alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepNight)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2.4f)
            val radius = (size.minDimension / 2f) * scale
            val brush = Brush.radialGradient(
                colors = listOf(
                    WarmOrange.copy(alpha = alpha),
                    MoonlightYellow.copy(alpha = alpha * 0.6f),
                    Color.Transparent
                ),
                center = center,
                radius = radius
            )
            drawCircle(brush = brush, radius = radius, center = center)
        }
        content()
    }
}
