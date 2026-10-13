package com.example.sleepready.ui.statistics

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen(
    viewModel: StatisticsViewModel,
    onBack: () -> Unit
) {
    val snap by viewModel.snapshot.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("数据统计") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 总条目 + 连续天数（双卡片）
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatBigCard(modifier = Modifier.weight(1f), label = "总条目", value = snap.totalCount.toString())
                StatBigCard(modifier = Modifier.weight(1f), label = "连续天数", value = "${snap.streakDays} 天")
            }

            // 各板块计数
            SectionCard("各板块条目") {
                snap.perSection.forEach { (k, v) ->
                    StatRow(k, v, snap.totalCount.coerceAtLeast(1))
                }
            }

            // 心动状态分布
            if (snap.knowledgeStatus.isNotEmpty()) {
                val total = snap.knowledgeStatus.values.sum().coerceAtLeast(1)
                SectionCard("心动状态分布") {
                    snap.knowledgeStatus.forEach { (k, v) -> StatRow(k, v, total) }
                }
            }

            // 印记类型分布
            if (snap.scheduleType.isNotEmpty()) {
                val total = snap.scheduleType.values.sum().coerceAtLeast(1)
                SectionCard("印记类型分布") {
                    snap.scheduleType.forEach { (k, v) -> StatRow(k, v, total) }
                }
            }

            // 拾句朝代 Top 5
            if (snap.verseDynastyTop5.isNotEmpty()) {
                val total = snap.verseDynastyTop5.values.sum().coerceAtLeast(1)
                SectionCard("拾句朝代 Top 5") {
                    snap.verseDynastyTop5.forEach { (k, v) -> StatRow(k, v, total) }
                }
            }

            // 最近 7 天柱状图
            SectionCard("最近 7 天活动") {
                BarChart(values = snap.recent7Days)
            }
        }
    }
}

@Composable
private fun StatBigCard(modifier: Modifier = Modifier, label: String, value: String) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            content()
        }
    }
}

@Composable
private fun StatRow(label: String, value: Int, total: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text("$value", style = MaterialTheme.typography.bodyMedium)
        }
        LinearProgressIndicator(
            progress = { (value.toFloat() / total.toFloat()).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
        )
    }
}

@Composable
private fun BarChart(values: IntArray) {
    val max = (values.maxOrNull() ?: 0).coerceAtLeast(1)
    val color = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 索引 0 = 今天，倒序展示成"6 天前 → 今天"
        for (i in values.indices.reversed()) {
            val v = values[i]
            val ratio = v.toFloat() / max.toFloat()
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("$v", style = MaterialTheme.typography.labelSmall)
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height((100 * ratio).dp.coerceAtLeast(2.dp))
                        .clip(RoundedCornerShape(4.dp))
                        .background(color)
                )
                Spacer(Modifier.size(2.dp))
                Text(
                    text = if (i == 0) "今" else "${i}d",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
