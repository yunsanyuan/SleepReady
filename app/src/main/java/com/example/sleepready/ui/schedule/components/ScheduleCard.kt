package com.example.sleepready.ui.schedule.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.Cake
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sleepready.domain.model.LunarCalendar
import com.example.sleepready.domain.model.Schedule
import com.example.sleepready.domain.model.ScheduleType
import com.example.sleepready.ui.schedule.ScheduleSummaries
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScheduleCard(
    schedule: Schedule,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val now = System.currentTimeMillis()
    val targetMs = schedule.targetTimeMillis
    // 生日、纪念日不过期（每年循环），日程、倒数日可过期
    val canExpire = schedule.type == ScheduleType.EVENT || schedule.type == ScheduleType.COUNTDOWN
    val isExpired = canExpire && targetMs != null && targetMs < now
    val isSoon = targetMs != null && targetMs in now..(now + 3600_000)

    val cardAlpha = if (isExpired) 0.55f else 1f
    val alphaAnim by animateFloatAsState(cardAlpha, tween(300), label = "cardAlpha")

    Card(
        modifier = modifier
            .fillMaxWidth()
            .alpha(alphaAnim)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 图标 + 状态点
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = iconFor(schedule.type),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = if (isExpired) 0.4f else 0.8f),
                    modifier = Modifier.size(26.dp)
                )
                Spacer(Modifier.size(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (schedule.reminderOffsetsMinutes.isNotEmpty())
                        DotIndicator(Color(0xFF8ECB8E))
                    if (schedule.useAlarmRing)
                        DotIndicator(Color(0xFFF4C27A))
                }
            }

            // 正文
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(schedule.title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                        modifier = Modifier.weight(1f, fill = false))
                    if (isSoon && !isExpired) {
                        Spacer(Modifier.width(8.dp))
                        Badge("即将", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.primary)
                    }
                    if (isExpired)
                        Badge("已过期", MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                }
                Text(secondaryLine(schedule), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                if (!schedule.location.isNullOrBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Place, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
                        Spacer(Modifier.size(2.dp))
                        Text(schedule.location, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), maxLines = 1)
                    }
                }
            }

            // 右侧数字
            ScheduleSummaries.headlineFor(schedule)?.let {
                Text(it, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, fontSize = 18.sp),
                    color = if (isSoon) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary.copy(alpha = if (isExpired) 0.4f else 0.8f))
            }
        }
    }
}

@Composable
private fun DotIndicator(color: Color) {
    Box(
        Modifier
            .size(7.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.8f))
    )
}

@Composable
private fun Badge(text: String, bg: Color, fg: Color) {
    Text(text, style = MaterialTheme.typography.labelSmall, color = fg,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(bg.copy(alpha = 0.5f))
            .padding(horizontal = 6.dp, vertical = 2.dp))
}

private fun secondaryLine(s: Schedule): String {
    val label = s.type.displayName
    val time = when (s.type) {
        ScheduleType.EVENT -> formatEvent(s)
        else -> formatDateOnly(s)
    } ?: "未设置时间"
    return "$label · $time"
}

private fun formatEvent(s: Schedule): String? {
    val start = s.targetTimeMillis ?: return null
    val fmt = if (s.isAllDay) SimpleDateFormat("MM-dd", Locale.getDefault())
    else SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    val startText = fmt.format(Date(start))
    val endText = s.endTimeMillis?.let { fmt.format(Date(it)) }
    return when {
        s.isAllDay && endText != null && endText != startText -> "$startText–$endText"
        endText != null -> "$startText – $endText"
        else -> startText
    }
}

private fun formatDateOnly(s: Schedule): String? {
    val ms = s.targetTimeMillis ?: return null
    val solar = SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(ms))
    if (!s.targetIsLunar) return solar
    val lunar = LunarCalendar.solarMillisToLunar(ms) ?: return solar
    return "农历${LunarCalendar.format(lunar)}"
}

private fun iconFor(t: ScheduleType) = when (t) {
    ScheduleType.EVENT -> Icons.Outlined.CalendarMonth
    ScheduleType.BIRTHDAY -> Icons.Outlined.Cake
    ScheduleType.ANNIVERSARY -> Icons.Outlined.FavoriteBorder
    ScheduleType.COUNTDOWN -> Icons.Outlined.Timer
}
