package com.example.sleepready.ui.inbox.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.sleepready.domain.model.KnowledgeCategory
import com.example.sleepready.domain.model.KnowledgeItem
import com.example.sleepready.domain.model.KnowledgeStatus
import com.example.sleepready.ui.theme.StatusDoneColor
import com.example.sleepready.ui.theme.StatusIdleColor
import com.example.sleepready.ui.theme.StatusPausedColor

private const val NOTE_PREVIEW_LIMIT = 15

/**
 * 探索板块条目卡片。三态状态点：
 *  - 想去做 / 在路上 / 已抵达
 *
 * v5：标题左侧若有图片则显示 40dp 圆角缩略图（首图）；备注预览独立成一行（最多 15 字 + …）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KnowledgeItemCard(
    item: KnowledgeItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    // 状态色条颜色
    val barColor = when (item.status) {
        KnowledgeStatus.IDLE -> StatusIdleColor
        KnowledgeStatus.PAUSED -> StatusPausedColor
        KnowledgeStatus.DONE -> StatusDoneColor
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 3.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            // 左侧状态色条
            Box(
                Modifier
                    .padding(start = 3.dp, top = 12.dp, bottom = 12.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(barColor)
                    .then(Modifier.padding(vertical = 0.dp))
                    .padding(start = 3.dp)
            )
            // 正文
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            if (item.imagePaths.isNotEmpty()) {
                AsyncImage(
                    model = item.imagePaths.first(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // 备注预览（独立一行）
                if (!item.note.isNullOrBlank()) {
                    Text(
                        text = item.note.take(NOTE_PREVIEW_LIMIT) +
                            if (item.note.length > NOTE_PREVIEW_LIMIT) "…" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "${labelsFor(item.categories)} · ${item.status.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
            } // end outer Row
    }
}

@Composable
private fun StatusDot(status: KnowledgeStatus) {
    val color = when (status) {
        KnowledgeStatus.IDLE -> StatusIdleColor
        KnowledgeStatus.PAUSED -> StatusPausedColor
        KnowledgeStatus.DONE -> StatusDoneColor
    }
    Box(
        modifier = Modifier
            .size(12.dp)
            .clip(CircleShape)
            .background(color)
    )
}

private fun labelsFor(categories: Set<KnowledgeCategory>): String =
    if (categories.isEmpty()) "未分类"
    else KnowledgeCategory.values()
        .filter { it in categories }
        .joinToString(" / ") { labelFor(it) }

private fun labelFor(category: KnowledgeCategory): String = when (category) {
    KnowledgeCategory.WANT_TO_LEARN -> "想学"
    KnowledgeCategory.WANT_TO_WATCH -> "想看"
    KnowledgeCategory.WANT_TO_READ -> "想读"
    KnowledgeCategory.WANT_TO_DO -> "想做"
}
