package cobresun.movieclub.app.club

import cobresun.movieclub.app.club.presentation.workStatusLabels
import kotlin.test.Test
import kotlin.test.assertEquals

class WorkStatusLabelsTest {
    private val listed = listOf(
        Triple("1", "watch", "Watch List"),
        Triple("2", "spooky", "Spooky Season"),
        Triple("3", "watch", "Watch List"),
        Triple("3", "spooky", "Spooky Season"),
    )

    @Test
    fun excludesTheDestinationList() {
        assertEquals(
            mapOf("2" to "In Spooky Season", "3" to "In Spooky Season"),
            workStatusLabels(emptySet(), listed, destinationListId = "watch"),
        )
    }

    @Test
    fun reviewedWinsAndAllListsAreNamed() {
        assertEquals(
            mapOf("1" to "Reviewed", "2" to "In Spooky Season", "3" to "In Watch List, Spooky Season"),
            workStatusLabels(setOf("1"), listed, destinationListId = null),
        )
    }
}
