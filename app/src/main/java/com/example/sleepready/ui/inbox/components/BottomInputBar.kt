package com.example.sleepready.ui.inbox.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.sleepready.domain.model.KnowledgeCategory
import com.example.sleepready.domain.model.KnowledgeItem

/**
 * 心动页底部输入区。默认只渲染一个折叠的「+ 添加」按钮；点击后展开完整表单。
 */
@Composable
fun BottomInputBar(
    onSubmit: (
        title: String,
        categories: Set<KnowledgeCategory>,
        link: String?,
        note: String?,
        imageUris: List<Uri>
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var formVisible by rememberSaveable { mutableStateOf(false) }
    var title by rememberSaveable { mutableStateOf("") }
    var link by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }
    val selectedCategories = remember { mutableStateOf<Set<KnowledgeCategory>>(emptySet()) }
    val pickedUris = remember { mutableStateListOf<Uri>() }
    var showCategoryHint by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        tonalElevation = 4.dp
    ) {
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            if (!formVisible) {
                Button(
                    onClick = { formVisible = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Text(
                        text = "  添加",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else {
                ExpandedForm(
                    title = title,
                    onTitleChange = { title = it },
                    link = link,
                    onLinkChange = { link = it },
                    note = note,
                    onNoteChange = { note = it },
                    selectedCategories = selectedCategories.value,
                    onToggleCategory = { c ->
                        selectedCategories.value = if (c in selectedCategories.value)
                            selectedCategories.value - c
                        else selectedCategories.value + c
                        showCategoryHint = false
                    },
                    pickedUris = pickedUris,
                    showCategoryHint = showCategoryHint,
                    onClose = {
                        formVisible = false
                        showCategoryHint = false
                        pickedUris.clear()
                    },
                    onSubmit = {
                        if (selectedCategories.value.isEmpty()) {
                            showCategoryHint = true
                            return@ExpandedForm
                        }
                        if (title.isBlank()) return@ExpandedForm
                        onSubmit(
                            title.trim(),
                            selectedCategories.value,
                            link.takeIf { it.isNotBlank() }?.trim(),
                            note.takeIf { it.isNotBlank() }?.trim(),
                            pickedUris.toList()
                        )
                        title = ""
                        link = ""
                        note = ""
                        pickedUris.clear()
                        showCategoryHint = false
                        formVisible = false
                    }
                )
            }
        }
    }
}

@Composable
private fun ExpandedForm(
    title: String,
    onTitleChange: (String) -> Unit,
    link: String,
    onLinkChange: (String) -> Unit,
    note: String,
    onNoteChange: (String) -> Unit,
    selectedCategories: Set<KnowledgeCategory>,
    onToggleCategory: (KnowledgeCategory) -> Unit,
    pickedUris: androidx.compose.runtime.snapshots.SnapshotStateList<Uri>,
    showCategoryHint: Boolean,
    onClose: () -> Unit,
    onSubmit: () -> Unit
) {
    val pickImages = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(
            maxItems = KnowledgeItem.MAX_IMAGES.coerceAtLeast(2)
        )
    ) { uris ->
        val remaining = (KnowledgeItem.MAX_IMAGES - pickedUris.size).coerceAtLeast(0)
        pickedUris.addAll(uris.take(remaining))
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = title,
                onValueChange = onTitleChange,
                placeholder = { Text("想学 / 想看 / 想读 / 想做什么？") },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.Close, contentDescription = "收起")
            }
        }

        AnimatedVisibility(
            visible = true,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryChip("想学", KnowledgeCategory.WANT_TO_LEARN, selectedCategories, onToggleCategory)
                    CategoryChip("想看", KnowledgeCategory.WANT_TO_WATCH, selectedCategories, onToggleCategory)
                    CategoryChip("想读", KnowledgeCategory.WANT_TO_READ, selectedCategories, onToggleCategory)
                    CategoryChip("想做", KnowledgeCategory.WANT_TO_DO, selectedCategories, onToggleCategory)
                }
                if (showCategoryHint) {
                    Text(
                        text = "请至少选择一个分类",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = onNoteChange,
                    placeholder = { Text("备注（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = link,
                    onValueChange = onLinkChange,
                    placeholder = { Text("链接（可选）") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = {
                            pickImages.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        enabled = pickedUris.size < KnowledgeItem.MAX_IMAGES
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null)
                        Text("  添加图片（${pickedUris.size}/${KnowledgeItem.MAX_IMAGES}）")
                    }
                }
                if (pickedUris.isNotEmpty()) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(pickedUris) { uri ->
                            UriThumb(uri = uri, onRemove = { pickedUris.remove(uri) })
                        }
                    }
                }
                Button(
                    onClick = onSubmit,
                    enabled = title.isNotBlank(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("保存", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}

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
private fun UriThumb(uri: Uri, onRemove: () -> Unit) {
    Box {
        AsyncImage(
            model = uri,
            contentDescription = null,
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(10.dp))
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
