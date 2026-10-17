package cobresun.movieclub.app.lists

import androidx.lifecycle.SavedStateHandle
import cobresun.movieclub.app.club.domain.ApiError
import cobresun.movieclub.app.club.domain.ClubFeatures
import cobresun.movieclub.app.club.domain.ClubRepository
import cobresun.movieclub.app.club.domain.InviteInfo
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.Club
import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.NewWork
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.domain.WorkType
import cobresun.movieclub.app.lists.domain.ListedWork
import cobresun.movieclub.app.lists.domain.CollapsedListsStore
import cobresun.movieclub.app.core.domain.WorkDetails
import cobresun.movieclub.app.core.domain.WorkDetailsRepository
import cobresun.movieclub.app.reviews.domain.ReviewComment
import cobresun.movieclub.app.reviews.domain.ReviewCommentsRepository
import cobresun.movieclub.app.lists.domain.ListsRepository
import cobresun.movieclub.app.lists.domain.WorkList
import cobresun.movieclub.app.lists.presentation.ListsAction
import cobresun.movieclub.app.lists.presentation.ListsEvent
import cobresun.movieclub.app.lists.presentation.ListsViewModel
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.member.domain.MemberRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ListItemRetryTest {
    private val dispatcher = StandardTestDispatcher()

    @BeforeTest fun setUp() = Dispatchers.setMain(dispatcher)
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun work(id: String) = Work(id, WorkType.MOVIE, "Title $id", "", null, null, null, null, null)

    private class FakeLists(val items: MutableMap<String, MutableList<Work>>) : ListsRepository {
        var fail = true
        val removeCalls = mutableListOf<Pair<String, String>>()
        val moveCalls = mutableListOf<Triple<String, String, String>>()
        private fun <T> ok(v: T): Result<T, DataError.Remote> = Result.Success(v)
        override suspend fun getLists(clubSlug: String) = ok(items.keys.map { WorkList(it, "List $it", items.getValue(it).size) })
        override suspend fun getItems(clubSlug: String, listId: String) = ok(items.getValue(listId).toList())
        override suspend fun createList(clubSlug: String, title: String) = ok(WorkList("n", title, 0))
        override suspend fun renameList(clubSlug: String, listId: String, title: String) = ok(Unit)
        override suspend fun deleteList(clubSlug: String, listId: String) = ok(Unit)
        override suspend fun reorderLists(clubSlug: String, listIds: List<String>) = ok(Unit)
        override suspend fun getAllItems(clubSlug: String) = ok(emptyList<ListedWork>())
        override suspend fun addItem(clubSlug: String, listId: String, work: NewWork) = ok(Unit)
        override suspend fun removeItem(clubSlug: String, listId: String, workId: String): Result<Unit, DataError.Remote> {
            removeCalls += listId to workId
            if (fail) return Result.Error(DataError.Remote.NO_INTERNET)
            items.getValue(listId).removeAll { it.id == workId }
            return ok(Unit)
        }
        val reorderCalls = mutableListOf<Pair<String, List<String>>>()
        override suspend fun reorderItems(clubSlug: String, listId: String, workIds: List<String>): Result<Unit, DataError.Remote> {
            reorderCalls += listId to workIds
            return ok(Unit)
        }
        override suspend fun moveItem(clubSlug: String, listId: String, workId: String, destinationListId: String): Result<Unit, DataError.Remote> {
            moveCalls += Triple(listId, workId, destinationListId)
            return if (fail) Result.Error(DataError.Remote.NO_INTERNET) else ok(Unit)
        }
        override suspend fun getReviewsListId(clubSlug: String) = ok("reviews")
        override suspend fun getUpNext(clubSlug: String) = ok<String?>(null)
        override suspend fun setUpNext(clubSlug: String, workId: String?) = ok(Unit)
    }

    private object Members : MemberRepository {
        override suspend fun getMember(): Result<Member, DataError.Remote> = Result.Error(DataError.Remote.UNKNOWN)
        override suspend fun getClubs(): Result<List<Club>, DataError.Remote> = Result.Success(emptyList())
        override suspend fun createClub(name: String, memberEmail: String, type: ClubType): Result<String, DataError.Remote> =
            Result.Error(DataError.Remote.UNKNOWN)
    }

    private object Clubs : ClubRepository {
        private val no = Result.Error(DataError.Remote.UNKNOWN)
        override suspend fun getMembers(clubSlug: String): Result<List<Member>, DataError.Remote> = Result.Success(emptyList())
        override suspend fun generateInviteToken(clubSlug: String): Result<String, DataError.Remote> = no
        override suspend fun leaveClub(clubSlug: String): Result<Unit, DataError.Remote> = no
        override suspend fun removeMember(clubSlug: String, memberId: String): Result<Unit, DataError.Remote> = no
        override suspend fun updateName(clubSlug: String, name: String): Result<Unit, ApiError> = throw NotImplementedError()
        override suspend fun updateSlug(clubSlug: String, newSlug: String): Result<String, ApiError> = throw NotImplementedError()
        override suspend fun getFeatures(clubSlug: String): Result<ClubFeatures, DataError.Remote> = no
        override suspend fun updateFeatures(clubSlug: String, awards: Boolean?, discussionQuestions: Boolean?): Result<ClubFeatures, DataError.Remote> = no
        override suspend fun getInviteInfo(token: String): Result<InviteInfo, ApiError> = throw NotImplementedError()
        override suspend fun joinClub(token: String): Result<Unit, ApiError> = throw NotImplementedError()
    }

    private object NoDetails : WorkDetailsRepository {
        override suspend fun getWorkDetails(clubSlug: String, workId: String): Result<WorkDetails?, DataError.Remote> = Result.Success(null)
    }

    private object NoComments : ReviewCommentsRepository {
        private val ok = Result.Success(Unit)
        override suspend fun getComments(clubSlug: String, workId: String): Result<List<ReviewComment>, DataError.Remote> = Result.Success(emptyList())
        override suspend fun addComment(clubSlug: String, workId: String, content: String, spoiler: Boolean) = ok
        override suspend fun editComment(clubSlug: String, workId: String, commentId: String, content: String, spoiler: Boolean) = ok
        override suspend fun deleteComment(clubSlug: String, workId: String, commentId: String) = ok
    }

    private object NoCollapsed : CollapsedListsStore {
        override suspend fun get(clubSlug: String) = emptySet<String>()
        override suspend fun set(clubSlug: String, listIds: Set<String>) = Unit
    }

    private fun TestScope.setUpViewModel(repo: FakeLists): ListsViewModel {
        val vm = ListsViewModel(SavedStateHandle(mapOf("clubSlug" to "club")), repo, Members, Clubs, NoCollapsed, NoDetails, NoComments)
        advanceUntilIdle()
        return vm
    }

    private fun ListsViewModel.items(listId: String) =
        (state.value.itemsByList[listId] as? AsyncResult.Success)?.data?.map { it.id }

    @Test
    fun failedRemoveRollsBackAndRetryTargetsTheOriginalList() = runTest(dispatcher) {
        val repo = FakeLists(mutableMapOf("a" to mutableListOf(work("1"), work("2")), "b" to mutableListOf(work("3"))))
        val vm = setUpViewModel(repo)
        val event = async { vm.events.first { it is ListsEvent.Message } as ListsEvent.Message }

        vm.onAction(ListsAction.RemoveWork(work("1"), "a"))
        advanceUntilIdle()
        assertEquals(listOf("1", "2"), vm.items("a"), "optimistic removal is rolled back")
        val retry = assertNotNull(event.await().message.onAction, "failure offers Retry")

        repo.fail = false
        retry()
        advanceUntilIdle()

        assertEquals(listOf("a" to "1", "a" to "1"), repo.removeCalls, "retry replays the remove on list a")
        assertEquals(listOf("2"), vm.items("a"))
        assertEquals(listOf("3"), vm.items("b"))
    }

    @Test
    fun failedMoveRetryReplaysSourceAndDestination() = runTest(dispatcher) {
        val repo = FakeLists(mutableMapOf("a" to mutableListOf(work("1")), "b" to mutableListOf(), "c" to mutableListOf()))
        val vm = setUpViewModel(repo)
        val event = async { vm.events.first { it is ListsEvent.Message } as ListsEvent.Message }

        vm.onAction(ListsAction.MoveWork(work("1"), "a", "b"))
        advanceUntilIdle()
        assertEquals(listOf("1"), vm.items("a"))
        val retry = assertNotNull(event.await().message.onAction)

        repo.fail = false
        retry()
        advanceUntilIdle()

        assertTrue(repo.moveCalls.all { it == Triple("a", "1", "b") } && repo.moveCalls.size == 2)
    }

    @Test
    fun settingUpNextMovesTheWorkToTheFrontOfItsList() = runTest(dispatcher) {
        val repo = FakeLists(mutableMapOf("a" to mutableListOf(work("1"), work("2"), work("3")), "b" to mutableListOf(work("4"))))
        repo.fail = false
        val vm = setUpViewModel(repo)

        vm.onAction(ListsAction.SetUpNext(work("3")))
        advanceUntilIdle()

        assertEquals(listOf("3", "1", "2"), vm.items("a"))
        assertEquals(listOf("a" to listOf("3", "1", "2")), repo.reorderCalls)
        assertEquals("3", vm.state.value.upNextWorkId)
    }

    @Test
    fun droppingAFilteredCardKeepsHiddenItemsInPlace() = runTest(dispatcher) {
        val repo = FakeLists(mutableMapOf("a" to mutableListOf(work("1"), work("2"), work("3"), work("4"))))
        val vm = setUpViewModel(repo)

        vm.onAction(ListsAction.Reorder("a", listOf("4", "2"), droppedId = "4"))
        advanceUntilIdle()

        assertEquals(listOf("1", "4", "3", "2"), vm.items("a"))
        assertEquals(listOf("a" to listOf("4", "2")), repo.reorderCalls)
    }

    private fun <T> TestScope.async(block: suspend () -> T) =
        kotlinx.coroutines.CompletableDeferred<T>().also { d -> launch { d.complete(block()) } }
}
