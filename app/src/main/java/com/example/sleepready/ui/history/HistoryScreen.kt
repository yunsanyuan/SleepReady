package com.example.sleepready.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sleepready.domain.model.ExecutionHistory
import com.example.sleepready.ui.history.components.ExpandableHistoryCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onBack: () -> Unit
) {
    val records by viewModel.records.collectAsStateWithLifecycle()
    var pendingClearAll by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<ExecutionHistory?>(null) }
    var menuExpandedId by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("历史记录") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (records.isNotEmpty()) {
                        IconButton(onClick = { pendingClearAll = true }) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = "一键清空")
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (records.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "还没有历史记录\n完成一次入梦倒计时后这里会有内容",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(records, key = { it.id }) { record ->
                    Box {
                        ExpandableHistoryCard(
                            record = record,
                            onLongClick = { menuExpandedId = record.id }
                        )
                        DropdownMenu(
                            expanded = menuExpandedId == record.id,
                            onDismissRequest = { menuExpandedId = null }
                        ) {
                            DropdownMenuItem(
                                text = { Text("删除此条") },
                                onClick = {
                                    menuExpandedId = null
                                    pendingDelete = record
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (pendingClearAll) {
        AlertDialog(
            onDismissRequest = { pendingClearAll = false },
            title = { Text("清空全部历史？") },
            text = { Text("此操作不可撤销，所有历史记录将被永久删除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingClearAll = false
                        viewModel.clearAll()
                    }
                ) { Text("清空") }
            },
            dismissButton = {
                TextButton(onClick = { pendingClearAll = false }) { Text("取消") }
            }
        )
    }

    pendingDelete?.let { record ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除该记录？") },
            text = { Text("此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.delete(record.id)
                        pendingDelete = null
                    }
                ) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}
