package cobresun.movieclub.app.reviews.data

import cobresun.movieclub.app.core.data.OfflineStore
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.domain.WorkType
import cobresun.movieclub.app.reviews.domain.MemberScore
import cobresun.movieclub.app.reviews.domain.Review
import kotlinx.coroutines.test.runTest
import okio.FileSystem
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScoreOutboxTest {
    private val directory = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "score-outbox-test-${Random.nextLong()}"
    private val store = OfflineStore(directory)
    private val outbox = ScoreOutbox(store)

    @AfterTest
    fun cleanUp() {
        FileSystem.SYSTEM.deleteRecursively(directory)
    }

    private fun score(work: String, value: Double, club: String = "c") = PendingScore(club, work, "me", value)

    @Test
    fun newerScoreForTheSameWorkReplacesTheQueuedOne() = runTest {
        outbox.put(score("w1", 6.0))
        outbox.put(score("w2", 4.0))
        outbox.put(score("w1", 8.5))
        assertEquals(listOf(score("w2", 4.0), score("w1", 8.5)), outbox.pending())
    }

    @Test
    fun queueSurvivesARestart() = runTest {
        outbox.put(score("w1", 7.0))
        assertEquals(listOf(score("w1", 7.0)), ScoreOutbox(OfflineStore(directory)).pending())
    }

    @Test
    fun sentScoresLeaveTheQueue() = runTest {
        outbox.put(score("w1", 7.0))
        outbox.put(score("w2", 3.0))
        val sent = outbox.sendAll { Result.Success(Unit) }
        assertEquals(2, sent.size)
        assertTrue(outbox.pending().isEmpty())
    }

    @Test
    fun stillOfflineKeepsThemQueued() = runTest {
        outbox.put(score("w1", 7.0))
        val sent = outbox.sendAll { Result.Error(DataError.Remote.NO_INTERNET) }
        assertTrue(sent.isEmpty())
        assertEquals(listOf(score("w1", 7.0)), outbox.pending())
    }

    @Test
    fun refusedScoresAreDroppedNotRetriedForever() = runTest {
        outbox.put(score("removed", 7.0))
        outbox.put(score("w2", 5.0))
        val sent = outbox.sendAll { if (it.workId == "removed") Result.Error(DataError.Remote.UNKNOWN) else Result.Success(Unit) }
        assertEquals(listOf(score("w2", 5.0)), sent)
        assertTrue(outbox.pending().isEmpty())
    }

    @Test
    fun scoreChangedWhileSendingStaysQueued() = runTest {
        outbox.put(score("w1", 7.0))
        outbox.sendAll {
            outbox.put(score("w1", 9.0)) // the member changes it while the send is in flight
            Result.Success(Unit)
        }
        assertEquals(listOf(score("w1", 9.0)), outbox.pending())
    }

    @Test
    fun removeForgetsTheWork() = runTest {
        outbox.put(score("w1", 7.0))
        outbox.put(score("w2", 2.0))
        outbox.remove("c", "w1")
        assertEquals(listOf(score("w2", 2.0)), outbox.pending())
    }

    private fun review(id: String, vararg scores: MemberScore) = Review(
        work = Work(id, WorkType.MOVIE, "Title $id", "", null, null, null, null, null),
        scores = scores.associateBy { it.userId },
        average = scores.map { it.value }.takeIf { it.isNotEmpty() }?.average(),
    )

    @Test
    fun pendingScoresShowOnTheirReviews() {
        val reviews = listOf(
            review("w1", MemberScore("s1", "me", 5.0), MemberScore("s2", "them", 7.0)),
            review("w2", MemberScore("s3", "them", 4.0)),
            review("w3"),
        )
        val shown = applyPendingScores(reviews, listOf(score("w1", 9.0), score("w2", 6.0)))

        assertEquals(MemberScore("s1", "me", 9.0), shown[0].scoreOf("me")) // keeps its server id
        assertEquals(8.0, shown[0].average)
        assertEquals(MemberScore.PENDING_ID, shown[1].scoreOf("me")?.id)
        assertEquals(5.0, shown[1].average)
        assertEquals(reviews[2], shown[2])
    }
}
