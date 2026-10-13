package com.example.sleepready.ui.schedule.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.sleepready.domain.model.LunarCalendar
import com.example.sleepready.ui.common.DateTimeWheelSheet
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale

/**
 * 日期/时间字段的不可变快照。年月日时分均用 String，便于展示「未填」状态；
 * 上层根据需要决定哪些字段必填。
 */
@Immutable
data class DateTimeFields(
    val year: String = "",
    val month: String = "",
    val day: String = "",
    val hour: String = "0",
    val minute: String = "0",
    val isLunar: Boolean = false,
    val lunarLeap: Boolean = false
) {
    fun toEpochMs(): Long? {
        val y = year.toIntOrNull() ?: return null
        val mo = month.toIntOrNull() ?: return null
        val d = day.toIntOrNull() ?: return null
        val h = hour.toIntOrNull() ?: 0
        val mi = minute.toIntOrNull() ?: 0
        if (mo !in 1..12 || d !in 1..31 || h !in 0..23 || mi !in 0..59) return null
        return if (isLunar) {
            val ms = LunarCalendar.lunarToSolarMillis(y, mo, d, lunarLeap, h, mi)
            if (ms == Long.MIN_VALUE) null else ms
        } else runCatching {
            GregorianCalendar().apply {
                isLenient = false
                clear()
                set(y, mo - 1, d, h, mi, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }.getOrNull()
    }

    fun hasFullDate(): Boolean =
        year.toIntOrNull() != null && month.toIntOrNull() != null && day.toIntOrNull() != null

    companion object {
        fun fromMillis(ms: Long?, isLunar: Boolean): DateTimeFields {
            if (ms == null) return DateTimeFields(isLunar = isLunar)
            return if (isLunar) {
                val cal = GregorianCalendar().apply { timeInMillis = ms }
                val ld = LunarCalendar.solarMillisToLunar(ms)
                if (ld != null) {
                    DateTimeFields(
                        year = ld.year.toString(),
                        month = ld.month.toString(),
                        day = ld.day.toString(),
                        hour = cal.get(Calendar.HOUR_OF_DAY).toString(),
                        minute = cal.get(Calendar.MINUTE).toString(),
                        isLunar = true,
                        lunarLeap = ld.isLeapMonth
                    )
                } else fromMillis(ms, isLunar = false).copy(isLunar = false)
            } else {
                val cal = GregorianCalendar().apply { timeInMillis = ms }
                DateTimeFields(
                    year = cal.get(Calendar.YEAR).toString(),
                    month = (cal.get(Calendar.MONTH) + 1).toString(),
                    day = cal.get(Calendar.DAY_OF_MONTH).toString(),
                    hour = cal.get(Calendar.HOUR_OF_DAY).toString(),
                    minute = cal.get(Calendar.MINUTE).toString(),
                    isLunar = false
                )
            }
        }
    }
}

/**
 * 通用「日期 + 时间」选择 Card。
 *
 *  - 公历：一个大按钮，点击弹底部滚轮 BottomSheet（[DateTimeWheelSheet]）
 *  - 农历：保留 [NumberStepperField]（农历的闰月、不规则天数让滚轮表达较复杂）
 *
 * 参数：
 *  - [showYear] = false 时年字段被隐藏（生日「无年份」模式）
 *  - [showTime] = false 时时分字段被隐藏（纯日期型 = 倒数日 / 纪念日）
 *  - [compact] = true 时把卡片折叠成「label + 时间预览 + 修改按钮」一行
 *    （日程的开始/结束时间字段使用，避免占用过多垂直空间）
 */
@Composable
fun DateTimePickerCard(
    label: String,
    fields: DateTimeFields,
    onChange: (DateTimeFields) -> Unit,
    modifier: Modifier = Modifier,
    showYear: Boolean = true,
    showTime: Boolean = true,
    allowLunarLeap: Boolean = true,
    compact: Boolean = false
) {
    var showSheet by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(if (compact) 14.dp else 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        if (compact && !fields.isLunar) {
            // 单行紧凑形态：label + 时间预览 + 修改按钮 + 历法切换 chip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, style = MaterialTheme.typography.labelMedium)
                    Text(
                        text = previewText(fields, showYear, showTime),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                FilterChip(
                    selected = false,
                    onClick = { onChange(fields.copy(isLunar = true)) },
                    label = { Text("农历") }
                )
                OutlinedButton(onClick = { showSheet = true }) {
                    Text(if (fields.hasFullDate()) "修改" else "选择")
                }
            }
        } else {
            // 标准形态（含农历或非紧凑）
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(label, style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = !fields.isLunar,
                        onClick = { onChange(fields.copy(isLunar = false, lunarLeap = false)) },
                        label = { Text("公历") }
                    )
                    FilterChip(
                        selected = fields.isLunar,
                        onClick = { onChange(fields.copy(isLunar = true)) },
                        label = { Text("农历") }
                    )
                    if (allowLunarLeap && fields.isLunar) {
                        FilterChip(
                            selected = fields.lunarLeap,
                            onClick = { onChange(fields.copy(lunarLeap = !fields.lunarLeap)) },
                            label = { Text(if (fields.lunarLeap) "闰月" else "非闰") }
                        )
                    }
                }

                if (!fields.isLunar) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            Text(
                                text = previewText(fields, showYear, showTime),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        OutlinedButton(onClick = { showSheet = true }) {
                            Text(if (fields.hasFullDate()) "修改" else "选择")
                        }
                    }
                } else {
                    // 农历：保留 stepper
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (showYear) {
                            NumberStepperField(
                                label = "年", value = fields.year,
                                onValueChange = { onChange(fields.copy(year = it.filterDigits().take(4))) },
                                range = 1900..2100,
                                modifier = Modifier.weight(1.2f)
                            )
                        }
                        NumberStepperField(
                            label = "月", value = fields.month,
                            onValueChange = { onChange(fields.copy(month = it.filterDigits().take(2))) },
                            range = 1..12,
                            modifier = Modifier.weight(1f)
                        )
                        NumberStepperField(
                            label = "日", value = fields.day,
                            onValueChange = { onChange(fields.copy(day = it.filterDigits().take(2))) },
                            range = 1..30,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (showTime) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            NumberStepperField(
                                label = "时", value = fields.hour,
                                onValueChange = { onChange(fields.copy(hour = it.filterDigits().take(2))) },
                                range = 0..23,
                                modifier = Modifier.weight(1f)
                            )
                            NumberStepperField(
                                label = "分", value = fields.minute,
                                onValueChange = { onChange(fields.copy(minute = it.filterDigits().take(2))) },
                                range = 0..59,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    Text(
                        text = previewText(fields, showYear, showTime),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (showSheet) {
        DateTimeWheelSheet(
            initialMillis = fields.toEpochMs(),
            showTime = showTime,
            showYear = showYear,
            onConfirm = { ms ->
                showSheet = false
                onChange(DateTimeFields.fromMillis(ms, isLunar = false))
            },
            onDismiss = { showSheet = false }
        )
    }
}

private fun previewText(
    fields: DateTimeFields,
    showYear: Boolean,
    showTime: Boolean
): String {
    if (!fields.hasFullDate() && showYear) return "未设置"
    // 当年份隐藏时，临时填入当前年用于预览
    val effective = if (showYear) fields else fields.copy(
        year = GregorianCalendar().get(Calendar.YEAR).toString()
    )
    val ms = effective.toEpochMs() ?: return "时间无效"
    val pattern = if (showTime) "yyyy-MM-dd HH:mm" else "yyyy-MM-dd"
    val solar = SimpleDateFormat(pattern, Locale.getDefault()).format(Date(ms))
    return if (fields.isLunar) {
        val ld = LunarCalendar.solarMillisToLunar(ms)
        val lunarText = ld?.let {
            (if (showYear) "${it.year} " else "") + LunarCalendar.format(it)
        } ?: "—"
        "农历 $lunarText · 公历 $solar"
    } else {
        if (showYear) "公历 $solar" else "公历 ${solar.substring(5)}"
    }
}

/** 数字步进字段：上下按钮 + 直接输入（仍用于农历输入）。 */
@Composable
fun NumberStepperField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    range: IntRange,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Column {
            IconButton(onClick = {
                val cur = value.toIntOrNull()
                val next = if (cur == null) range.first else (cur + 1).coerceIn(range)
                onValueChange(next.toString())
            }) { Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "+") }
            IconButton(onClick = {
                val cur = value.toIntOrNull()
                val next = if (cur == null) range.first else (cur - 1).coerceIn(range)
                onValueChange(next.toString())
            }) { Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "−") }
        }
    }
}

internal fun String.filterDigits(): String = filter { it.isDigit() }
