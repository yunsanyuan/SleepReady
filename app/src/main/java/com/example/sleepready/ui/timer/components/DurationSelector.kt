package com.example.sleepready.ui.timer.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sleepready.domain.model.TimerConfig
import com.example.sleepready.ui.theme.MoonlightYellow
import com.example.sleepready.ui.theme.WarmOrange
import kotlin.math.roundToInt

/**
 * 入梦时长选择器（v2，最长 24 小时）。
 *
 *  1. 大数字读出：H 小时 M 分钟（仅当 H>0 显示小时部分）
 *  2. 上 slider：小时 0..24
 *  3. 下 slider：分钟 0..59
 *  4. 快捷 chips：15 分钟 / 30 分钟 / 45 分钟 / 1 小时 / 2 小时 / 8 小时
 *
 * 注意：当小时已经是 24 时，分钟会被限制为 0（避免超出上限）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DurationSelector(
    selectedMinutes: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val total = TimerConfig.clampMinutes(selectedMinutes)
    val hours = total / 60
    val minutes = total % 60

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // -- 大数字读出 ---------------------------------------------------
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (hours > 0) {
                Text(
                    text = hours.toString(),
                    color = MoonlightYellow,
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 56.sp
                    )
                )
                Text(
                    text = "时",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }
            Text(
                text = minutes.toString(),
                color = MoonlightYellow,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 56.sp
                )
            )
            Text(
                text = "分",
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 10.dp)
            )
        }

        // -- 快捷 chips（横向滚动避免 6 个 chip 挤不下） -------------------
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TimerConfig.PRESET_DURATIONS.forEach { presetMin ->
                FilterChip(
                    selected = presetMin == total,
                    onClick = { onSelect(presetMin) },
                    label = { Text(formatPreset(presetMin)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = WarmOrange,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        // -- 小时 slider --------------------------------------------------
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "小时：$hours",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = hours.toFloat(),
                onValueChange = { newH ->
                    val h = newH.roundToInt().coerceIn(0, 24)
                    val newTotal = if (h == 24) 24 * 60 else h * 60 + minutes
                    onSelect(newTotal.coerceAtLeast(TimerConfig.MIN_DURATION_MINUTES))
                },
                valueRange = 0f..24f,
                steps = 23, // 24 个分段（0..24 共 25 点）
                colors = SliderDefaults.colors(
                    thumbColor = WarmOrange,
                    activeTrackColor = WarmOrange,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        // -- 分钟 slider --------------------------------------------------
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "分钟：$minutes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = minutes.toFloat(),
                onValueChange = { newM ->
                    val m = newM.roundToInt().coerceIn(0, 59)
                    // 24 小时已是上限，分钟必须保持 0
                    val newTotal = if (hours >= 24) 24 * 60 else hours * 60 + m
                    onSelect(newTotal.coerceAtLeast(TimerConfig.MIN_DURATION_MINUTES))
                },
                valueRange = 0f..59f,
                steps = 58,
                enabled = hours < 24,
                colors = SliderDefaults.colors(
                    thumbColor = WarmOrange,
                    activeTrackColor = WarmOrange,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier.fillMaxWidth()
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "至少 ${TimerConfig.MIN_DURATION_MINUTES} 分钟",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "最长 24 小时",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun formatPreset(minutes: Int): String = when {
    minutes >= 60 && minutes % 60 == 0 -> "${minutes / 60} 小时"
    minutes >= 60 -> "${minutes / 60} 小时 ${minutes % 60} 分钟"
    else -> "$minutes 分钟"
}
