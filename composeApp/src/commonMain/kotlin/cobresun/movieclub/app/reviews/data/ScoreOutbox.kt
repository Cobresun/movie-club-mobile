package cobresun.movieclub.app.reviews.data

import cobresun.movieclub.app.core.data.OfflineStore
import cobresun.movieclub.app.core.data.isConnectivityFailure
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.reviews.domain.MemberScore
import cobresun.movieclub.app.reviews.domain.Review
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

/** A score the member set while offline, waiting to be sent. */
@Serializable
data class PendingScore(
    val clubSlug: String,
    val workId: String,
    val userId: String,
    val score: Double,
)

/**
 * Scores set while offline, kept on disk and sent when the connection returns.
 *
 * Only score *sets* are queued: sending one is idempotent (the sender checks the member's
 * current score on the server, then creates or updates it), and a newer set for the same
 * work replaces the queued one. Destructive or order-dependent writes (deletes, moves,
 * reorders) are never queued; they fail at once while offline.
 */
class ScoreOutbox(private val store: OfflineStore) {
    private val mutex = Mutex()

    suspend fun pending(): List<PendingScore> = mutex.withLock { read() }

    /** Queues [score], replacing an earlier queued score for the same work and member. */
    suspend fun put(score: PendingScore) = mutex.withLock {
        write(read().filterNot { it.sameTarget(score) } + score)
    }

    /** Drops the queued score for this work (a direct save or delete superseded it). */
    suspend fun remove(clubSlug: String, workId: String) = mutex.withLock {
        val all = read()
        val kept = all.filterNot { it.clubSlug == clubSlug && it.workId == workId }
        if (kept.size != all.size) write(kept)
    }

    /**
     * Sends every queued score with [send], in the order they were set. Ones that fail for
     * connectivity reasons stay queued for the next reconnect; ones the server refuses
     * (the work was removed, the member left the club) are dropped, since retrying can't
     * help. A score re-set while its send was in flight stays queued.
     *
     * @return the scores that reached the server.
     */
    suspend fun sendAll(send: suspend (PendingScore) -> Result<Unit, DataError.Remote>): List<PendingScore> {
        val sent = mutableListOf<PendingScore>()
        for (score in pending()) {
            val result = send(score)
            if (result is Result.Error && result.error.isConnectivityFailure()) continue
            if (result is Result.Success) sent += score
            mutex.withLock { write(read().filterNot { it == score }) }
        }
        return sent
    }

    private suspend fun read(): List<PendingScore> = store.load(KEY, serializer).orEmpty()

    private suspend fun write(scores: List<PendingScore>) {
        if (scores.isEmpty()) store.remove(KEY) else store.save(KEY, serializer, scores)
    }

    private fun PendingScore.sameTarget(other: PendingScore) =
        clubSlug == other.clubSlug && workId == other.workId && userId == other.userId

    private companion object {
        const val KEY = "outbox/scores"
        val serializer = ListSerializer(PendingScore.serializer())
    }
}

/** [reviews] as the member will see them once [pending] scores are sent. */
fun applyPendingScores(reviews: List<Review>, pending: List<PendingScore>): List<Review> {
    if (pending.isEmpty()) return reviews
    val byWork = pending.associateBy { it.workId }
    return reviews.map { review ->
        val score = byWork[review.work.id] ?: return@map review
        val existing = review.scoreOf(score.userId)
        review.withScore(
            score.userId,
            MemberScore(existing?.id ?: MemberScore.PENDING_ID, score.userId, score.score, existing?.createdAt),
        )
    }
}
