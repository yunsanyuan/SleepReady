package com.example.sleepready.ui.inbox.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.sleepready.domain.model.KnowledgeCategory
import com.example.sleepready.domain.model.KnowledgeItem
import com.example.sleepready.ui.moment.components.ImageViewerDialog

/**
 * 编辑现有 [KnowledgeItem] 的弹窗。支持多分类切换、图片增删、点开放大查看。
 */
@Composable
fun EditKnowledgeDialog(
    item: KnowledgeItem,
    onDismiss: () -> Unit,
    onSave: (
        id: Long,
        title: String,
        categories: Set<KnowledgeCategory>,
        link: String?,
        note: String?,
        keepImagePaths: List<String>,
        newImageUris: List<Uri>
    ) -> Unit
) {
    var title by remember { mutableStateOf(item.title) }
    var categories by remember { mutableStateOf(item.categories) }
    var link by remember { mutableStateOf(item.link.orEmpty()) }
    var note by remember { mutableStateOf(item.note.orEmpty()) }
    val keepPaths = remember { mutableStateListOf<String>().apply { addAll(item.imagePaths) } }
    val newImageUris = remember { mutableStateListOf<Uri>() }
    var viewerIndex by remember { mutableIntStateOf(-1) }

    val pickImages = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(
            maxItems = KnowledgeItem.MAX_IMAGES.coerceAtLeast(2)
        )
    ) { uris ->
        val remaining = (KnowledgeItem.MAX_IMAGES - keepPaths.size - newImageUris.size).coerceAtLeast(0)
        newImageUris.addAll(uris.take(remaining))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑条目") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryChip("想学", KnowledgeCategory.WANT_TO_LEARN, categories) { categories = toggle(categories, it) }
                    CategoryChip("想看", KnowledgeCategory.WANT_TO_WATCH, categories) { categories = toggle(categories, it) }
                    CategoryChip("想读", KnowledgeCategory.WANT_TO_READ, categories) { categories = toggle(categories, it) }
                    CategoryChip("想做", KnowledgeCategory.WANT_TO_DO, categories) { categories = toggle(categories, it) }
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = { Text("链接（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // 图片区
                val totalCount = keepPaths.size + newImageUris.size
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            pickImages.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = totalCount < KnowledgeItem.MAX_IMAGES
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null)
                        Text("  图片（$totalCount/${KnowledgeItem.MAX_IMAGES}）")
                    }
                }
                if (totalCount > 0) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(keepPaths) { path ->
                            ThumbWithRemove(
                                model = path,
                                onClick = { viewerIndex = keepPaths.indexOf(path) },
                                onRemove = { keepPaths.remove(path) }
                            )
                        }
                        items(newImageUris) { uri ->
                            ThumbWithRemove(
                                model = uri,
                                onClick = null,
                                onRemove = { newImageUris.remove(uri) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = title.isNotBlank() && categories.isNotEmpty(),
                onClick = {
                    onSave(
                        item.id,
                        title.trim(),
                        categories,
                        link.takeIf { it.isNotBlank() }?.trim(),
                        note.takeIf { it.isNotBlank() }?.trim(),
                        keepPaths.toList(),
                        newImageUris.toList()
                    )
                }
            ) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )

    if (viewerIndex >= 0 && keepPaths.isNotEmpty()) {
        ImageViewerDialog(
            images = keepPaths,
            initialIndex = viewerIndex,
            onDismiss = { viewerIndex = -1 }
        )
    }
}

private fun toggle(set: Set<KnowledgeCategory>, c: KnowledgeCategory): Set<KnowledgeCategory> =
    if (c in set) set - c else set + c

@Composable
private fun CategoryChip(
    label: String,
    value: KnowledgeCategory,
    selected: Set<KnowledgeCategory>,
    onToggle: (KnowledgeCategory) -> Unit
) {
    FilterChip(
        selected = value in selected,
        onClick = { onToggle(value) },
        label = { Text(label) }
    )
}

@Composable
private fun ThumbWithRemove(
    model: Any,
    onClick: (() -> Unit)?,
    onRemove: () -> Unit
) {
    Box {
        AsyncImage(
            model = model,
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(10.dp))
                .let { if (onClick != null) it.clickable { onClick() } else it }
        )
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
        ) {
            IconButton(onClick = onRemove, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "移除", modifier = Modifier.size(16.dp))
            }
        }
    }
}
