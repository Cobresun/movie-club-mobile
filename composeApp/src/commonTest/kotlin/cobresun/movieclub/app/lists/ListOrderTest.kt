package cobresun.movieclub.app.lists

import cobresun.movieclub.app.lists.domain.withFirst
import cobresun.movieclub.app.lists.domain.withSubsetReordered
import kotlin.test.Test
import kotlin.test.assertEquals

class ListOrderTest {
    private val full = listOf("a", "b", "c", "d", "e")

    @Test
    fun reorderingEverythingReplacesTheOrder() {
        assertEquals(listOf("c", "a", "b", "d", "e"), full.withSubsetReordered(listOf("c", "a", "b", "d", "e")) { it })
    }

    @Test
    fun filteredReorderOnlyShufflesTheSubsetSlots() {
        // b and d are visible (filtered); dragging d before b swaps their slots.
        assertEquals(listOf("a", "d", "c", "b", "e"), full.withSubsetReordered(listOf("d", "b")) { it })
    }

    @Test
    fun unknownKeysAreIgnored() {
        assertEquals(full, full.withSubsetReordered(listOf("x")) { it })
    }

    @Test
    fun withFirstMovesTheWorkToTheFront() {
        assertEquals(listOf("d", "a", "b", "c", "e"), full.withFirst("d") { it })
        assertEquals(full, full.withFirst("z") { it })
    }
}
