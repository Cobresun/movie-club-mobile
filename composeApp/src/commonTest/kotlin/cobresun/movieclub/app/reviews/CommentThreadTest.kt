package cobresun.movieclub.app.reviews

import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.reviews.domain.ReviewComment
import cobresun.movieclub.app.reviews.domain.ReviewCommentsRepository
import cobresun.movieclub.app.reviews.presentation.CommentThread
import cobresun.movieclub.app.reviews.presentation.CommentsState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CommentThreadTest {
    private fun comment(id: String) = ReviewComment(id, "w", "u", "Ann", null, "hi", null, spoiler = false)

    private class FakeRepo(var comments: List<ReviewComment>) : ReviewCommentsRepository {
        var addResult: Result<Unit, DataError.Remote> = Result.Success(Unit)
        var deleteResult: Result<Unit, DataError.Remote> = Result.Success(Unit)
        var pendingAdd: CompletableDeferred<Unit>? = null

        override suspend fun getComments(clubSlug: String, workId: String) = Result.Success(comments)
        override suspend fun addComment(clubSlug: String, workId: String, content: String, spoiler: Boolean): Result<Unit, DataError.Remote> {
            pendingAdd?.await()
            return addResult
        }
        override suspend fun editComment(clubSlug: String, workId: String, commentId: String, content: String, spoiler: Boolean) =
            Result.Success(Unit)
        override suspend fun deleteComment(clubSlug: String, workId: String, commentId: String) = deleteResult
    }

    private class Harness(val repo: FakeRepo, scope: kotlinx.coroutines.CoroutineScope) {
        var state = CommentsState()
        val errors = mutableListOf<String>()
        val thread = CommentThread(
            scope = scope,
            repository = repo,
            clubSlug = "club",
            open = { "w" to state },
            me = { Member("u", "a@b.c", "Ann") },
            update = { workId, transform -> if (workId == "w") state = transform(state) },
            onError = { _, action -> errors += action },
        )
    }

    @Test
    fun addShowsTheCommentAtOnceThenTheServersList() = runTest(UnconfinedTestDispatcher()) {
        val repo = FakeRepo(emptyList()).apply { pendingAdd = CompletableDeferred() }
        val h = Harness(repo, backgroundScope)
        h.thread.load("w")

        h.thread.add("hello", spoiler = false)
        assertEquals(listOf("hello"), (h.state.comments as AsyncResult.Success).data.map { it.content })

        repo.comments = listOf(comment("1"))
        repo.pendingAdd!!.complete(Unit)
        assertEquals(listOf("1"), (h.state.comments as AsyncResult.Success).data.map { it.id })
    }

    @Test
    fun failedAddRemovesTheOptimisticComment() = runTest(UnconfinedTestDispatcher()) {
        val repo = FakeRepo(listOf(comment("1"))).apply { addResult = Result.Error(DataError.Remote.NO_INTERNET) }
        val h = Harness(repo, backgroundScope)
        h.thread.load("w")

        h.thread.add("hello", spoiler = false)

        assertEquals(listOf("1"), (h.state.comments as AsyncResult.Success).data.map { it.id })
        assertEquals(listOf("post your comment"), h.errors)
    }

    @Test
    fun failedDeleteRestoresTheComment() = runTest(UnconfinedTestDispatcher()) {
        val repo = FakeRepo(listOf(comment("1"), comment("2"))).apply { deleteResult = Result.Error(DataError.Remote.SERVER) }
        val h = Harness(repo, backgroundScope)
        h.thread.load("w")

        h.thread.delete("1")

        assertEquals(listOf("1", "2"), (h.state.comments as AsyncResult.Success).data.map { it.id })
        assertEquals(listOf("delete your comment"), h.errors)
    }

    @Test
    fun aFailedRefetchKeepsTheShownComments() = runTest(UnconfinedTestDispatcher()) {
        val repo = object : ReviewCommentsRepository by FakeRepo(emptyList()) {
            var fail = false
            override suspend fun getComments(clubSlug: String, workId: String): Result<List<ReviewComment>, DataError.Remote> =
                if (fail) Result.Error(DataError.Remote.NO_INTERNET) else Result.Success(listOf(comment("1")))
        }
        var state = CommentsState()
        val thread = CommentThread(backgroundScope, repo, "club", { "w" to state }, { null }, { _, t -> state = t(state) }, { _, _ -> })
        thread.load("w")
        repo.fail = true

        thread.load("w")

        assertEquals(listOf("1"), (state.comments as AsyncResult.Success).data.map { it.id })
    }
}
