package com.example.sleepready.ui.moment.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.sleepready.domain.model.Moment
import com.example.sleepready.ui.common.DateTimeWheelSheet
import com.example.sleepready.ui.common.formatDateTimeLabel

/**
 * 时光编辑器：标题（必填）+ 时间（可选）+ 正文 + 图片（多张，从相册选择）。
 *
 * 图片来源：[ActivityResultContracts.PickMultipleVisualMedia]，无需声明
 * 任何运行时权限即可读取（系统 Photo Picker）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentEditorScreen(
    initial: Moment?,
    onBack: () -> Unit,
    onSave: (
        title: String,
        body: String?,
        eventTimeMillis: Long?,
        keepImagePaths: List<String>,
        newImageUris: List<Uri>
    ) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title.orEmpty()) }
    var body by remember { mutableStateOf(initial?.body.orEmpty()) }
    var eventTime by remember { mutableStateOf(initial?.eventTimeMillis) }
    val keepPaths = remember { mutableStateListOf<String>().apply { addAll(initial?.imagePaths.orEmpty()) } }
    val newImageUris = remember { mutableStateListOf<Uri>() }
    var showTimeSheet by remember { mutableStateOf(false) }

    val pickImages = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 9)
    ) { uris -> newImageUris.addAll(uris) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initial == null) "新建时光" else "编辑时光") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        enabled = title.isNotBlank(),
                        onClick = {
                            onSave(
                                title.trim(),
                                body.takeIf { it.isNotBlank() },
                                eventTime,
                                keepPaths.toList(),
                                newImageUris.toList()
                            )
                        }
                    ) { Text("保存") }
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("标题") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // 时间字段：点击打开 BottomSheet 滚轮选择器
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)) {
                        Text("时间（可选）", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = formatDateTimeLabel(eventTime, showTime = true),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    if (eventTime != null) {
                        TextButton(onClick = { eventTime = null }) { Text("清除") }
                    }
                    OutlinedButton(onClick = { showTimeSheet = true }) {
                        Text(if (eventTime == null) "选择" else "修改")
                    }
                }
            }

            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("正文（可选）") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )

            // 图片区
            Text("图片（${keepPaths.size + newImageUris.size}）", style = MaterialTheme.typography.titleSmall)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(keepPaths.size) { idx ->
                    val path = keepPaths[idx]
                    ImageThumb(model = path) { keepPaths.remove(path) }
                }
                items(newImageUris.size) { idx ->
                    val uri = newImageUris[idx]
                    ImageThumb(model = uri) { newImageUris.remove(uri) }
                }
                item {
                    AddImageTile(onClick = {
                        pickImages.launch(
                            androidx.activity.result.PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    })
                }
            }
        }
    }

    if (showTimeSheet) {
        DateTimeWheelSheet(
            initialMillis = eventTime,
            showTime = true,
            onConfirm = {
                eventTime = it
                showTimeSheet = false
            },
            onDismiss = { showTimeSheet = false }
        )
    }
}

@Composable
private fun ImageThumb(model: Any, onRemove: () -> Unit) {
    Box {
        AsyncImage(
            model = model,
            contentDescription = null,
            modifier = Modifier
                .size(96.dp)
                .clip(RoundedCornerShape(12.dp))
        )
        Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
        ) {
            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Filled.Close, contentDescription = "移除", modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun AddImageTile(onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.size(96.dp),
        onClick = onClick
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Add, contentDescription = "添加图片")
        }
    }
}
