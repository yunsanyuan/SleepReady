package com.example.sleepready.ui.moment.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.sleepready.domain.model.Moment
import com.example.sleepready.ui.common.formatDateTimeLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentDetailScreen(
    moment: Moment,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmDeleteFinal by remember { mutableStateOf(false) }
    var viewerIndex by remember { mutableIntStateOf(-1) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("时光详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { shareMoment(context, moment) }) {
                        Icon(Icons.Filled.Share, contentDescription = "分享")
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Filled.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Filled.Delete, contentDescription = "删除")
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题
            Text(
                text = moment.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )

            // 元信息：事件时间 + 创建时间，单行更克制
            val eventLabel = moment.eventTimeMillis?.let { formatDateTimeLabel(it, showTime = true) }
            val metaText = buildString {
                if (eventLabel != null) {
                    append(eventLabel)
                } else {
                    append("未填时间")
                }
                append("  ·  创建于 ")
                append(formatDateTimeLabel(moment.createdAt, showTime = true))
            }
            Text(
                text = metaText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            )

            // 正文
            if (!moment.body.isNullOrBlank()) {
                Text(
                    text = moment.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // 图片：单张全宽 240dp，多张 2 列网格 1:1
            if (moment.imagePaths.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                if (moment.imagePaths.size == 1) {
                    val path = moment.imagePaths[0]
                    AsyncImage(
                        model = path,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { viewerIndex = 0 }
                    )
                } else {
                    ImageGrid(
                        paths = moment.imagePaths,
                        onClickIndex = { viewerIndex = it }
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("确定删除吗？") },
            text = { Text("将要删除「${moment.title}」") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    confirmDeleteFinal = true
                }) { Text("确定") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("取消") }
            }
        )
    }
    if (confirmDeleteFinal) {
        AlertDialog(
            onDismissRequest = { confirmDeleteFinal = false },
            title = { Text("此操作不可恢复，确认删除？") },
            text = { Text("条目和图片都会被永久删除。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteFinal = false
                    onDelete()
                }) { Text("永久删除") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteFinal = false }) { Text("取消") }
            }
        )
    }

    if (viewerIndex >= 0) {
        ImageViewerDialog(
            images = moment.imagePaths,
            initialIndex = viewerIndex,
            onDismiss = { viewerIndex = -1 }
        )
    }
}

/**
 * 2 列图片网格。每行两张，最后单数最后一张占左半。
 * 不用 LazyVerticalGrid 因为外层已经是 verticalScroll，避免嵌套滚动。
 */
@Composable
private fun ImageGrid(
    paths: List<String>,
    onClickIndex: (Int) -> Unit
) {
    val rows = paths.chunked(2)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEachIndexed { rowIdx, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEachIndexed { colIdx, path ->
                    val absoluteIndex = rowIdx * 2 + colIdx
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black.copy(alpha = 0.05f))
                            .clickable { onClickIndex(absoluteIndex) }
                    ) {
                        AsyncImage(
                            model = path,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                // 单数最后一行右侧补空，保持网格对齐
                if (row.size == 1) {
                    Box(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

private fun shareMoment(context: android.content.Context, moment: Moment) {
    val timeText = moment.eventTimeMillis?.let {
        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(it))
    } ?: "未填时间"
    val text = buildString {
        append(moment.title).append("\n\n")
        append("时间：").append(timeText).append("\n\n")
        if (!moment.body.isNullOrBlank()) append(moment.body)
    }.trim()
    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(android.content.Intent.EXTRA_TEXT, text)
        addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching {
        context.startActivity(android.content.Intent.createChooser(intent, "分享时光"))
    }
}
