package com.example.sleepready.ui.statistics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sleepready.statistics.StatisticsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class StatisticsSnapshot(
    val totalCount: Int = 0,
    val perSection: Map<String, Int> = emptyMap(),
    val knowledgeStatus: Map<String, Int> = emptyMap(),
    val scheduleType: Map<String, Int> = emptyMap(),
    val verseDynastyTop5: Map<String, Int> = emptyMap(),
    val recent7Days: IntArray = IntArray(7),
    val streakDays: Int = 0
) {
    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}

/**
 * 数据统计 VM。所有聚合调用都委托给 Java 实现的 [StatisticsRepository]
 * （在 [Dispatchers.IO] 上执行）。
 */
class StatisticsViewModel(
    private val repo: StatisticsRepository
) : ViewModel() {

    private val _snapshot = MutableStateFlow(StatisticsSnapshot())
    val snapshot: StateFlow<StatisticsSnapshot> = _snapshot.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val snap = withContext(Dispatchers.IO) {
                StatisticsSnapshot(
                    totalCount = repo.totalCount,
                    perSection = repo.counts.asMap(),
                    knowledgeStatus = repo.knowledgeStatusBreakdown,
                    scheduleType = repo.scheduleTypeBreakdown,
                    verseDynastyTop5 = repo.verseDynastyTop5,
                    recent7Days = repo.getRecentActivity(7),
                    streakDays = repo.overallStreak
                )
            }
            _snapshot.value = snap
        }
    }
}
