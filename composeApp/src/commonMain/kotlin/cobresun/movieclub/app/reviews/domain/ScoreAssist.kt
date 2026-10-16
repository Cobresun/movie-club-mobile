package cobresun.movieclub.app.reviews.domain

import cobresun.movieclub.app.core.domain.WorkType
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** A work the signed-in member has scored, in any of their clubs (`GET /api/member/scores`). */
data class ScoredWork(
    val workId: String,
    val type: WorkType,
    val title: String,
    val imageUrl: String?,
    val externalId: String?,
    val genres: List<String>,
    /** The member's own score, 0–10. */
    val score: Double,
    val clubId: String,
    val clubName: String,
    val clubSlug: String,
    /** ISO-8601; sorts lexicographically. */
    val scoredDate: String,
)

/** The user's verdict on the current comparison, relative to the work being scored. */
enum class ComparisonAnswer { MORE, LESS, SAME, SKIP }

/**
 * Score Assist: converges on a 0–10 suggestion for a target work by asking the user to
 * compare it with works they've already scored, binary-searching their own scale.
 * Port of web `scoreAssistLogic.ts`; similarity uses genre overlap only.
 */
object ScoreAssist {
    /** The entry point stays hidden until the user has this many comparable scored works. */
    const val MIN_SCORED_WORKS = 5
    const val MAX_COMPARISONS = 5
    private const val CONVERGED_BRACKET_WIDTH = 1.0
    private const val POSITION_WEIGHT = 0.3
    private const val RANK_TIEBREAK_WEIGHT = 0.01

    data class Target(val workId: String, val type: WorkType, val externalId: String?, val genres: List<String>)

    data class Result(val suggestedScore: Double, val kind: Kind) {
        enum class Kind { CONVERGED, MATCHED, ABOVE_ALL, BELOW_ALL }
    }

    data class Session(
        val target: Target,
        /** Sorted ascending by score (title tie-break). */
        val candidates: List<ScoredWork>,
        val lo: Double = 0.0,
        val hi: Double = 10.0,
        val lowerWork: ScoredWork? = null,
        val upperWork: ScoredWork? = null,
        val askedWorkIds: Set<String> = emptySet(),
        val comparisons: Int = 0,
        val pivot: ScoredWork? = null,
        val result: Result? = null,
    )

    /**
     * Same-type works the user scored, excluding the target, deduplicated across clubs by
     * external id (most recent score wins), sorted ascending by score.
     */
    fun buildCandidatePool(scores: List<ScoredWork>, target: Target): List<ScoredWork> {
        val seen = mutableSetOf<String>()
        target.externalId?.let { seen += "${target.type}:$it" }
        val pool = mutableListOf<ScoredWork>()
        for (scored in scores.sortedByDescending { it.scoredDate }) {
            if (scored.type != target.type || scored.workId == target.workId || !scored.score.isFinite()) continue
            val key = scored.externalId?.let { "${scored.type}:$it" }
            if (key != null && !seen.add(key)) continue
            pool += scored
        }
        return pool.sortedWith(byScoreThenTitle)
    }

    fun isEligible(pool: List<ScoredWork>) = pool.size >= MIN_SCORED_WORKS

    fun start(target: Target, candidates: List<ScoredWork>): Session =
        withNextStep(Session(target, candidates.sortedWith(byScoreThenTitle)))

    fun answer(session: Session, answer: ComparisonAnswer): Session {
        val pivot = session.pivot ?: return session
        if (session.result != null) return session
        val asked = session.askedWorkIds + pivot.workId
        return when (answer) {
            ComparisonAnswer.MORE -> withNextStep(
                session.copy(lo = pivot.score, lowerWork = pivot, askedWorkIds = asked, comparisons = session.comparisons + 1, pivot = null)
            )
            ComparisonAnswer.LESS -> withNextStep(
                session.copy(hi = pivot.score, upperWork = pivot, askedWorkIds = asked, comparisons = session.comparisons + 1, pivot = null)
            )
            ComparisonAnswer.SAME -> session.copy(
                askedWorkIds = asked,
                pivot = null,
                result = Result(clamp(round2(pivot.score)), Result.Kind.MATCHED),
            )
            ComparisonAnswer.SKIP -> withNextStep(session.copy(askedWorkIds = asked, pivot = null))
        }
    }

    private val byScoreThenTitle = compareBy<ScoredWork>({ it.score }, { it.title })

    private fun withNextStep(session: Session): Session {
        if (session.hi - session.lo <= CONVERGED_BRACKET_WIDTH || session.comparisons >= MAX_COMPARISONS) {
            return finish(session)
        }
        val pivot = selectPivot(session) ?: return finish(session)
        return session.copy(pivot = pivot)
    }

    private fun eligiblePivots(s: Session) = s.candidates.filter { c ->
        if (c.workId in s.askedWorkIds) return@filter false
        val aboveLo = if (s.lowerWork != null) c.score > s.lo else c.score >= s.lo
        val belowHi = if (s.upperWork != null) c.score < s.hi else c.score <= s.hi
        aboveLo && belowHi
    }

    private fun similarity(a: List<String>, b: List<String>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val sa = a.toSet()
        val sb = b.toSet()
        return (sa intersect sb).size.toDouble() / (sa union sb).size
    }

    /** Rank-middle third of the eligible works, scored by similarity plus a bounded positional nudge. */
    private fun selectPivot(s: Session): ScoredWork? {
        val eligible = eligiblePivots(s)
        if (eligible.isEmpty()) return null
        val start = eligible.size / 3
        val end = max(start + 1, ceil(2.0 * eligible.size / 3).toInt())
        val midpoint = (s.lo + s.hi) / 2
        val windowMiddle = (start + end - 1) / 2.0
        var maxScoreDistance = 0.0
        var maxRankDistance = 0.0
        for (i in start until end) {
            maxScoreDistance = max(maxScoreDistance, abs(eligible[i].score - midpoint))
            maxRankDistance = max(maxRankDistance, abs(i - windowMiddle))
        }
        var best: ScoredWork? = null
        var bestScore = Double.NEGATIVE_INFINITY
        for (i in start until end) {
            val c = eligible[i]
            val proximity = if (maxScoreDistance == 0.0) 1.0 else 1 - abs(c.score - midpoint) / maxScoreDistance
            val centrality = if (maxRankDistance == 0.0) 1.0 else 1 - abs(i - windowMiddle) / maxRankDistance
            val combined = similarity(s.target.genres, c.genres) + POSITION_WEIGHT * (proximity + RANK_TIEBREAK_WEIGHT * centrality)
            if (combined > bestScore) {
                best = c
                bestScore = combined
            }
        }
        return best
    }

    private fun finish(s: Session): Session {
        val poolMax = s.candidates.lastOrNull()
        val poolMin = s.candidates.firstOrNull()
        val result = when {
            s.lowerWork != null && s.upperWork == null && s.lowerWork.score == poolMax?.score ->
                Result(clamp(round2(s.lo + 0.5)), Result.Kind.ABOVE_ALL)
            s.upperWork != null && s.lowerWork == null && s.upperWork.score == poolMin?.score ->
                Result(clamp(round2(s.hi - 0.5)), Result.Kind.BELOW_ALL)
            else -> Result(clamp(round2((s.lo + s.hi) / 2)), Result.Kind.CONVERGED)
        }
        return s.copy(pivot = null, result = result)
    }

    private fun round2(v: Double) = (v * 100).roundToInt() / 100.0
    private fun clamp(v: Double) = min(10.0, max(0.0, v))
}
