package cobresun.movieclub.app.club

import androidx.lifecycle.SavedStateHandle
import cobresun.movieclub.app.club.presentation.ClubTab
import cobresun.movieclub.app.club.presentation.ClubViewModel
import cobresun.movieclub.app.core.domain.LastClubStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Which tab a club opens on: restored, then the route's, then the one last used there. */
@OptIn(ExperimentalCoroutinesApi::class)
class ClubViewModelTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private class Store(private val section: String?) : LastClubStore {
        override suspend fun get(): String? = null
        override suspend fun set(slug: String) = Unit
        override suspend fun getSection(slug: String): String? = section
        override suspend fun setSection(slug: String, section: String) = Unit
    }

    @Test
    fun routeTabWinsOverTheRememberedOne() = runTest(dispatcher) {
        val vm = ClubViewModel("club", ClubTab.STATS, SavedStateHandle(), Store(ClubTab.LISTS.name))
        advanceUntilIdle()
        assertTrue(vm.tabResolved.value)
        assertEquals(ClubTab.STATS, vm.selectedTab.value)
    }

    @Test
    fun withoutARouteTabTheRememberedOneIsRestored() = runTest(dispatcher) {
        val vm = ClubViewModel("club", null, SavedStateHandle(), Store(ClubTab.LISTS.name))
        assertEquals(false, vm.tabResolved.value)
        advanceUntilIdle()
        assertTrue(vm.tabResolved.value)
        assertEquals(ClubTab.LISTS, vm.selectedTab.value)
    }

    @Test
    fun firstVisitOpensReviews() = runTest(dispatcher) {
        val vm = ClubViewModel("club", null, SavedStateHandle(), Store(null))
        advanceUntilIdle()
        assertTrue(vm.tabResolved.value)
        assertEquals(ClubTab.REVIEWS, vm.selectedTab.value)
    }

    @Test
    fun aTabChosenWhileRestoringIsKept() = runTest(dispatcher) {
        val vm = ClubViewModel("club", null, SavedStateHandle(), Store(ClubTab.LISTS.name))
        vm.selectTab(ClubTab.AWARDS)
        advanceUntilIdle()
        assertEquals(ClubTab.AWARDS, vm.selectedTab.value)
    }

    @Test
    fun restoredStateBeatsTheRouteTab() = runTest(dispatcher) {
        val vm = ClubViewModel("club", ClubTab.STATS, SavedStateHandle(mapOf(ClubViewModel.KEY_TAB to ClubTab.LISTS)), Store(null))
        advanceUntilIdle()
        assertEquals(ClubTab.LISTS, vm.selectedTab.value)
    }
}
