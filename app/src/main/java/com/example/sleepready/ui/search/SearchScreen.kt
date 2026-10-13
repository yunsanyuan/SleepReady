package com.example.sleepready.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sleepready.search.SearchableItem

/**
 * 全局搜索页。结果按板块（印记 / 心动 / 时光 / 拾句）分组显示，关键词高亮。
 *
 * 点击跳转：印记/心动/时光由参数回调跳目标板块；拾句直接跳详情页（参数传 id）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    viewModel: SearchViewModel,
    onBack: () -> Unit,
    onJumpToSchedule: () -> Unit,
    onJumpToExplore: () -> Unit,
    onJumpToMoment: () -> Unit,
    onJumpToVerseDetail: (Long) -> Unit
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val results by viewModel.results.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("搜索") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text("搜索印记 / 心动 / 时光 / 拾句") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "清空")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            )

            when {
                query.isBlank() -> EmptyHint("输入关键词开始搜索")
                results.isEmpty() -> EmptyHint("没有匹配结果")
                else -> {
                    val grouped = results.groupBy { it.source }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SearchableItem.Source.values().forEach { src ->
                            val list = grouped[src] ?: return@forEach
                            item(key = "h_${src.name}") {
                                SectionHeader("${src.displayName} · ${list.size} 条")
                            }
                            items(list, key = { "${src.name}_${it.id}" }) { hit ->
                                ResultCard(
                                    title = hit.title,
                                    snippet = hit.snippet,
                                    query = query,
                                    onClick = {
                                        when (hit.source) {
                                            SearchableItem.Source.SCHEDULE -> onJumpToSchedule()
                                            SearchableItem.Source.KNOWLEDGE -> onJumpToExplore()
                                            SearchableItem.Source.MOMENT -> onJumpToMoment()
                                            SearchableItem.Source.VERSE -> onJumpToVerseDetail(hit.id)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun ResultCard(title: String, snippet: String, query: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = highlight(title, query),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (snippet.isNotBlank()) {
                Text(
                    text = highlight(snippet, query),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 把 [text] 中所有 [query] 的出现高亮（背景色淡黄）。大小写不敏感。 */
private fun highlight(text: String, query: String): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    val highlight = SpanStyle(
        background = Color(0xFFFFE082),
        color = Color(0xFF222222),
        fontWeight = FontWeight.SemiBold
    )
    return buildAnnotatedString {
        var cursor = 0
        val lower = text.lowercase()
        val q = query.lowercase()
        while (cursor < text.length) {
            val idx = lower.indexOf(q, cursor)
            if (idx < 0) {
                append(text.substring(cursor))
                break
            }
            append(text.substring(cursor, idx))
            withStyle(highlight) { append(text.substring(idx, idx + q.length)) }
            cursor = idx + q.length
        }
    }
}
