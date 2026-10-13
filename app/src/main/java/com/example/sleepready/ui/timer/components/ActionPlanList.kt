package com.example.sleepready.ui.timer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sleepready.domain.model.ActionPlan
import com.example.sleepready.domain.model.SwitchAction
import com.example.sleepready.domain.model.SwitchItem
import com.example.sleepready.ui.theme.WarmOrange

/**
 * 13-row action plan editor in the canonical [SwitchItem.executionOrder].
 *
 * Each row uses a custom tri-state segmented control where every segment has
 * the **exact same width and height** regardless of selection state — the
 * selected segment changes background colour only, never size. This fixes the
 * earlier issue where the selected option visually "popped" larger.
 *
 * Property P6: [SwitchItem.SCREEN_RECORDING] is rendered as a two-state row
 * (NONE / TURN_OFF) so the user cannot pick a value that
 * [ActionPlan.normalized] would later strip.
 */
@Composable
fun ActionPlanList(
    plan: ActionPlan,
    onChange: (SwitchItem, SwitchAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SwitchItem.executionOrder.forEach { item ->
            ActionPlanRow(
                item = item,
                action = plan.actionFor(item),
                onChange = { onChange(item, it) }
            )
        }
    }
}

@Composable
private fun ActionPlanRow(
    item: SwitchItem,
    action: SwitchAction,
    onChange: (SwitchAction) -> Unit
) {
    val allowTurnOn = item != SwitchItem.SCREEN_RECORDING
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = labelFor(item),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        )
        TriStateSegment(
            current = action,
            allowTurnOn = allowTurnOn,
            onChange = onChange
        )
    }
}

@Composable
private fun TriStateSegment(
    current: SwitchAction,
    allowTurnOn: Boolean,
    onChange: (SwitchAction) -> Unit
) {
    // Order is fixed; we render every segment with identical sizing.
    val options = buildList {
        add(SwitchAction.NONE to "不执行")
        add(SwitchAction.TURN_OFF to "关闭")
        if (allowTurnOn) add(SwitchAction.TURN_ON to "开启")
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(20.dp)
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEachIndexed { index, (value, label) ->
            if (index > 0) {
                // Hairline divider keeps the segments visually anchored without
                // adding any horizontal "growth" to the selected button.
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(36.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant)
                )
            }
            SegmentCell(
                label = label,
                selected = current == value,
                onClick = { onChange(value) }
            )
        }
    }
}

@Composable
private fun SegmentCell(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    // Identical layout box for every cell. ONLY the colours change with `selected`.
    val bg = if (selected) WarmOrange else Color.Transparent
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .height(36.dp)
            .width(64.dp)
            .background(bg)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
        )
    }
}

private fun labelFor(item: SwitchItem): String = when (item) {
    SwitchItem.DO_NOT_DISTURB -> "勿扰模式"
    SwitchItem.MUTE -> "静音模式"
    SwitchItem.VIBRATE -> "振动模式"
    SwitchItem.AIRPLANE_MODE -> "飞行模式"
    SwitchItem.WIFI -> "Wi-Fi"
    SwitchItem.BLUETOOTH -> "蓝牙"
    SwitchItem.MOBILE_DATA -> "移动数据"
    SwitchItem.LOCATION -> "定位"
    SwitchItem.FLASHLIGHT -> "手电筒"
    SwitchItem.SCREEN_RECORDING -> "屏幕录制"
    SwitchItem.NFC -> "NFC"
    SwitchItem.POWER_SAVE -> "省电模式"
    SwitchItem.EYE_COMFORT -> "护眼模式"
}
