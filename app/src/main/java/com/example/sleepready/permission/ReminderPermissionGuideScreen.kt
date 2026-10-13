package com.example.sleepready.permission

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.example.sleepready.R

@Composable
fun rememberPermissionGuideItems(): List<PermissionGuideItem> {
    val context = LocalContext.current
    var items by remember { mutableStateOf<List<PermissionGuideItem>>(emptyList()) }

    LaunchedEffect(Unit) {
        val status = ReminderPermissionChecker.detectAll(context)
        // toGuideItems 内部已包含 ROM 权限项，无需重复检测
        val guideItems = ReminderPermissionChecker.toGuideItems(status, context)
        val all = guideItems
            .filter { !PermissionGuideState.isInCooldown(context, it.key) }
            .sortedBy { it.priority }
        items = all
    }

    return items
}

@Composable
fun PermissionGuideCard(
    items: List<PermissionGuideItem>,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return

    val context = LocalContext.current
    val activity = context as? Activity

    val runtimePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        // Permissions result handled on next recomposition via LaunchedEffect
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_dialog_alert),
                    contentDescription = null,
                    tint = Color(0xFFF57C00),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.size(8.dp))
                Text(
                    text = "提醒权限未开启",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFE65100)
                )
            }

            Spacer(Modifier.height(12.dp))

            items.forEach { item ->
                PermissionGuideItemRow(
                    item = item,
                    onAction = {
                        when (item.actionType) {
                            ActionType.REQUEST_RUNTIME -> {
                                val permissions = mutableListOf<String>()
                                item.runtimePermission?.let { permissions.add(it) }
                                if (permissions.isNotEmpty() && activity != null) {
                                    runtimePermissionLauncher.launch(permissions.toTypedArray())
                                }
                            }
                            ActionType.GOTO_SETTINGS -> {
                                item.settingsIntent?.let { intent ->
                                    try { context.startActivity(intent) } catch (_: Throwable) {}
                                }
                            }
                            ActionType.GOTO_ROM_SETTINGS -> {
                                val intent = item.settingsIntent ?: buildAppDetailsIntent(context)
                                try { context.startActivity(intent) } catch (_: Throwable) {
                                    try { context.startActivity(buildAppDetailsIntent(context)) } catch (_: Throwable) {}
                                }
                            }
                        }
                    },
                    onSkip = { onDismiss(item.key) }
                )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun PermissionGuideItemRow(
    item: PermissionGuideItem,
    onAction: () -> Unit,
    onSkip: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = Color(0xFF37474F)
        )
        Text(
            text = item.description,
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF78909C),
            modifier = Modifier.padding(top = 2.dp)
        )
        Row(
            horizontalArrangement = Arrangement.End,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
        ) {
            OutlinedButton(
                onClick = onSkip,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Text("跳过", style = MaterialTheme.typography.labelMedium)
            }
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF57C00),
                    contentColor = Color.White
                )
            ) {
                Text("去设置", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

private fun buildAppDetailsIntent(context: Context): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = "package:${context.packageName}".toUri()
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }