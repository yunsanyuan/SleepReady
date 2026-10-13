package com.example.sleepready.ui.inbox.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.sleepready.domain.model.KnowledgeItem
import com.example.sleepready.domain.model.KnowledgeStatus
import com.example.sleepready.ui.theme.StatusDoneColor
import com.example.sleepready.ui.theme.StatusIdleColor
import com.example.sleepready.ui.theme.StatusPausedColor
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 心动行的滑动交互（v7，按状态决定方向 + 1/3 阈值）。
 *
 * 三态状态机：IDLE → PAUSED → DONE。
 *  - IDLE：仅可右滑，松手过阈值 → 进入 PAUSED；左滑被锚点禁止
 *  - PAUSED：左右皆可，左滑 → IDLE，右滑 → DONE
 *  - DONE：仅可左滑，松手过阈值 → 回到 PAUSED；右滑被锚点禁止
 *
 * 阈值：滑过 [REVEAL_WIDTH_DP] 的 1/3 即触发切换；松手未过阈值则弹回原位。
 *
 * 与归档分离：归档由长按菜单的「删除」入口承担，不再通过滑动手势触发。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SwipeableRow(
    item: KnowledgeItem,
    onMarkDone: (KnowledgeItem) -> Unit, // 兼容外部签名（未使用）
    onSetStatus: (KnowledgeItem, KnowledgeStatus) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val revealPx = with(density) { REVEAL_WIDTH_DP.dp.toPx() }

    val anchors = remember(item.status) {
        DraggableAnchors<SwipeAnchor> {
            SwipeAnchor.Closed at 0f
            when (item.status) {
                KnowledgeStatus.IDLE -> {
                    SwipeAnchor.OpenRight at revealPx
                }
                KnowledgeStatus.PAUSED -> {
                    SwipeAnchor.OpenLeft at -revealPx
                    SwipeAnchor.OpenRight at revealPx
                }
                KnowledgeStatus.DONE -> {
                    SwipeAnchor.OpenLeft at -revealPx
                }
            }
        }
    }

    val dragState = remember(item.id, item.status) {
        AnchoredDraggableState(
            initialValue = SwipeAnchor.Closed,
            anchors = anchors,
            positionalThreshold = { distance -> distance / 3f },
            velocityThreshold = { with(density) { 320.dp.toPx() } },
            animationSpec = tween(durationMillis = 220)
        )
    }

    LaunchedEffect(dragState, item.id, item.status) {
        snapshotFlow { dragState.currentValue }
            .distinctUntilChanged()
            .collectLatest { settled ->
                val next: KnowledgeStatus? = when (item.status) {
                    KnowledgeStatus.IDLE -> if (settled == SwipeAnchor.OpenRight) KnowledgeStatus.PAUSED else null
                    KnowledgeStatus.PAUSED -> when (settled) {
                        SwipeAnchor.OpenLeft -> KnowledgeStatus.IDLE
                        SwipeAnchor.OpenRight -> KnowledgeStatus.DONE
                        else -> null
                    }
                    KnowledgeStatus.DONE -> if (settled == SwipeAnchor.OpenLeft) KnowledgeStatus.PAUSED else null
                }
                if (next != null) {
                    onSetStatus(item, next)
                    // 状态变化会触发 anchors 重建，dragState re-init 自动归零
                }
            }
    }

    val offsetPx = runCatching { dragState.requireOffset() }.getOrDefault(0f)

    Box(modifier = modifier.fillMaxWidth()) {
        SwipeRevealHint(
            offsetPx = offsetPx,
            revealPx = revealPx,
            currentStatus = item.status
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetPx.toInt(), 0) }
                .anchoredDraggable(
                    state = dragState,
                    orientation = Orientation.Horizontal
                )
        ) {
            content()
        }
    }
}

@Composable
private fun SwipeRevealHint(
    offsetPx: Float,
    revealPx: Float,
    currentStatus: KnowledgeStatus
) {
    if (offsetPx == 0f) return
    val rightSwipe = offsetPx > 0f
    val targetStatus = when {
        rightSwipe && currentStatus == KnowledgeStatus.IDLE -> KnowledgeStatus.PAUSED
        rightSwipe && currentStatus == KnowledgeStatus.PAUSED -> KnowledgeStatus.DONE
        !rightSwipe && currentStatus == KnowledgeStatus.PAUSED -> KnowledgeStatus.IDLE
        !rightSwipe && currentStatus == KnowledgeStatus.DONE -> KnowledgeStatus.PAUSED
        else -> return
    }
    val color = when (targetStatus) {
        KnowledgeStatus.IDLE -> StatusIdleColor
        KnowledgeStatus.PAUSED -> StatusPausedColor
        KnowledgeStatus.DONE -> StatusDoneColor
    }
    val progress = (kotlin.math.abs(offsetPx) / revealPx).coerceIn(0f, 1f)
    val alpha by animateFloatAsState(targetValue = progress, label = "swipeHint")
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(color.copy(alpha = alpha * 0.85f))
            .padding(horizontal = 24.dp),
        horizontalArrangement = if (rightSwipe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (rightSwipe) "→ ${targetStatus.displayName}" else "${targetStatus.displayName} ←",
                color = androidx.compose.ui.graphics.Color.White,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

private enum class SwipeAnchor { OpenLeft, Closed, OpenRight }

private const val REVEAL_WIDTH_DP = 160
