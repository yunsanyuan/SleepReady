package com.example.sleepready.ui.timer.components

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.sleepready.permission.PermissionManager

/**
 * One-time onboarding dialog shown on the timer configuration page when
 * `prefs.onboarding_shown == false`. The composable itself is stateless —
 * visibility and persistence of the "shown" flag are owned by
 * [com.example.sleepready.ui.timer.TimerConfigViewModel.onboardingNeeded] /
 * [com.example.sleepready.ui.timer.TimerConfigViewModel.dismissOnboarding].
 *
 * On confirm:
 * 1. On API 33+ it triggers [PermissionManager.requestNotifications], which
 *    launches the system permission dialog. On API < 33 the call is a no-op
 *    (POST_NOTIFICATIONS does not exist there).
 * 2. It calls [onDismiss], at which point the host should flip
 *    `prefs.onboarding_shown = true`.
 *
 * Validates Requirements 8.2 (one-time permission onboarding) and 12.3
 * (without POST_NOTIFICATIONS the countdown notification is silently
 * suppressed, so we surface the request inline).
 */
@Composable
fun OnboardingDialog(
    permissionManager: PermissionManager?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                // PermissionManager.requestNotifications already short-circuits
                // to a no-op on API < 33 via PermissionMatrix.notificationsPermission().
                // The explicit SDK guard here is just defensive documentation.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionManager?.requestNotifications()
                }
                onDismiss()
            }) {
                Text("我知道了")
            }
        },
        title = { Text("使用前请允许这些权限") },
        text = {
            Column(
                modifier = Modifier.padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    BulletItem(
                        "通知权限（POST_NOTIFICATIONS）—— Android 13+ 必须授权，" +
                            "否则倒计时通知会被系统静默屏蔽，前台 Service 仍会运行但你看不到剩余时间。"
                    )
                }
                BulletItem("勿扰模式访问 —— 用于在入梦时自动开启勿扰，需在系统设置中授权。")
                BulletItem("摄像头 —— 用于控制手电筒（CameraManager.setTorchMode）。")
                BulletItem("蓝牙 —— 用于关闭/开启蓝牙；Android 13+ 会跳到系统设置由你确认。")
                Text(
                    text = "其余开关（飞行模式、Wi-Fi、移动数据、定位、NFC）会引导你跳转到对应系统设置页手动操作。",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    )
}

@Composable
private fun BulletItem(text: String) {
    Text(
        text = "• $text",
        style = MaterialTheme.typography.bodyMedium
    )
}
