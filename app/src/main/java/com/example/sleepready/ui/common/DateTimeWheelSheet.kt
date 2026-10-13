package com.example.sleepready.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale

/**
 * 公历日期时间选择器 — v2 优化版。
 * 分两步：先选日期（年月日三列大滚轮），再选时间（时分两列大滚轮）。
 * 每列约占总宽 1/3（日期）或 1/2（时间），触控区域充足。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeWheelSheet(
    initialMillis: Long?,
    showTime: Boolean,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit,
    showYear: Boolean = true
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // 0 = 日期页, 1 = 时间页
    var page by remember { mutableStateOf(0) }

    val initMs = initialMillis ?: System.currentTimeMillis()
    val initCal = remember {
        GregorianCalendar().apply { timeInMillis = initMs }
    }

    var year by remember { mutableStateOf(initCal.get(Calendar.YEAR)) }
    var month by remember { mutableStateOf(initCal.get(Calendar.MONTH) + 1) }
    var day by remember { mutableStateOf(initCal.get(Calendar.DAY_OF_MONTH)) }
    var hour by remember { mutableStateOf(initCal.get(Calendar.HOUR_OF_DAY)) }
    var minute by remember { mutableStateOf(initCal.get(Calendar.MINUTE)) }

    // 年份范围
    val years = remember { (1940..2099).toList() }
    val months = remember { (1..12).toList() }
    val daysInMonth = remember(year, month) {
        GregorianCalendar(year, month - 1, 1).getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    val days = remember(daysInMonth) { (1..daysInMonth).toList() }
    val hours = remember { (0..23).toList() }
    val minutes = remember { (0..59).toList() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ---- 标题 & 标签切换 ----
            if (showTime) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilterChip(
                        selected = page == 0,
                        onClick = { page = 0 },
                        label = { Text("选择日期") }
                    )
                    Spacer(Modifier.width(12.dp))
                    FilterChip(
                        selected = page == 1,
                        onClick = { page = 1 },
                        label = {
                            Text(
                                "${hour.toString().padStart(2, '0')}:${
                                    minute.toString().padStart(2, '0')
                                }"
                            )
                        }
                    )
                }
            }

            // 当前选中预览
            Spacer(Modifier.height(6.dp))
            Text(
                buildPreview(year, month, day, hour, minute, showTime, showYear),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))

            // ---- 滚轮区 ----
            if (page == 0) {
                // 日期：年月日三列
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (showYear) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Text("年", style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            WheelPicker(
                                items = years,
                                selectedIndex = (year - years.first()).coerceIn(0, years.lastIndex),
                                onSelectedChange = { idx -> year = years[idx] },
                                itemFormatter = { "$it" }
                            )
                        }
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("月", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        WheelPicker(
                            items = months,
                            selectedIndex = (month - 1).coerceIn(0, months.lastIndex),
                            onSelectedChange = { idx ->
                                month = months[idx]
                                val max = GregorianCalendar(year, month - 1, 1)
                                    .getActualMaximum(Calendar.DAY_OF_MONTH)
                                if (day > max) day = max
                            },
                            itemFormatter = { "${it}月" }
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("日", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        WheelPicker(
                            items = days,
                            selectedIndex = (day - 1).coerceIn(0, days.lastIndex),
                            onSelectedChange = { idx -> day = days[idx] },
                            itemFormatter = { "${it}日" }
                        )
                    }
                }
            } else {
                // 时间：时分两列
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(120.dp)
                    ) {
                        Text("时", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        WheelPicker(
                            items = hours,
                            selectedIndex = hour.coerceIn(0, 23),
                            onSelectedChange = { idx -> hour = hours[idx] },
                            itemFormatter = { it.toString().padStart(2, '0') },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(Modifier.width(24.dp))
                    Text(
                        ":",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 28.dp)
                    )
                    Spacer(Modifier.width(24.dp))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(120.dp)
                    ) {
                        Text("分", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        WheelPicker(
                            items = minutes,
                            selectedIndex = minute.coerceIn(0, 59),
                            onSelectedChange = { idx -> minute = minutes[idx] },
                            itemFormatter = { it.toString().padStart(2, '0') },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ---- 按钮 ----
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val onNextOrConfirm: () -> Unit = {
                    if (showTime && page == 0) {
                        page = 1
                    } else {
                        val cal = GregorianCalendar().apply {
                            clear()
                            set(year, month - 1, day,
                                if (showTime) hour else 0,
                                if (showTime) minute else 0, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        onConfirm(cal.timeInMillis)
                    }
                }

                Button(
                    onClick = { onDismiss() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) { Text("取消") }

                Button(
                    onClick = onNextOrConfirm,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        if (showTime && page == 0) "下一步" else "确定",
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun buildPreview(
    year: Int, month: Int, day: Int,
    hour: Int, minute: Int,
    showTime: Boolean, showYear: Boolean
): String {
    val cal = GregorianCalendar(year, month - 1, day, hour, minute, 0)
    cal.set(Calendar.MILLISECOND, 0)
    val ms = cal.timeInMillis
    val dateFmt = if (showYear) "yyyy年M月d日" else "M月d日"
    val datePart = SimpleDateFormat(dateFmt, Locale.CHINESE).format(Date(ms))
    val weekday = SimpleDateFormat("EEEE", Locale.CHINESE).format(Date(ms))
    return if (showTime) {
        "$datePart $weekday ${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"
    } else {
        "$datePart $weekday"
    }
}

fun formatDateTimeLabel(ms: Long?, showTime: Boolean): String {
    if (ms == null) return "未设置"
    val cal = GregorianCalendar().apply { timeInMillis = ms }
    val y = cal.get(Calendar.YEAR)
    val m = cal.get(Calendar.MONTH) + 1
    val d = cal.get(Calendar.DAY_OF_MONTH)
    val h = cal.get(Calendar.HOUR_OF_DAY)
    val mi = cal.get(Calendar.MINUTE)
    return if (showTime) {
        "%04d年%02d月%02d日 %02d:%02d".format(y, m, d, h, mi)
    } else {
        "%04d年%02d月%02d日".format(y, m, d)
    }
}
