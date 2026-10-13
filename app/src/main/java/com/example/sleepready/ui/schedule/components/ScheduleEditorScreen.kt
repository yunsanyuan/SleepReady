package com.example.sleepready.ui.schedule.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sleepready.domain.model.CustomRepeatUnit
import com.example.sleepready.domain.model.RepeatRule
import com.example.sleepready.domain.model.Schedule
import com.example.sleepready.domain.model.ScheduleType
import com.example.sleepready.permission.ReminderPermissionChecker
import com.example.sleepready.permission.ReminderPermissionState
import com.example.sleepready.ui.common.CalendarPickerSheet
import com.example.sleepready.ui.common.formatDateTimeLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleEditorScreen(
    initial: Schedule?,
    type: ScheduleType,
    onBack: () -> Unit,
    onSave: (Schedule) -> Unit
) {
    val context = LocalContext.current
    val state = remember(initial, type) { EditorState.from(initial, type) }
    var draft by remember { mutableStateOf(state) }
    var hasDateError by remember { mutableStateOf(false) }
    var showStartSheet by remember { mutableStateOf(false) }
    var showEndSheet by remember { mutableStateOf(false) }

    val schedule = draft.toSchedule(initial)
    // 只要求日期非空，允许早于现在
    val dateValid = schedule.targetTimeMillis != null
    val canSave = draft.title.isNotBlank() && dateValid
    val permStatus = remember { ReminderPermissionChecker.detectAll(context) }

    // 日历选择器弹窗
    if (showStartSheet) CalendarPickerSheet(
        initialMillis = draft.start.toEpochMs(),
        showTime = type != ScheduleType.BIRTHDAY && type != ScheduleType.ANNIVERSARY
                && type != ScheduleType.COUNTDOWN && !draft.isAllDay,
        showYear = when (type) { ScheduleType.BIRTHDAY -> draft.hasYear; else -> true },
        onConfirm = { ms ->
            showStartSheet = false
            draft = draft.copy(start = DateTimeFields.fromMillis(ms, isLunar = false))
            hasDateError = false
        },
        onDismiss = { showStartSheet = false }
    )
    if (showEndSheet) CalendarPickerSheet(
        initialMillis = draft.end.toEpochMs(),
        showTime = !draft.isAllDay,
        onConfirm = { ms ->
            showEndSheet = false
            draft = draft.copy(end = DateTimeFields.fromMillis(ms, isLunar = false))
        },
        onDismiss = { showEndSheet = false }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initial == null) "新建${type.displayName}" else "编辑${type.displayName}", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = {
                    TextButton(
                        onClick = { if (!dateValid) hasDateError = true; else onSave(schedule) },
                        enabled = draft.title.isNotBlank()
                    ) {
                        Text("保存", fontWeight = FontWeight.Bold,
                            color = if (canSave) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题
            OutlinedTextField(
                value = draft.title, onValueChange = { draft = draft.copy(title = it); hasDateError = false },
                label = { Text(if (type == ScheduleType.BIRTHDAY) "寿星姓名" else "标题 *") },
                placeholder = { Text("输入标题") }, modifier = Modifier.fillMaxWidth(),
                singleLine = true, shape = RoundedCornerShape(12.dp)
            )

            // 日期时间卡片
            Card(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (!dateValid && hasDateError) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                ),
                border = if (!dateValid && hasDateError) androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.error) else null
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.AccessTime, null, tint = if (dateValid) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("日期时间 *", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(12.dp))

                    when (type) {
                        ScheduleType.EVENT -> {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("全天事件", style = MaterialTheme.typography.bodyMedium)
                                Switch(checked = draft.isAllDay, onCheckedChange = { draft = draft.copy(isAllDay = it) })
                            }
                            Spacer(Modifier.height(10.dp))
                            ClickDateRow("开始时间", draft.start.toEpochMs(), !draft.isAllDay) { showStartSheet = true }
                            Spacer(Modifier.height(6.dp))
                            ClickDateRow("结束时间", draft.end.toEpochMs(), !draft.isAllDay) { showEndSheet = true }
                        }
                        ScheduleType.BIRTHDAY -> {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("不指定年份", style = MaterialTheme.typography.bodyMedium)
                                Switch(checked = !draft.hasYear, onCheckedChange = { draft = draft.copy(hasYear = !it) })
                            }
                            Spacer(Modifier.height(10.dp))
                            ClickDateRow("生日", draft.start.toEpochMs(), false) { showStartSheet = true }
                        }
                        ScheduleType.ANNIVERSARY -> ClickDateRow("纪念日", draft.start.toEpochMs(), false) { showStartSheet = true }
                        ScheduleType.COUNTDOWN -> ClickDateRow("目标日期", draft.start.toEpochMs(), false) { showStartSheet = true }
                    }

                    AnimatedVisibility(!dateValid && hasDateError, enter = fadeIn(), exit = fadeOut()) {
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp).clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ErrorOutline, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("请点击选择日期", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                        }
                    }
                    if (dateValid) {
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(formatTarget(schedule.targetTimeMillis!!), style = MaterialTheme.typography.bodySmall, color = Color(0xFF4CAF50))
                        }
                    }
                }
            }

            // 地点
            OutlinedTextField(value = draft.location, onValueChange = { draft = draft.copy(location = it) },
                label = { Text("地点（可选）") }, modifier = Modifier.fillMaxWidth(), singleLine = true, shape = RoundedCornerShape(12.dp))

            // 优先级
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
                Column(Modifier.padding(16.dp)) {
                    Text("优先级", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(when { draft.priority <= 3 -> "高" ; draft.priority <= 7 -> "中" ; else -> "低" },
                            style = MaterialTheme.typography.bodyMedium,
                            color = when { draft.priority <= 3 -> Color(0xFFE53935) ; draft.priority <= 7 -> Color(0xFFFB8C00) ; else -> Color(0xFF43A047) })
                        Text("${draft.priority}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Slider(value = draft.priority.toFloat(), onValueChange = { draft = draft.copy(priority = it.roundToInt()) },
                        valueRange = Schedule.MIN_PRIORITY.toFloat()..Schedule.MAX_PRIORITY.toFloat(),
                        steps = Schedule.MAX_PRIORITY - Schedule.MIN_PRIORITY - 1)
                }
            }

            // 重复
            RepeatRuleEditor(config = draft.repeat, onChange = { draft = draft.copy(repeat = it) })

            // 提醒
            ReminderListEditor(offsetsMinutes = draft.reminderOffsetsMinutes, onChange = { draft = draft.copy(reminderOffsetsMinutes = it) })

            // 闹钟
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (draft.useAlarmRing)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))) {
                Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Filled.NotificationsActive, null, tint = if (draft.useAlarmRing) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("闹钟强提醒", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text("到点全屏循环响铃 + 震动", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Switch(checked = draft.useAlarmRing, onCheckedChange = { draft = draft.copy(useAlarmRing = it) })
                }
            }

            // 权限
            PermissionRow(permStatus, draft.useAlarmRing)

            // 备注
            OutlinedTextField(value = draft.note, onValueChange = { draft = draft.copy(note = it) },
                label = { Text("备注（可选）") }, modifier = Modifier.fillMaxWidth(), minLines = 2, shape = RoundedCornerShape(12.dp))

            Spacer(Modifier.height(80.dp))
        }
    }
}

