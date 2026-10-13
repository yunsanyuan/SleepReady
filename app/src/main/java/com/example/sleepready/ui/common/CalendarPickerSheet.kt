package com.example.sleepready.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.sleepready.domain.model.LunarCalendar
import java.util.Calendar
import java.util.Date
import java.util.GregorianCalendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarPickerSheet(
    initialMillis: Long?,
    showTime: Boolean,
    showYear: Boolean = true,
    onConfirm: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val now = System.currentTimeMillis()
    var selectedMs by remember { mutableLongStateOf(initialMillis ?: now) }

    // 公历字段
    fun readSolar() = GregorianCalendar().apply { timeInMillis = selectedMs }
    fun sy() = readSolar().get(Calendar.YEAR)
    fun sm() = readSolar().get(Calendar.MONTH) + 1
    fun sd() = readSolar().get(Calendar.DAY_OF_MONTH)
    fun sh() = readSolar().get(Calendar.HOUR_OF_DAY)
    fun smi() = readSolar().get(Calendar.MINUTE)

    // 农历字段
    fun readLunar(): Triple<Int, Int, Int>? {
        val ld = LunarCalendar.solarMillisToLunar(selectedMs) ?: return null
        return Triple(ld.year, ld.month, ld.day)
    }

    var sYear by remember { mutableIntStateOf(sy()) }
    var sMonth by remember { mutableIntStateOf(sm()) }
    var sDay by remember { mutableIntStateOf(sd()) }
    var hour by remember { mutableIntStateOf(sh()) }
    var minute by remember { mutableIntStateOf(smi()) }

    val initL = readLunar() ?: Triple(2024, 1, 1)
    var lunarYear by remember { mutableIntStateOf(initL.first) }
    var lunarMonth by remember { mutableIntStateOf(initL.second) }
    var lunarDay by remember { mutableIntStateOf(initL.third) }
    var lunarLeap by remember { mutableStateOf(false) }

    var isLunar by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 更新 selectedMs（从公历字段）
    fun updateMs() {
        val c = GregorianCalendar()
        c.set(Calendar.YEAR, sYear)
        c.set(Calendar.MONTH, sMonth - 1)
        c.set(Calendar.DAY_OF_MONTH, sDay)
        c.set(Calendar.HOUR_OF_DAY, if (showTime) hour else 0)
        c.set(Calendar.MINUTE, if (showTime) minute else 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        selectedMs = c.timeInMillis
    }

    // 公历滚轮变化时
    fun onSolarChanged() {
        updateMs()
        val l = readLunar()
        if (l != null) { lunarYear = l.first; lunarMonth = l.second; lunarDay = l.third }
    }

    // 农历滚轮变化时
    fun onLunarChanged() {
        val ms = LunarCalendar.lunarToSolarMillis(lunarYear, lunarMonth, lunarDay, lunarLeap, 0, 0)
        if (ms != Long.MIN_VALUE) {
            selectedMs = ms
            sYear = sy(); sMonth = sm(); sDay = sd()
        }
    }

    // 切换历法时同步
    LaunchedEffect(isLunar) {
        if (isLunar) {
            val l = readLunar()
            if (l != null) { lunarYear = l.first; lunarMonth = l.second; lunarDay = l.third }
            val ld = LunarCalendar.solarMillisToLunar(selectedMs)
            lunarLeap = ld?.isLeapMonth == true
        } else {
            onLunarChanged()
        }
    }

    // 数据
    val years = remember { (1940..2099).toList() }
    val months = remember { (1..12).toList() }
    val daysInMonth = remember(sYear, sMonth) {
        GregorianCalendar(sYear, sMonth - 1, 1).getActualMaximum(Calendar.DAY_OF_MONTH)
    }
    val days = remember(daysInMonth) { (1..daysInMonth).toList() }
    val hs = remember { (0..23).toList() }
    val mins = remember { (0..59).toList() }
    val lyY = remember { (1940..2099).toList() }
    val lyM = remember { (1..12).toList() }
    val lyD = remember { (1..30).toList() }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {

            // 历法切换
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                FilterChip(selected = !isLunar, onClick = { isLunar = false }, label = { Text("公历") })
                Spacer(Modifier.padding(horizontal = 8.dp))
                FilterChip(selected = isLunar, onClick = { isLunar = true }, label = { Text("农历") })
                if (isLunar) {
                    Spacer(Modifier.padding(horizontal = 4.dp))
                    val leapM = LunarCalendar.leapMonth(lunarYear)
                    if (leapM > 0) {
                        FilterChip(
                            selected = lunarLeap,
                            onClick = { lunarLeap = !lunarLeap; onLunarChanged() },
                            label = { Text(if (lunarLeap) "闰月" else "非闰") }
                        )
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // 预览
            val preview = if (isLunar) {
                val mt = LunarCalendar.formatMonth(lunarMonth, lunarLeap)
                val dy = LunarCalendar.formatDay(lunarDay)
                val sm = LunarCalendar.lunarToSolarMillis(lunarYear, lunarMonth, lunarDay, lunarLeap, 0, 0)
                val sol = if (sm != Long.MIN_VALUE)
                    java.text.SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(sm)) else ""
                if (showYear) "农历${lunarYear}年$mt$dy · 公历$sol" else "农历$mt$dy · 公历$sol"
            } else {
                buildString {
                    if (showYear) append("${sYear}年")
                    append("${sMonth}月${sDay}日")
                    if (showTime) append(" ${hour.toString().padStart(2,'0')}:${minute.toString().padStart(2,'0')}")
                }
            }
            Text(preview, style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center)

            Spacer(Modifier.height(14.dp))

            // 滚轮
            if (isLunar) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    if (showYear) WheelCol("年", lyY, { idx -> lunarYear = lyY[idx]; onLunarChanged() }, { "$it" }, lunarYear, Modifier.weight(1.2f))
                    WheelCol("月", lyM, { idx -> lunarMonth = lyM[idx]; onLunarChanged() }, { LunarCalendar.formatMonth(it, false) }, lunarMonth, Modifier.weight(1f))
                    WheelCol("日", lyD, { idx -> lunarDay = lyD[idx]; onLunarChanged() }, { LunarCalendar.formatDay(it) }, lunarDay, Modifier.weight(1f))
                }
            } else if (showTime) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    WheelCol("日", days, { idx -> sDay = days[idx]; onSolarChanged() }, { "${it}日" }, sDay, Modifier.weight(1f))
                    WheelCol("时", hs, { idx -> hour = hs[idx]; onSolarChanged() }, { it.toString().padStart(2, '0') }, hour, Modifier.weight(1f))
                    WheelCol("分", mins, { idx -> minute = mins[idx]; onSolarChanged() }, { it.toString().padStart(2, '0') }, minute, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                    Text("${sYear}年", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    WheelCol("月", months, { idx -> sMonth = months[idx]; onSolarChanged() }, { "${it}月" }, sMonth, Modifier.padding(start = 4.dp).weight(1f).height((ITEM_HEIGHT_DP * 3).dp))
                }
            } else {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    if (showYear) WheelCol("年", years, { idx -> sYear = years[idx]; onSolarChanged() }, { "$it" }, sYear, Modifier.weight(1.2f))
                    WheelCol("月", months, { idx -> sMonth = months[idx]; onSolarChanged() }, { "${it}月" }, sMonth, Modifier.weight(1f))
                    WheelCol("日", days, { idx -> sDay = days[idx]; onSolarChanged() }, { "${it}日" }, sDay, Modifier.weight(1f))
                }
            }

            Spacer(Modifier.height(20.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = onDismiss, modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant)) { Text("取消") }
                Button(onClick = {
                    val ms = if (isLunar)
                        LunarCalendar.lunarToSolarMillis(lunarYear, lunarMonth, lunarDay, lunarLeap,
                            if (showTime) hour else 0, if (showTime) minute else 0)
                    else selectedMs
                    onConfirm(if (ms == Long.MIN_VALUE) selectedMs else ms)
                }, modifier = Modifier.weight(1f)) { Text("确定", fontWeight = FontWeight.Bold) }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun WheelCol(
    label: String, items: List<Int>, onSelectedIdx: (Int) -> Unit,
    formatter: (Int) -> String, selectedValue: Int, modifier: Modifier = Modifier
) {
    val idx = items.indexOf(selectedValue).coerceAtLeast(0)
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        WheelPicker(items = items, selectedIndex = idx, onSelectedChange = onSelectedIdx, itemFormatter = formatter)
    }
}
