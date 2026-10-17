package cobresun.movieclub.app.awards.presentation.components

import androidx.compose.animation.BoundsTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateBounds
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import cobresun.movieclub.app.core.platform.HapticFeedback
import org.koin.compose.koinInject

/** What a row needs to render itself inside [ReorderableColumn]. */
class ReorderableItemScope(
    /** Attach to the drag handle (the whole row would fight with scrolling). */
    val handleModifier: Modifier,
    val isDragging: Boolean,
    /** Position in the list as currently displayed (updates live while dragging). */
    val index: Int,
)

/**
 * A short, non-lazy column whose rows can be dragged into a new order by their
 * handle. The order changes locally while dragging; [onMove] fires once, on
 * drop, with the original and final positions. Rows also expose "Move up" /
 * "Move down" accessibility actions.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun <T> ReorderableColumn(
    items: List<T>,
    key: (T) -> Any,
    onMove: (from: Int, to: Int) -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = 8.dp,
    enabled: Boolean = true,
    itemContent: @Composable ReorderableItemScope.(T) -> Unit,
) {
    val haptics = koinInject<HapticFeedback>()
    val spacingPx = with(LocalDensity.current) { spacing.toPx() }
    var order by remember { mutableStateOf(items) }
    var draggingKey by remember { mutableStateOf<Any?>(null) }
    var dragStartIndex by remember { mutableStateOf(-1) }
    var offset by remember { mutableFloatStateOf(0f) }
    val heights = remember { mutableStateMapOf<Any, Int>() }
    if (draggingKey == null && order != items) order = items
    val currentOnMove by rememberUpdatedState(onMove)

    LookaheadScope {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
        order.forEachIndexed { index, item ->
            val itemKey = key(item)
            key(itemKey) {
                val dragging = draggingKey == itemKey
                val elevation by animateDpAsState(if (dragging) 12.dp else 0.dp)
                val handle = if (!enabled) Modifier else Modifier.pointerInput(itemKey) {
                    detectDragGestures(
                        onDragStart = {
                            draggingKey = itemKey
                            dragStartIndex = order.indexOfFirst { key(it) == itemKey }
                            offset = 0f
                            haptics.performLightPulse()
                        },
                        onDrag = { change, amount ->
                            change.consume()
                            offset += amount.y
                            val current = order.indexOfFirst { key(it) == itemKey }
                            // Swap with a neighbour once dragged past half of it.
                            if (offset > 0 && current < order.lastIndex) {
                                val next = heights[key(order[current + 1])] ?: return@detectDragGestures
                                if (offset > next / 2f) {
                                    order = order.toMutableList().apply { add(current + 1, removeAt(current)) }
                                    offset -= next + spacingPx
                                    haptics.performTick()
                                }
                            } else if (offset < 0 && current > 0) {
                                val prev = heights[key(order[current - 1])] ?: return@detectDragGestures
                                if (-offset > prev / 2f) {
                                    order = order.toMutableList().apply { add(current - 1, removeAt(current)) }
                                    offset += prev + spacingPx
                                    haptics.performTick()
                                }
                            }
                        },
                        onDragEnd = {
                            val end = order.indexOfFirst { key(it) == itemKey }
                            val start = dragStartIndex
                            draggingKey = null
                            offset = 0f
                            haptics.performConfirm()
                            if (start >= 0 && end != start) currentOnMove(start, end)
                        },
                        onDragCancel = {
                            draggingKey = null
                            offset = 0f
                            order = items
                        },
                    )
                }
                val rowModifier = Modifier
                    // Neighbours glide into their new slots; the dragged row follows the finger instead.
                    .then(if (dragging) Modifier else Modifier.animateBounds(this@LookaheadScope, boundsTransform = ReorderBounds))
                    .onSizeChanged { heights[itemKey] = it.height }
                    .zIndex(if (dragging) 1f else 0f)
                    .graphicsLayer {
                        translationY = if (dragging) offset else 0f
                        scaleX = if (dragging) 1.02f else 1f
                        scaleY = if (dragging) 1.02f else 1f
                    }
                    .shadow(elevation, MaterialTheme.shapes.medium, clip = false)
                    .semantics {
                        if (enabled) customActions = buildList {
                            if (index > 0) add(CustomAccessibilityAction("Move up") { currentOnMove(index, index - 1); true })
                            if (index < order.lastIndex) add(CustomAccessibilityAction("Move down") { currentOnMove(index, index + 1); true })
                        }
                    }
                Column(rowModifier) {
                    ReorderableItemScope(handle, dragging, index).itemContent(item)
                }
            }
        }
    }
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
private val ReorderBounds = BoundsTransform { _, _ ->
    spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)
}

/** The standard grip icon for [ReorderableItemScope.handleModifier]. */
@Composable
fun DragHandle(modifier: Modifier, contentDescription: String?) {
    Icon(
        Icons.Default.DragIndicator,
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = modifier.size(24.dp),
    )
}
