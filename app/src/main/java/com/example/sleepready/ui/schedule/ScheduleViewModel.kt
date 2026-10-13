package com.example.sleepready.ui.schedule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.sleepready.domain.model.Schedule
import com.example.sleepready.domain.model.ScheduleType
import com.example.sleepready.permission.ReminderPermissionChecker
import com.example.sleepready.permission.ReminderPermissionState
import com.example.sleepready.repository.ScheduleRepository
import com.example.sleepready.service.schedule.ScheduleReminderScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.GregorianCalendar

/** 顶部筛选：全部 / 各类型。 */
sealed interface ScheduleFilter {
    data object All : ScheduleFilter
    data class ByType(val type: ScheduleType) : ScheduleFilter
}

/** 时间范围筛选：全部 / 今天 / 本周 / 本月。 */
enum class DateRangeFilter(val displayName: String) {
    ALL("全部"),
    TODAY("今天"),
    THIS_WEEK("本周"),
    THIS_MONTH("本月")
}

class ScheduleViewModel(
    application: Application,
    private val repo: ScheduleRepository
) : AndroidViewModel(application) {

    private val _filter = MutableStateFlow<ScheduleFilter>(ScheduleFilter.All)
    val filter: StateFlow<ScheduleFilter> = _filter.asStateFlow()

    private val _dateRange = MutableStateFlow(DateRangeFilter.ALL)
    val dateRange: StateFlow<DateRangeFilter> = _dateRange.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** 保存日程后触发一次权限预警（null = 无需预警） */
    private val _permissionWarnings = MutableStateFlow<List<String>?>(null)
    val permissionWarnings: StateFlow<List<String>?> = _permissionWarnings.asStateFlow()

    /** UI 消费后置 null */
    fun clearPermissionWarnings() { _permissionWarnings.value = null }

    @OptIn(ExperimentalCoroutinesApi::class)
    val items: StateFlow<List<Schedule>> = _filter
        .flatMapLatest { f ->
            when (f) {
                ScheduleFilter.All -> repo.observeAll()
                is ScheduleFilter.ByType -> repo.observeByType(f.type)
            }
        }
        .combine(_query) { list, q ->
            if (q.isBlank()) list
            else list.filter { schedule ->
                schedule.title.contains(q, ignoreCase = true) ||
                    (schedule.note?.contains(q, ignoreCase = true) == true) ||
                    (schedule.location?.contains(q, ignoreCase = true) == true)
            }
        }
        .combine(_dateRange) { list, range -> applyDateRange(list, range) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setFilter(filter: ScheduleFilter) {
        _filter.value = filter
    }

    fun setDateRange(range: DateRangeFilter) {
        _dateRange.value = range
    }

    fun setQuery(q: String) {
        _query.value = q
    }

    fun add(schedule: Schedule) {
        viewModelScope.launch {
            val ctx = getApplication<Application>().applicationContext
            val id = repo.add(schedule)
            val saved = schedule.copy(id = id)
            ScheduleReminderScheduler.schedule(ctx, saved)
            checkAndReportPermissions(ctx, saved.useAlarmRing)
        }
    }

    fun update(schedule: Schedule) {
        viewModelScope.launch {
            val ctx = getApplication<Application>().applicationContext
            repo.update(schedule)
            ScheduleReminderScheduler.schedule(ctx, schedule)
            checkAndReportPermissions(ctx, schedule.useAlarmRing)
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            val ctx = getApplication<Application>().applicationContext
            ScheduleReminderScheduler.cancel(ctx, id)
            repo.delete(id)
        }
    }

    /** 测试提醒：10 秒后触发，用于验证闹钟系统是否正常 */
    fun testReminder(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val ctx = getApplication<Application>().applicationContext
            val testSchedule = Schedule(
                id = -999,               // 临时 ID，不存库
                title = "🧪 测试提醒",
                type = ScheduleType.EVENT,
                priority = 1,
                targetTimeMillis = System.currentTimeMillis() + 10_000,
                reminderOffsetsMinutes = listOf(0),
                useAlarmRing = false,
                note = null,
                location = null,
                createdAt = 0L,
                updatedAt = 0L
            )
            ScheduleReminderScheduler.schedule(ctx, testSchedule)
            onResult("测试提醒已设置，10 秒后触发")
        }
    }

    /**
     * 保存日程后检查关键权限，如有缺失则通过 [permissionWarnings] 告知 UI 显示 snackbar。
     */
    private fun checkAndReportPermissions(ctx: android.content.Context, usesAlarmRing: Boolean) {
        val warnings = mutableListOf<String>()
        val status = ReminderPermissionChecker.detectAll(ctx)

        // 通知权限（所有提醒都需要）
        when (status.postNotifications) {
            is ReminderPermissionState.Denied -> warnings.add("通知权限未开启，到点可能没有提醒弹窗")
            is ReminderPermissionState.PermanentlyDenied -> warnings.add("通知权限被拒绝，请到系统设置开启")
            else -> {}
        }

        // 精确闹钟权限（所有提醒都需要准时）
        when (status.scheduleExactAlarm) {
            is ReminderPermissionState.PermanentlyDenied -> warnings.add("精确闹钟未授权，提醒可能延迟数分钟")
            else -> {}
        }

        // 锁屏全屏权限（仅闹钟强提醒需要）
        if (usesAlarmRing) {
            when (status.useFullScreenIntent) {
                is ReminderPermissionState.Denied -> warnings.add("锁屏全屏权限未开启，闹钟不会全屏显示")
                else -> {}
            }
        }

        // ROM 厂商权限（自启动等）
        val romMissing = status.romPermissions.filter { !it.isGranted }
        if (romMissing.isNotEmpty()) {
            warnings.add("手机厂商限制未解除（${romMissing.joinToString("、") { it.type }}），提醒可能被杀后台")
        }

        if (warnings.isNotEmpty()) {
            _permissionWarnings.value = warnings
        }
    }

    private fun applyDateRange(list: List<Schedule>, range: DateRangeFilter): List<Schedule> {
        if (range == DateRangeFilter.ALL) return list
        val (start, end) = when (range) {
            DateRangeFilter.TODAY -> dayBounds(System.currentTimeMillis())
            DateRangeFilter.THIS_WEEK -> weekBounds(System.currentTimeMillis())
            DateRangeFilter.THIS_MONTH -> monthBounds(System.currentTimeMillis())
            DateRangeFilter.ALL -> error("unreachable")
        }
        return list.filter { schedule ->
            val ms = schedule.targetTimeMillis ?: return@filter false
            ms in start..end
        }
    }
}

/** 该日的 [00:00, 23:59:59.999]。 */
private fun dayBounds(now: Long): Pair<Long, Long> {
    val cal = GregorianCalendar().apply { timeInMillis = now }
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    val start = cal.timeInMillis
    return start to (start + 24L * 60 * 60 * 1000 - 1)
}

/** 本周（周一到周日）。 */
private fun weekBounds(now: Long): Pair<Long, Long> {
    val cal = GregorianCalendar().apply { timeInMillis = now; firstDayOfWeek = Calendar.MONDAY }
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    val start = cal.timeInMillis
    cal.add(Calendar.DAY_OF_YEAR, 7)
    val end = cal.timeInMillis - 1
    return start to end
}

/** 本月 1 日 ~ 月末。 */
private fun monthBounds(now: Long): Pair<Long, Long> {
    val cal = GregorianCalendar().apply { timeInMillis = now }
    cal.set(Calendar.DAY_OF_MONTH, 1)
    cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
    val start = cal.timeInMillis
    cal.add(Calendar.MONTH, 1)
    val end = cal.timeInMillis - 1
    return start to end
}
