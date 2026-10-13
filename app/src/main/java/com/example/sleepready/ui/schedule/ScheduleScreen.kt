package com.example.sleepready.ui.schedule

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sleepready.domain.model.Schedule
import com.example.sleepready.domain.model.ScheduleType
import com.example.sleepready.permission.PermissionGuideCard
import com.example.sleepready.permission.PermissionGuideItem
import com.example.sleepready.permission.PermissionGuideState
import com.example.sleepready.permission.ReminderPermissionChecker
import com.example.sleepready.permission.ReminderPermissionState
import com.example.sleepready.permission.rememberPermissionGuideItems
import com.example.sleepready.ui.schedule.components.ScheduleCard
import kotlinx.coroutines.launch
import com.example.sleepready.ui.schedule.components.ScheduleEditorScreen
import com.example.sleepready.ui.schedule.components.ScheduleTypePickerScreen
import kotlinx.coroutines.flow.StateFlow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
    viewModel: ScheduleViewModel,
    onOpenSettings: () -> Unit = {}
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val dateRange by viewModel.dateRange.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    var menuAnchor by remember { mutableStateOf<Schedule?>(null) }
    var overlay by remember { mutableStateOf<EditorOverlay>(EditorOverlay.None) }

    BackHandler(enabled = overlay != EditorOverlay.None) {
        overlay = when (val s = overlay) {
            is EditorOverlay.Editor -> if (s.cameFromTypePicker) EditorOverlay.TypePicker else EditorOverlay.None
            EditorOverlay.TypePicker -> EditorOverlay.None
            EditorOverlay.None -> EditorOverlay.None
        }
    }

    when (val state = overlay) {
        EditorOverlay.None -> {
            ScheduleListContent(
                items = items,
                filter = filter,
                dateRange = dateRange,
                query = query,
                onSetFilter = viewModel::setFilter,
                onSetDateRange = viewModel::setDateRange,
                onSetQuery = viewModel::setQuery,
                onOpenSettings = onOpenSettings,
                onClickItem = {
                    overlay = EditorOverlay.Editor(it, type = it.type, cameFromTypePicker = false)
                },
                onLongClickItem = { menuAnchor = it },
                menuAnchor = menuAnchor,
                onDismissMenu = { menuAnchor = null },
                onEdit = {
                    menuAnchor = null
                    overlay = EditorOverlay.Editor(it, type = it.type, cameFromTypePicker = false)
                },
                onDelete = {
                    menuAnchor = null
                    viewModel.delete(it.id)
                },
                onClickAdd = { overlay = EditorOverlay.TypePicker },
                permissionWarnings = viewModel.permissionWarnings,
                onClearPermissionWarnings = viewModel::clearPermissionWarnings,
                onTestReminder = { viewModel.testReminder(it) }
            )
        }
        EditorOverlay.TypePicker -> {
            ScheduleTypePickerScreen(
                onPick = { type ->
                    overlay = EditorOverlay.Editor(initial = null, type = type, cameFromTypePicker = true)
                },
                onBack = { overlay = EditorOverlay.None }
            )
        }
        is EditorOverlay.Editor -> {
            ScheduleEditorScreen(
                initial = state.initial,
                type = state.type,
                onBack = {
                    overlay = if (state.cameFromTypePicker) EditorOverlay.TypePicker else EditorOverlay.None
                },
                onSave = { schedule ->
                    if (state.initial == null) viewModel.add(schedule)
                    else viewModel.update(schedule)
                    overlay = EditorOverlay.None
                }
            )
        }
    }
}

private sealed interface EditorOverlay {
    data object None : EditorOverlay
    data object TypePicker : EditorOverlay
    data class Editor(
        val initial: Schedule?,
        val type: ScheduleType,
        val cameFromTypePicker: Boolean
    ) : EditorOverlay
}