// ==== 点击式日期行 ====
@Composable
private fun ClickDateRow(label: String, ms: Long?, showTime: Boolean, onClick: () -> Unit) {
    val display = if (ms != null) formatDateTimeLabel(ms, showTime) else "点击选择"
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
            .then(if (ms != null) Modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f))
                  else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surface))
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(2.dp))
            Text(display, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = if (ms != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
        }
        Icon(Icons.Filled.CalendarMonth, "选择", tint = if (ms != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error.copy(alpha = 0.6f), modifier = Modifier.size(24.dp))
    }
}

// ==== 权限 ====
@Composable
private fun PermissionRow(status: com.example.sleepready.permission.PermissionStatus, usesAlarmRing: Boolean) {
    val items = buildList<Pair<String, Boolean>> {
        add("通知" to (status.postNotifications is ReminderPermissionState.Granted))
        add("精确闹钟" to (status.scheduleExactAlarm is ReminderPermissionState.Granted))
        if (usesAlarmRing) add("全屏" to (status.useFullScreenIntent is ReminderPermissionState.Granted))
    }
    if (items.isEmpty()) return
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Security, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Text("权限", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items.forEach { (name, ok) ->
                    val c = if (ok) Color(0xFF4CAF50) else Color(0xFFFF9800)
                    Row(Modifier.clip(RoundedCornerShape(6.dp)).background(c.copy(alpha = 0.1f)).padding(horizontal = 6.dp, vertical = 3.dp)) {
                        Icon(if (ok) Icons.Filled.CheckCircle else Icons.Filled.WarningAmber, null, tint = c, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(name, style = MaterialTheme.typography.labelSmall, color = c, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// ==== 工具 ====
private fun formatTarget(ms: Long): String {
    val diff = ms - System.currentTimeMillis()
    val date = SimpleDateFormat("M月d日 HH:mm", Locale.CHINESE).format(Date(ms))
    val wd = SimpleDateFormat("EEEE", Locale.CHINESE).format(Date(ms))
    return when {
        diff < -86400000 -> "$date $wd（${-diff / 86400000}天前）"
        diff < -3600000 -> "$date $wd（${-diff / 3600000}小时前）"
        diff < 0 -> "$date $wd（已过）"
        diff < 60000 -> "$date $wd（现在）"
        diff < 3600000 -> "$date $wd（${diff / 60000}分钟后）"
        diff < 86400000 -> "$date $wd（${diff / 3600000}小时后）"
        else -> "$date $wd（${diff / 86400000}天后）"
    }
}

// ==== EditorState ====
private data class EditorState(
    val type: ScheduleType, val title: String, val start: DateTimeFields, val end: DateTimeFields,
    val isAllDay: Boolean, val hasYear: Boolean, val location: String, val priority: Int,
    val repeat: RepeatRuleConfig, val reminderOffsetsMinutes: List<Long>, val useAlarmRing: Boolean, val note: String
) {
    fun toSchedule(initial: Schedule?): Schedule {
        val startMs = start.toEpochMs()
        return Schedule(
            id = initial?.id ?: 0L, title = title.trim(), type = type,
            priority = priority.coerceIn(Schedule.MIN_PRIORITY, Schedule.MAX_PRIORITY),
            targetTimeMillis = startMs,
            endTimeMillis = if (type == ScheduleType.EVENT) end.toEpochMs() else null,
            isAllDay = type == ScheduleType.EVENT && isAllDay,
            targetIsLunar = start.isLunar && startMs != null,
            hasYear = if (type == ScheduleType.BIRTHDAY) hasYear else true,
            location = location.takeIf { it.isNotBlank() }?.trim(),
            repeatRule = repeat.rule,
            customRepeatInterval = if (repeat.rule == RepeatRule.CUSTOM) repeat.customInterval.toIntOrNull()?.coerceAtLeast(1) ?: 1 else null,
            customRepeatUnit = if (repeat.rule == RepeatRule.CUSTOM) repeat.customUnit else null,
            weekdaysMask = repeat.weekdaysMask, monthlyDay = repeat.monthlyDay.toIntOrNull(),
            reminderOffsetsMinutes = reminderOffsetsMinutes, useAlarmRing = useAlarmRing,
            note = note.takeIf { it.isNotBlank() }?.trim(),
            createdAt = initial?.createdAt ?: 0L, updatedAt = 0L
        )
    }
    companion object {
        fun from(initial: Schedule?, type: ScheduleType): EditorState {
            val start = DateTimeFields.fromMillis(initial?.targetTimeMillis, initial?.targetIsLunar ?: false)
            val end = DateTimeFields.fromMillis(initial?.endTimeMillis, false)
            return EditorState(
                type = type, title = initial?.title.orEmpty(), start = start, end = end,
                isAllDay = initial?.isAllDay ?: false, hasYear = initial?.hasYear ?: true,
                location = initial?.location.orEmpty(), priority = initial?.priority ?: Schedule.DEFAULT_PRIORITY,
                repeat = RepeatRuleConfig(initial?.repeatRule ?: RepeatRule.NEVER, (initial?.customRepeatInterval ?: 1).toString(),
                    initial?.customRepeatUnit ?: CustomRepeatUnit.DAY, initial?.weekdaysMask ?: 0, initial?.monthlyDay?.toString().orEmpty()),
                reminderOffsetsMinutes = initial?.reminderOffsetsMinutes ?: Schedule.DEFAULT_REMINDER_OFFSETS_MINUTES,
                useAlarmRing = initial?.useAlarmRing ?: false, note = initial?.note.orEmpty()
            )
        }
    }
}
