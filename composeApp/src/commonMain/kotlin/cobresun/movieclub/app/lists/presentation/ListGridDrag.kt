package cobresun.movieclub.app.lists.presentation

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.grid.LazyGridItemInfo
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.toOffset
import cobresun.movieclub.app.core.platform.HapticFeedback
import kotlinx.coroutines.delay

/** Grid key of a list's card: "listId/workId". */
internal fun cardKey(listId: String, workId: String) = "$listId/$workId"

/**
 * Long-press drag-to-reorder for the cards of one list in the Lists grid (web
 * `VueDraggableNext` in `ListItems`). While a card is lifted, [order] is that
 * list's shown work ids in their live order; the screen lays the list out in
 * it and offsets the lifted card by [displacement] so it follows the finger.
 * The Up Next card at the front of a list stays put, and nothing may be
 * dropped in front of it.
 */
class ListGridDrag internal constructor(
    private val gridState: LazyGridState,
    private val haptics: HapticFeedback,
) {
    /** The list being reordered, or null when nothing is lifted. */
    var listId by mutableStateOf<String?>(null)
        private set
    var order by mutableStateOf<List<String>>(emptyList())
        private set
    var draggedId by mutableStateOf<String?>(null)
        private set

    private var startOrder: List<String> = emptyList()
    private var lockedFirst = false
    private var pointer by mutableStateOf(Offset.Zero)
    /** Where the finger sits inside the lifted card. */
    private var grip = Offset.Zero

    internal lateinit var visibleIds: (listId: String) -> List<String>
    internal lateinit var upNextId: () -> String?
    internal lateinit var isPending: (workId: String) -> Boolean
    internal lateinit var onDrop: (listId: String, order: List<String>, droppedId: String) -> Unit

    /** How far the lifted card is drawn from its slot, so it stays under the finger. */
    fun displacement(): Offset {
        val info = draggedInfo() ?: return Offset.Zero
        return pointer - grip - info.offset.toOffset()
    }

    internal fun start(position: Offset) {
        val item = itemAt(position) ?: return
        val (list, work) = parse(item) ?: return
        val ids = visibleIds(list)
        val upNext = upNextId()
        if (work == upNext || isPending(work)) return
        listId = list
        draggedId = work
        order = ids
        startOrder = ids
        lockedFirst = upNext != null && ids.firstOrNull() == upNext
        pointer = position
        grip = position - item.offset.toOffset()
        haptics.performTick()
    }

    internal fun drag(delta: Offset) {
        if (listId == null) return
        pointer += delta
        swapIfOverTarget()
    }

    internal fun end() {
        val list = listId ?: return
        val dropped = draggedId ?: return
        val final = order
        reset()
        if (final != startOrder) {
            haptics.performConfirm()
            onDrop(list, final, dropped)
        }
    }

    internal fun cancel() = reset()

    /** Pixels to scroll this frame while the lifted card is held near an edge. */
    internal fun autoScrollDelta(): Float {
        if (listId == null) return 0f
        val info = gridState.layoutInfo
        val top = info.viewportStartOffset.toFloat()
        val bottom = info.viewportEndOffset.toFloat()
        val edge = (bottom - top) * 0.12f
        return when {
            pointer.y < top + edge -> -((top + edge - pointer.y) / edge).coerceAtMost(1f) * MAX_SCROLL
            pointer.y > bottom - edge -> ((pointer.y - (bottom - edge)) / edge).coerceAtMost(1f) * MAX_SCROLL
            else -> 0f
        }
    }

    internal fun swapIfOverTarget() {
        val list = listId ?: return
        val dragged = draggedId ?: return
        val center = pointer - grip + draggedInfo()?.size?.let { Offset(it.width / 2f, it.height / 2f) }.orZero()
        val target = gridState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            val (l, w) = parse(item) ?: return@firstOrNull false
            l == list && w != dragged &&
                center.x >= item.offset.x && center.x <= item.offset.x + item.size.width &&
                center.y >= item.offset.y && center.y <= item.offset.y + item.size.height
        } ?: return
        val targetId = parse(target)?.second ?: return
        val to = order.indexOf(targetId)
        val from = order.indexOf(dragged)
        if (to < 0 || from < 0 || (lockedFirst && to == 0)) return
        order = order.toMutableList().apply { add(to, removeAt(from)) }
        haptics.performTick()
    }

    private fun reset() {
        listId = null
        draggedId = null
        order = emptyList()
    }

    private fun draggedInfo(): LazyGridItemInfo? {
        val list = listId ?: return null
        val key = cardKey(list, draggedId ?: return null)
        return gridState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == key }
    }

    private fun itemAt(position: Offset): LazyGridItemInfo? =
        gridState.layoutInfo.visibleItemsInfo.firstOrNull { item ->
            parse(item) != null &&
                position.x >= item.offset.x && position.x <= item.offset.x + item.size.width &&
                position.y >= item.offset.y && position.y <= item.offset.y + item.size.height
        }

    private fun parse(item: LazyGridItemInfo): Pair<String, String>? {
        val key = item.key as? String ?: return null
        val slash = key.indexOf('/')
        if (slash <= 0 || key.startsWith("skeleton-")) return null
        return key.substring(0, slash) to key.substring(slash + 1)
    }

    private fun Offset?.orZero() = this ?: Offset.Zero

    private companion object {
        const val MAX_SCROLL = 24f
    }
}

@Composable
fun rememberListGridDrag(
    gridState: LazyGridState,
    haptics: HapticFeedback,
    visibleIds: (listId: String) -> List<String>,
    upNextId: () -> String?,
    isPending: (workId: String) -> Boolean,
    onDrop: (listId: String, order: List<String>, droppedId: String) -> Unit,
): ListGridDrag {
    val drag = remember(gridState, haptics) { ListGridDrag(gridState, haptics) }
    val currentVisible by rememberUpdatedState(visibleIds)
    val currentUpNext by rememberUpdatedState(upNextId)
    val currentPending by rememberUpdatedState(isPending)
    val currentDrop by rememberUpdatedState(onDrop)
    drag.visibleIds = { currentVisible(it) }
    drag.upNextId = { currentUpNext() }
    drag.isPending = { currentPending(it) }
    drag.onDrop = { l, o, d -> currentDrop(l, o, d) }

    // Scroll while a card is held near the top or bottom edge.
    LaunchedEffect(drag, drag.listId) {
        if (drag.listId == null) return@LaunchedEffect
        while (true) {
            val delta = drag.autoScrollDelta()
            if (delta != 0f) {
                gridState.scrollBy(delta)
                drag.swapIfOverTarget()
            }
            delay(16)
        }
    }
    return drag
}

/** Attach to the grid: long-press a card to lift it, drag to move it, release to drop. */
fun Modifier.listGridDrag(drag: ListGridDrag): Modifier = pointerInput(drag) {
    detectDragGesturesAfterLongPress(
        onDragStart = { drag.start(it) },
        onDrag = { change, amount ->
            if (drag.listId != null) change.consume()
            drag.drag(amount)
        },
        onDragEnd = { drag.end() },
        onDragCancel = { drag.cancel() },
    )
}