// ==================== 列表页 ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ScheduleListContent(
    items: List<Schedule>,
    filter: ScheduleFilter,
    dateRange: DateRangeFilter,
    query: String,
    onSetFilter: (ScheduleFilter) -> Unit,
    onSetDateRange: (DateRangeFilter) -> Unit,
    onSetQuery: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onClickItem: (Schedule) -> Unit,
    onLongClickItem: (Schedule) -> Unit,
    menuAnchor: Schedule?,
    onDismissMenu: () -> Unit,
    onEdit: (Schedule) -> Unit,
    onDelete: (Schedule) -> Unit,
    onClickAdd: () -> Unit,
    permissionWarnings: StateFlow<List<String>?>,
    onClearPermissionWarnings: () -> Unit,
    onTestReminder: ((String) -> Unit) -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val guideItems = rememberPermissionGuideItems()
    var dismissedKeys by remember { mutableStateOf(setOf<String>()) }
    var showPermissionBanner by remember { mutableStateOf(true) }

    // 实时权限检测
    val permStatus = remember { ReminderPermissionChecker.detectAll(context) }
    val hasCriticalIssue = permStatus.postNotifications !is ReminderPermissionState.Granted
            || permStatus.scheduleExactAlarm !is ReminderPermissionState.Granted

    LaunchedEffect(Unit) {
        if (PermissionGuideState.consumeDegradedReminderFlag(context)) {
            snackbarHostState.showSnackbar("检测到提醒降级，请在设置中开启权限")
        }
    }

    val warnings by permissionWarnings.collectAsStateWithLifecycle()
    LaunchedEffect(warnings) {
        val list = warnings ?: return@LaunchedEffect
        onClearPermissionWarnings()
        list.forEach { msg -> snackbarHostState.showSnackbar(msg) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("印记", fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    actions = {
                        IconButton(onClick = {
                            onTestReminder { msg ->
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(msg)
                                }
                            }
                        }) {
                            Icon(
                                Icons.Filled.BugReport,
                                contentDescription = "测试提醒",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                        IconButton(onClick = onOpenSettings) {
                            Icon(Icons.Filled.Settings, contentDescription = "设置")
                        }
                    }
                )

                // ★ 权限危机横幅（不可忽略）
                AnimatedVisibility(
                    visible = hasCriticalIssue && showPermissionBanner,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp)
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFF3E0))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "提醒功能受限",
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE65100)
                                )
                            )
                            Text(
                                "请开启通知和精确闹钟权限，否则到点不会提醒",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFBF360C).copy(alpha = 0.8f)
                            )
                        }
                        TextButton(onClick = { onOpenSettings() }) {
                            Text("去开启", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        IconButton(onClick = { showPermissionBanner = false }) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "关闭",
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFF795548)
                            )
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onClickAdd,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("添加", fontWeight = FontWeight.Medium) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(16.dp)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 权限引导卡片
            val visibleGuideItems = guideItems.filter { it.key !in dismissedKeys }
            if (visibleGuideItems.isNotEmpty()) {
                PermissionGuideCard(
                    items = visibleGuideItems,
                    onDismiss = { key ->
                        PermissionGuideState.markSkipped(context, key)
                        dismissedKeys = dismissedKeys + key
                    }
                )
            }

            // 搜索框
            OutlinedTextField(
                value = query,
                onValueChange = onSetQuery,
                placeholder = { Text("搜索标题 / 备注 / 地点") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onSetQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "清空")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            )

            // 日期范围 chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DateRangeFilter.values().forEach { r ->
                    FilterChip(
                        selected = dateRange == r,
                        onClick = { onSetDateRange(r) },
                        label = { Text(r.displayName) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // 类型 chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = filter == ScheduleFilter.All,
                    onClick = { onSetFilter(ScheduleFilter.All) },
                    label = { Text("全部类型") },
                    shape = RoundedCornerShape(8.dp)
                )
                ScheduleType.values().forEach { t ->
                    FilterChip(
                        selected = filter is ScheduleFilter.ByType && filter.type == t,
                        onClick = { onSetFilter(ScheduleFilter.ByType(t)) },
                        label = { Text(t.displayName) },
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            }

            // 列表或空状态
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text("还没有印记", style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(6.dp))
                        Text("点击右下角 + 添加一条日程或纪念日",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    }
                }
            } else {
                val grouped = remember(items, dateRange) { groupSchedules(items, dateRange) }
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    grouped.forEach { (header, group) ->
                        item(key = "header_$header") {
                            Text(
                                text = header,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp, top = 8.dp, bottom = 2.dp)
                            )
                        }
                        items(group, key = { it.id }) { schedule ->
                            Box {
                                ScheduleCard(
                                    schedule = schedule,
                                    onClick = { onClickItem(schedule) },
                                    onLongClick = { onLongClickItem(schedule) }
                                )
                                DropdownMenu(
                                    expanded = menuAnchor?.id == schedule.id,
                                    onDismissRequest = onDismissMenu
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("编辑") },
                                        onClick = { onEdit(schedule) }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                                        onClick = { onDelete(schedule) }
                                    )
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

// ==================== 分组逻辑 ====================

private fun groupSchedules(
    items: List<Schedule>,
    range: DateRangeFilter
): List<Pair<String, List<Schedule>>> {
    val now = System.currentTimeMillis()
    val (todayStart, todayEnd) = dayRange(now)
    val (weekStart, weekEnd) = weekRange(now)
    val (monthStart, monthEnd) = monthRange(now)
    val dayFmt = SimpleDateFormat("M月d日 EEE", Locale.CHINESE)

    return when (range) {
        DateRangeFilter.TODAY -> listOf("今天" to items)
        DateRangeFilter.THIS_WEEK, DateRangeFilter.THIS_MONTH -> {
            val grouped = items.groupBy { schedule ->
                val ms = schedule.targetTimeMillis
                if (ms == null) "未设时间" else dayFmt.format(Date(ms))
            }
            grouped.entries.sortedBy { (_, list) ->
                list.firstOrNull()?.targetTimeMillis ?: Long.MAX_VALUE
            }.map { it.key to it.value }
        }
        DateRangeFilter.ALL -> {
            val expired = mutableListOf<Schedule>()
            val today = mutableListOf<Schedule>()
            val thisWeek = mutableListOf<Schedule>()
            val thisMonth = mutableListOf<Schedule>()
            val later = mutableListOf<Schedule>()
            val noTime = mutableListOf<Schedule>()
            items.forEach { s ->
                // 生日、纪念日不过期（每年循环）
                val canExpire = s.type == ScheduleType.EVENT || s.type == ScheduleType.COUNTDOWN
                when {
                    s.targetTimeMillis == null -> noTime += s
                    canExpire && s.targetTimeMillis < todayStart -> expired += s
                    s.targetTimeMillis in todayStart..todayEnd -> today += s
                    s.targetTimeMillis in weekStart..weekEnd -> thisWeek += s
                    s.targetTimeMillis in monthStart..monthEnd -> thisMonth += s
                    else -> later += s
                }
            }
            buildList {
                if (today.isNotEmpty()) add("今天" to today)
                if (thisWeek.isNotEmpty()) add("本周" to thisWeek)
                if (thisMonth.isNotEmpty()) add("本月" to thisMonth)
                if (later.isNotEmpty()) add("更晚" to later)
                if (expired.isNotEmpty()) add("已过期" to expired)
                if (noTime.isNotEmpty()) add("未设时间" to noTime)
            }
        }
    }
}

private fun dayRange(now: Long): Pair<Long, Long> {
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    val s = cal.timeInMillis
    return s to (s + 24L * 60 * 60 * 1000 - 1)
}

private fun weekRange(now: Long): Pair<Long, Long> {
    val cal = Calendar.getInstance().apply {
        timeInMillis = now; firstDayOfWeek = Calendar.MONDAY
    }
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    val s = cal.timeInMillis
    cal.add(Calendar.DAY_OF_YEAR, 7)
    return s to (cal.timeInMillis - 1)
}

private fun monthRange(now: Long): Pair<Long, Long> {
    val cal = Calendar.getInstance().apply { timeInMillis = now }
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    val s = cal.timeInMillis
    cal.add(Calendar.MONTH, 1)
    return s to (cal.timeInMillis - 1)
}
