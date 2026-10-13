package com.example.sleepready.ui.common

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.collectLatest
import kotlin.math.abs

private const val ITEM_H = 48
private const val VISIBLE = 7
private const val HALF = VISIBLE / 2
private const val WHEEL_H = ITEM_H * VISIBLE

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun <T> WheelPicker(
    items: List<T>,
    selectedIndex: Int,
    onSelectedChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemFormatter: (T) -> String = { it.toString() }
) {
    val idx = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = idx)
    val snap = rememberSnapFlingBehavior(lazyListState = listState)

    val centerIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)) }
    }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex in items.indices && centerIndex != selectedIndex) {
            listState.scrollToItem(selectedIndex)
        }
    }

    LaunchedEffect(listState, items) {
        snapshotFlow { listState.isScrollInProgress to centerIndex }
            .collectLatest { (scrolling, idx2) ->
                if (!scrolling && idx2 != selectedIndex && idx2 in items.indices) {
                    onSelectedChange(idx2)
                }
            }
    }

    val bg = MaterialTheme.colorScheme.background

    Box(modifier = modifier.height(WHEEL_H.dp), contentAlignment = Alignment.Center) {
        // Top gradient
        Box(
            Modifier.fillMaxWidth().height((HALF * ITEM_H).dp).align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(bg, bg.copy(alpha = 0f))))
        )
        // Bottom gradient
        Box(
            Modifier.fillMaxWidth().height((HALF * ITEM_H).dp).align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(bg.copy(alpha = 0f), bg)))
        )
        // Center highlight
        Box(
            Modifier.fillMaxWidth(0.88f).height(ITEM_H.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0f)
                        )
                    )
                )
        )

        LazyColumn(
            state = listState,
            flingBehavior = snap,
            contentPadding = PaddingValues(vertical = (HALF * ITEM_H).dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(items.size) { index ->
                val dist = abs(index - centerIndex)
                val isCenter = dist == 0
                val itemAlpha = (1f - dist * 0.16f).coerceAtLeast(0.3f)

                Box(Modifier.fillMaxWidth().height(ITEM_H.dp), contentAlignment = Alignment.Center) {
                    Text(
                        text = itemFormatter(items[index]),
                        fontSize = if (isCenter) 22.sp else (17f - dist).coerceAtLeast(12f).sp,
                        fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCenter) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = itemAlpha),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.alpha(itemAlpha)
                    )
                }
            }
        }
    }
}

const val ITEM_HEIGHT_DP = ITEM_H
const val VISIBLE_ITEM_COUNT = VISIBLE
const val PADDING_ITEMS = HALF
const val WHEEL_HEIGHT_DP = WHEEL_H
