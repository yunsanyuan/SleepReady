package com.example.sleepready.ui.schedule.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import com.example.sleepready.domain.model.CustomRepeatUnit
import com.example.sleepready.domain.model.RepeatRule

/**
 * 重复规则的可编辑配置。
 *
 * @param rule 主规则
 * @param customInterval 自定义间隔（仅 CUSTOM）
 * @param customUnit 自定义单位（仅 CUSTOM）
 * @param weekdaysMask 周几位掩码 bit0=周一 … bit6=周日（仅 WEEKLY 或 CUSTOM(WEEK)）
 * @param monthlyDay 每月几号（仅 MONTHLY 或 CUSTOM(MONTH)）；null = 跟随起始日
 */
@Immutable
data class RepeatRuleConfig(
    val rule: RepeatRule = RepeatRule.NEVER,
    val customInterval: String = "1",
    val customUnit: CustomRepeatUnit = CustomRepeatUnit.DAY,
    val weekdaysMask: Int = 0,
    val monthlyDay: String = ""
)

@Composable
fun RepeatRuleEditor(
    config: RepeatRuleConfig,
    onChange: (RepeatRuleConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("重复", style = MaterialTheme.typography.titleSmall)
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RepeatRule.values().forEach { r ->
                    FilterChip(
                        selected = config.rule == r,
                        onClick = { onChange(config.copy(rule = r)) },
                        label = { Text(r.displayName) }
                    )
                }
            }

            // 周选项（WEEKLY 或 CUSTOM(WEEK)）
            val showWeekdays = config.rule == RepeatRule.WEEKLY ||
                (config.rule == RepeatRule.CUSTOM && config.customUnit == CustomRepeatUnit.WEEK)
            if (showWeekdays) {
                Text("选择重复的周", style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val labels = listOf("一", "二", "三", "四", "五", "六", "日")
                    labels.forEachIndexed { idx, label ->
                        WeekDayDot(
                            label = label,
                            selected = (config.weekdaysMask shr idx) and 1 == 1
                        ) {
                            val bit = 1 shl idx
                            val newMask = config.weekdaysMask xor bit
                            onChange(config.copy(weekdaysMask = newMask))
                        }
                    }
                }
            }

            // 月选项（MONTHLY 或 CUSTOM(MONTH)）
            val showMonthlyDay = config.rule == RepeatRule.MONTHLY ||
                (config.rule == RepeatRule.CUSTOM && config.customUnit == CustomRepeatUnit.MONTH)
            if (showMonthlyDay) {
                NumberStepperField(
                    label = "每月第几日（留空 = 跟随起始日）",
                    value = config.monthlyDay,
                    onValueChange = {
                        onChange(config.copy(monthlyDay = it.filterDigits().take(2)))
                    },
                    range = 1..31
                )
            }

            // CUSTOM 时显示「每 N 单位」
            if (config.rule == RepeatRule.CUSTOM) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = config.customInterval,
                        onValueChange = {
                            onChange(config.copy(customInterval = it.filterDigits().take(3)))
                        },
                        label = { Text("每隔") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Row(
                        modifier = Modifier
                            .weight(2f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CustomRepeatUnit.values().forEach { u ->
                            FilterChip(
                                selected = config.customUnit == u,
                                onClick = { onChange(config.copy(customUnit = u)) },
                                label = { Text(u.displayName) }
                            )
                        }
                    }
                }
                Text(
                    text = customSummary(config),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun customSummary(config: RepeatRuleConfig): String {
    val n = config.customInterval.toIntOrNull() ?: return ""
    return "每 $n ${config.customUnit.displayName}"
}

@Composable
private fun WeekDayDot(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape),
        color = bg,
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(label, color = fg, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun TextLinkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(onClick = onClick, modifier = modifier) { Text(text) }
}
