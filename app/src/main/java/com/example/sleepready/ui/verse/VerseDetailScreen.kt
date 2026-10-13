package com.example.sleepready.ui.verse

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sleepready.domain.model.VerseItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerseDetailScreen(id: Long, viewModel: VerseViewModel, onBack: () -> Unit, onEdit: (Long) -> Unit) {
    val item: VerseItem? by produceState<VerseItem?>(initialValue = null, id) { value = viewModel.getById(id) }
    var confirmDelete by remember { mutableStateOf(false) }
    val isShort = item?.author == "佚名" || (item?.dynasty == "现代" && item?.author == "佚名")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(item?.title ?: "拾句", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                actions = {
                    IconButton(onClick = { viewModel.pickRandom() }) {
                        Icon(Icons.Outlined.Shuffle, "随机一首")
                    }
                    item?.let { v ->
                        IconButton(onClick = { onEdit(v.id) }) { Icon(Icons.Outlined.Edit, "编辑") }
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Outlined.Delete, "删除") }
                    }
                }
            )
        }
    ) { padding ->
        item?.let { v ->
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 标题
                Text(v.title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

                // 作者/朝代（诗词模式显示）
                if (!isShort) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(v.dynasty, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                        Text(" · ", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                        Text(v.author, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // 正文 — 衬线字体 + 左侧引号装饰
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                ) {
                    Row(Modifier.padding(16.dp)) {
                        Icon(Icons.Outlined.FormatQuote, null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            modifier = Modifier.padding(top = 2.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = v.content,
                            fontFamily = FontFamily.Serif,
                            fontSize = if (isShort) 22.sp else 19.sp,
                            lineHeight = if (isShort) 34.sp else 32.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f),
                            textAlign = if (isShort) TextAlign.Center else TextAlign.Start,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 赏析
                if (!v.notes.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row {
                        Box(
                            Modifier
                                .width(3.dp)
                                .height(24.dp)
                                .padding(top = 2.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("赏析", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(6.dp))
                            Text(v.notes, style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 24.sp, fontSize = 15.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        item?.let { v ->
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text("删除「${v.title}」？") },
                text = { Text("删除后无法恢复。") },
                confirmButton = { TextButton(onClick = { confirmDelete = false; viewModel.delete(v.id); onBack() }) { Text("删除") } },
                dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } }
            )
        }
    }
}
