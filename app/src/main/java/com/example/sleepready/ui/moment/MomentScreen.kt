package com.example.sleepready.ui.moment

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.sleepready.domain.model.Moment
import com.example.sleepready.ui.moment.components.MomentCard
import com.example.sleepready.ui.moment.components.MomentDetailScreen
import com.example.sleepready.ui.moment.components.MomentEditorScreen

/**
 * 时光主屏。三态切换：列表 / 编辑 / 详情。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomentScreen(
    viewModel: MomentViewModel,
    onOpenSettings: () -> Unit = {}
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var overlay by remember { mutableStateOf<MomentOverlay>(MomentOverlay.None) }
    var menuAnchor by remember { mutableStateOf<Moment?>(null) }

    // 系统返回手势 / 左滑返回：在 overlay 上回到列表，而不是直接退到首页。
    BackHandler(enabled = overlay != MomentOverlay.None) {
        overlay = MomentOverlay.None
    }

    // 删除二次确认状态机：null = 不显示；FirstStep / SecondStep = 第 1/2 次确认
    var deleteFlow by remember { mutableStateOf<DeleteFlow?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                MomentUiEvent.Saved -> {
                    overlay = MomentOverlay.None
                    snackbar.showSnackbar("已保存")
                }
                is MomentUiEvent.Failed -> snackbar.showSnackbar(event.message)
            }
        }
    }

    when (val state = overlay) {
        MomentOverlay.None -> {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("时光") },
                        actions = {
                            IconButton(onClick = onOpenSettings) {
                                Icon(Icons.Filled.Settings, contentDescription = "设置")
                            }
                        }
                    )
                },
                floatingActionButton = {
                    ExtendedFloatingActionButton(
                        onClick = { overlay = MomentOverlay.Editor(initialId = null) },
                        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                        text = { Text("记录") },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                },
                snackbarHost = { SnackbarHost(snackbar) }
            ) { padding ->
                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "还没有时光记录\n点击右下角创建第一条",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(items, key = { it.id }) { moment ->
                            // 长按菜单：编辑 / 删除（删除走双重确认 DeleteFlow）
                            Box {
                                MomentCard(
                                    moment = moment,
                                    onClick = { overlay = MomentOverlay.Detail(moment.id) },
                                    onLongClick = { menuAnchor = moment }
                                )
                                androidx.compose.material3.DropdownMenu(
                                    expanded = menuAnchor?.id == moment.id,
                                    onDismissRequest = { menuAnchor = null }
                                ) {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("编辑") },
                                        onClick = {
                                            menuAnchor = null
                                            overlay = MomentOverlay.Editor(initialId = moment.id)
                                        }
                                    )
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("删除") },
                                        onClick = {
                                            menuAnchor = null
                                            deleteFlow = DeleteFlow.FirstStep(moment)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        is MomentOverlay.Editor -> {
            val initial by produceState<Moment?>(initialValue = null, state.initialId) {
                value = state.initialId?.let { viewModel.getById(it) }
            }
            // 等加载完才显示编辑器（避免闪现）；新建时 initialId=null 直接显示。
            if (state.initialId == null || initial != null) {
                MomentEditorScreen(
                    initial = initial,
                    onBack = { overlay = MomentOverlay.None },
                    onSave = { title, body, eventTime, keepPaths, newUris ->
                        if (initial == null) {
                            viewModel.add(title, body, eventTime, newUris)
                        } else {
                            viewModel.update(initial!!.id, title, body, eventTime, keepPaths, newUris)
                        }
                    }
                )
            }
        }

        is MomentOverlay.Detail -> {
            val moment by produceState<Moment?>(initialValue = null, state.id) {
                value = viewModel.getById(state.id)
            }
            moment?.let { m ->
                MomentDetailScreen(
                    moment = m,
                    onBack = { overlay = MomentOverlay.None },
                    onEdit = { overlay = MomentOverlay.Editor(initialId = m.id) },
                    onDelete = {
                        viewModel.delete(m.id)
                        overlay = MomentOverlay.None
                    }
                )
            }
        }
    }

    // 双重确认弹窗（左滑触发）
    deleteFlow?.let { flow ->
        when (flow) {
            is DeleteFlow.FirstStep -> AlertDialog(
                onDismissRequest = { deleteFlow = null },
                title = { Text("确定删除吗？") },
                text = { Text("将要删除「${flow.target.title}」") },
                confirmButton = {
                    TextButton(onClick = {
                        deleteFlow = DeleteFlow.SecondStep(flow.target)
                    }) { Text("确定") }
                },
                dismissButton = {
                    TextButton(onClick = { deleteFlow = null }) { Text("取消") }
                }
            )
            is DeleteFlow.SecondStep -> AlertDialog(
                onDismissRequest = { deleteFlow = null },
                title = { Text("此操作不可恢复，确认删除？") },
                text = { Text("条目和图片都会被永久删除。") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.delete(flow.target.id)
                        deleteFlow = null
                    }) { Text("永久删除") }
                },
                dismissButton = {
                    TextButton(onClick = { deleteFlow = null }) { Text("取消") }
                }
            )
        }
    }
}

private sealed interface MomentOverlay {
    data object None : MomentOverlay
    data class Editor(val initialId: Long?) : MomentOverlay
    data class Detail(val id: Long) : MomentOverlay
}

private sealed interface DeleteFlow {
    val target: Moment
    data class FirstStep(override val target: Moment) : DeleteFlow
    data class SecondStep(override val target: Moment) : DeleteFlow
}
