package com.example.sleepready.ui.inbox

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sleepready.domain.model.KnowledgeCategory
import com.example.sleepready.domain.model.KnowledgeItem
import com.example.sleepready.domain.model.KnowledgeStatus
import com.example.sleepready.ui.inbox.components.BottomInputBar
import com.example.sleepready.ui.inbox.components.EditKnowledgeDialog
import com.example.sleepready.ui.inbox.components.KnowledgeItemCard
import com.example.sleepready.ui.inbox.components.RandomPickBottomSheet
import com.example.sleepready.ui.inbox.components.SwipeableRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KnowledgeInboxScreen(
    viewModel: KnowledgeInboxViewModel,
    onOpenSettings: () -> Unit = {}
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var pickedItem by remember { mutableStateOf<KnowledgeItem?>(null) }
    var editingItem by remember { mutableStateOf<KnowledgeItem?>(null) }
    var menuAnchor by remember { mutableStateOf<KnowledgeItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                InboxUiEvent.RequireTitle ->
                    snackbar.showSnackbar("标题不能为空")
                InboxUiEvent.RequireCategory ->
                    snackbar.showSnackbar("请先选择一个分类")
                InboxUiEvent.PendingPoolEmpty ->
                    snackbar.showSnackbar("没有未完成的条目")
                is InboxUiEvent.RandomPicked -> pickedItem = event.item
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("心动") },
                actions = {
                    IconButton(onClick = { viewModel.pickRandom() }) {
                        Icon(Icons.Filled.Shuffle, contentDescription = "随机一条")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "设置")
                    }
                }
            )
        },
        bottomBar = {
            BottomInputBar(
                onSubmit = { title, categories, link, note, imageUris ->
                    viewModel.submit(title, categories, link, note, imageUris)
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = filter == InboxFilter.All,
                    onClick = { viewModel.setFilter(InboxFilter.All) },
                    label = { Text("全部") }
                )
                KnowledgeCategory.values().forEach { c ->
                    FilterChip(
                        selected = filter is InboxFilter.ByCategory &&
                            (filter as InboxFilter.ByCategory).category == c,
                        onClick = { viewModel.setFilter(InboxFilter.ByCategory(c)) },
                        label = { Text(labelFor(c)) }
                    )
                }
            }
            if (items.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "清单是空的，先添加几条吧",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items, key = { it.id }) { item ->
                        Box {
                            SwipeableRow(
                                item = item,
                                onMarkDone = viewModel::markDone,
                                onSetStatus = viewModel::setStatus
                            ) {
                                KnowledgeItemCard(
                                    item = item,
                                    onClick = { editingItem = item },
                                    onLongClick = { menuAnchor = item }
                                )
                            }
                            DropdownMenu(
                                expanded = menuAnchor?.id == item.id,
                                onDismissRequest = { menuAnchor = null }
                            ) {
                                StatusMenuItems(item) { status ->
                                    menuAnchor = null
                                    viewModel.setStatus(item, status)
                                }
                                DropdownMenuItem(
                                    text = { Text("编辑") },
                                    onClick = {
                                        menuAnchor = null
                                        editingItem = item
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("删除") },
                                    onClick = {
                                        menuAnchor = null
                                        viewModel.delete(item.id)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    pickedItem?.let { picked ->
        RandomPickBottomSheet(
            item = picked,
            onDismiss = { pickedItem = null },
            onGoStudy = { item ->
                viewModel.markStudying(item.id)
                pickedItem = null
            },
            onPickAgain = {
                pickedItem = null
                viewModel.pickRandom()
            }
        )
    }

    editingItem?.let { editing ->
        EditKnowledgeDialog(
            item = editing,
            onDismiss = { editingItem = null },
            onSave = { id, title, categories, link, note, keepPaths, newUris ->
                viewModel.updateItem(id, title, categories, link, note, keepPaths, newUris)
                editingItem = null
            }
        )
    }
}

@Composable
private fun StatusMenuItems(item: KnowledgeItem, onPick: (KnowledgeStatus) -> Unit) {
    KnowledgeStatus.values().forEach { status ->
        if (status == item.status) return@forEach
        DropdownMenuItem(
            text = { Text("标记为「${status.displayName}」") },
            onClick = { onPick(status) }
        )
    }
}

private fun labelFor(category: KnowledgeCategory): String = when (category) {
    KnowledgeCategory.WANT_TO_LEARN -> "想学"
    KnowledgeCategory.WANT_TO_WATCH -> "想看"
    KnowledgeCategory.WANT_TO_READ -> "想读"
    KnowledgeCategory.WANT_TO_DO -> "想做"
}
