package com.example.sleepready.ui.timer.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.sleepready.ui.theme.MoonlightYellow

/**
 * 倒计时 MM:SS 数字，带脉动呼吸光晕。
 */
@Composable
fun CountdownDigits(remainingMs: Long, modifier: Modifier = Modifier) {
    val totalSeconds = (remainingMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    val text = "%02d:%02d".format(minutes, seconds)

    // 呼吸光晕动画：2.5 秒循环，20% → 60% 亮度
    val infinite = rememberInfiniteTransition(label = "breathe")
    val glowAlpha by infinite.animateFloat(
        initialValue = 0.2f, targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "glow"
    )

    val style = MaterialTheme.typography.displayLarge.copy(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        shadow = Shadow(
            color = MoonlightYellow.copy(alpha = glowAlpha),
            offset = Offset.Zero,
            blurRadius = 32f
        )
    )

    Text(text = text, modifier = modifier, style = style, color = MoonlightYellow)
}
