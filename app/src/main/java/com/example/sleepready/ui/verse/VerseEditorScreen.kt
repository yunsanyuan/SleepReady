package com.example.sleepready.ui.verse

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sleepready.domain.model.VerseItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VerseEditorScreen(
    initialId: Long?,
    viewModel: VerseViewModel,
    onBack: () -> Unit
) {
    var initial by remember { mutableStateOf<VerseItem?>(null) }
    LaunchedEffect(initialId) { initial = initialId?.let { viewModel.getById(it) } }

    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var dynasty by remember { mutableStateOf("唐") }
    var content by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var loaded by remember { mutableStateOf(false) }
    // 模式：诗词 / 短句
    var isShort by remember { mutableStateOf(false) }

    LaunchedEffect(initial) {
        if (initial != null && !loaded) {
            title = initial!!.title
            author = initial!!.author
            dynasty = initial!!.dynasty
            content = initial!!.content
            notes = initial!!.notes.orEmpty()
            // 如果作者为空则默认为短句模式
            isShort = initial!!.author.isBlank() || initial!!.dynasty.isBlank()
            loaded = true
        }
        if (initialId == null) loaded = true
    }

    val canSave = content.isNotBlank()
            && (isShort || (title.isNotBlank() && author.isNotBlank() && dynasty.isNotBlank()))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initial == null) "收录" else "编辑", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") }
                },
                actions = {
                    TextButton(
                        enabled = canSave,
                        onClick = {
                            val t = if (isShort) title.trim().ifBlank { content.trim().lineSequence().first().take(20) }
                                    else title.trim()
                            val a = if (isShort) author.trim().ifBlank { "佚名" } else author.trim()
                            val d = if (isShort) "现代" else dynasty.trim()
                            val n = notes.takeIf { it.isNotBlank() }?.trim()
                            if (initial == null) viewModel.add(t, a, d, content.trim(), n)
                            else viewModel.update(initial!!.id, t, a, d, content.trim(), n)
                            onBack()
                        }
                    ) { Text("保存", fontWeight = FontWeight.Bold) }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 模式切换
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                FilterChip(selected = !isShort, onClick = { isShort = false }, label = { Text("诗词") })
                Spacer(Modifier.padding(horizontal = 8.dp))
                FilterChip(selected = isShort, onClick = { isShort = true }, label = { Text("短句") })
            }

            if (!isShort) {
                // 诗词模式：标题+作者+朝代
                OutlinedTextField(value = title, onValueChange = { title = it },
                    label = { Text("诗题") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                // 诗词模式：作者+朝代
                OutlinedTextField(value = author, onValueChange = { author = it },
                    label = { Text("作者") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
                Text("朝代", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    VerseItem.DYNASTIES.filter { it != "全部" }.forEach { d ->
                        FilterChip(selected = dynasty == d, onClick = { dynasty = d }, label = { Text(d) })
                    }
                }
            } else {
                // 短句模式：作者可选
                OutlinedTextField(value = author, onValueChange = { author = it },
                    label = { Text("出处/作者（可选）") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp))
            }

            // 正文 — 衬线字体预览
            Spacer(Modifier.height(4.dp))
            Text(if (isShort) "短句内容" else "全文", style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(value = content, onValueChange = { content = it },
                modifier = Modifier.fillMaxWidth().height(if (isShort) 120.dp else 180.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(fontFamily = FontFamily.Serif, lineHeight = 28.sp),
                shape = RoundedCornerShape(12.dp))

            // 赏析
            OutlinedTextField(value = notes, onValueChange = { notes = it },
                label = { Text("赏析/笔记（可选）") },
                modifier = Modifier.fillMaxWidth().height(120.dp), shape = RoundedCornerShape(12.dp))
        }
    }
}
