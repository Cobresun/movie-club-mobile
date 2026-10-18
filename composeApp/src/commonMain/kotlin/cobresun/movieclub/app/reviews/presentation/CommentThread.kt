package cobresun.movieclub.app.reviews.presentation

import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.onError
import cobresun.movieclub.app.core.domain.onSuccess
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.reviews.domain.ReviewComment
import cobresun.movieclub.app.reviews.domain.ReviewCommentsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.time.Clock

/** The comments under a detail sheet. */
data class CommentsState(
    val comments: AsyncResult<List<ReviewComment>> = AsyncResult.Loading,
    val isPosting: Boolean = false,
)

/**
 * Loads and edits the open detail sheet's comment thread, for Reviews and Lists:
 * an optimistic add (replaced by the refetch), edit, and delete with rollback.
 * A refetch that fails keeps the comments already shown.
 */
internal class CommentThread(
    private val scope: CoroutineScope,
    private val repository: ReviewCommentsRepository,
    private val clubSlug: String,
    /** The open sheet's work and its comments, or null when no sheet is open. */
    private val open: () -> Pair<String, CommentsState>?,
    private val me: () -> Member?,
    /** Applies [transform] to [workId]'s comments if its sheet is still open. */
    private val update: (workId: String, transform: (CommentsState) -> CommentsState) -> Unit,
    private val onError: (DataError.Remote, action: String) -> Unit,
) {
    private var tempCount = 0

    fun load(workId: String) {
        scope.launch {
            val result = repository.getComments(clubSlug, workId)
            update(workId) {
                it.copy(
                    comments = when (result) {
                        is Result.Success -> AsyncResult.Success(result.data)
                        is Result.Error -> if (it.comments is AsyncResult.Success) it.comments
                        else AsyncResult.Error(dataError = result.error)
                    }
                )
            }
        }
    }

    fun retry() {
        val workId = open()?.first ?: return
        update(workId) { it.copy(comments = AsyncResult.Loading) }
        load(workId)
    }

    fun add(content: String, spoiler: Boolean) {
        val workId = open()?.first ?: return
        if (content.isBlank()) return
        // Optimistic copy (web does the same), replaced by the refetch.
        val temp = me()?.let {
            ReviewComment(
                id = "temp-${++tempCount}",
                workId = workId,
                userId = it.id,
                userName = it.name,
                userImageUrl = it.imageUrl,
                content = content.trim(),
                createdDate = null,
                spoiler = spoiler,
                createdAt = Clock.System.now().toString(),
            )
        }
        if (temp != null) updateList(workId) { it + temp } else update(workId) { it.copy(isPosting = true) }
        scope.launch {
            repository.addComment(clubSlug, workId, content, spoiler)
                .onSuccess { load(workId) }
                .onError { error ->
                    temp?.let { t -> updateList(workId) { list -> list.filter { it.id != t.id } } }
                    onError(error, "post your comment")
                }
            update(workId) { it.copy(isPosting = false) }
        }
    }

    fun edit(commentId: String, content: String, spoiler: Boolean) {
        val workId = open()?.first ?: return
        scope.launch {
            repository.editComment(clubSlug, workId, commentId, content, spoiler)
                .onSuccess { load(workId) }
                .onError { onError(it, "edit your comment") }
        }
    }

    fun delete(commentId: String) {
        val (workId, before) = open() ?: return
        updateList(workId) { list -> list.filter { it.id != commentId } }
        scope.launch {
            repository.deleteComment(clubSlug, workId, commentId).onError { error ->
                update(workId) { it.copy(comments = before.comments) }
                onError(error, "delete your comment")
            }
        }
    }

    private fun updateList(workId: String, transform: (List<ReviewComment>) -> List<ReviewComment>) {
        update(workId) { s ->
            (s.comments as? AsyncResult.Success)?.let { s.copy(comments = AsyncResult.Success(transform(it.data))) } ?: s
        }
    }
}
