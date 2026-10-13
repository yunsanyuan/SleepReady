package com.example.sleepready.ui.inbox.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.sleepready.domain.model.KnowledgeCategory
import com.example.sleepready.domain.model.KnowledgeItem

/**
 * BottomSheet rendered after the user activates "Random Pick" and the pending
 * pool was non-empty. Shows the picked item's title, category label, and the
 * optional `link` / `note` fields, plus two action buttons:
 *
 * - **去学习 (Go Study)**:
 *   * If the item has a non-blank `link`, fires `Intent.ACTION_VIEW(uri)` to
 *     open it in the user's preferred handler **and** notifies the caller via
 *     [onGoStudy] so it can flip the item to `IN_PROGRESS` (Requirement 5.3).
 *   * Otherwise just notifies [onGoStudy] (Requirement 5.4).
 *   In both cases the sheet dismisses afterwards.
 * - **关闭 (Close)**: dismisses without modifying status.
 *
 * The sheet itself is purely presentational — status mutation is delegated to
 * the parent screen / ViewModel via [onGoStudy] (typically
 * `viewModel.markStudying(item.id)`).
 *
 * Validates: Requirements 5.2, 5.3, 5.4
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomPickBottomSheet(
    item: KnowledgeItem,
    onDismiss: () -> Unit,
    onGoStudy: (KnowledgeItem) -> Unit,
    onPickAgain: () -> Unit
) {
    val context = LocalContext.current
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                text = labelsFor(item.categories),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!item.link.isNullOrBlank()) {
                Text(
                    text = item.link,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (!item.note.isNullOrBlank()) {
                Text(
                    text = item.note,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("关闭")
                }
                OutlinedButton(
                    onClick = onPickAgain,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("换一个")
                }
                Button(
                    onClick = {
                        if (!item.link.isNullOrBlank()) {
                            runCatching {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.link))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                context.startActivity(intent)
                            }
                        }
                        onGoStudy(item)
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("去学习")
                }
            }
        }
    }
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
