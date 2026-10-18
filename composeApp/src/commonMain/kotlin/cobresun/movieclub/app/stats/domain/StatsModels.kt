package cobresun.movieclub.app.stats.domain

import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.member.domain.Member
import kotlin.math.abs

/** Headline numbers for the top of the statistics page. */
data class StatsSummary(
    val workCount: Int,
    val scoreCount: Int,
    /** Mean of the club averages across scored works. */
    val clubAverage: Double?,
    /** Total runtime of reviewed movies (0 for book clubs). */
    val runtimeMinutes: Int,
    /** Total pages of reviewed books (0 for movie clubs). */
    val pageCount: Int,
)

/** A single work highlighted by a stat, with the value that earned it. */
data class WorkStat(val work: Work, val value: Double)

data class ClubRecords(
    val highest: WorkStat,
    val lowest: WorkStat,
    /** Widest spread (std dev) of member scores, when any work has 2+ scores. */
    val mostDivisive: WorkStat?,
)

/** Number of each member's scores falling in each whole-number bin 0..10. */
data class ScoreDistribution(val binsByMember: Map<String, List<Int>>)

/** One point on a time-series chart, positioned by epoch milliseconds. */
data class TimePoint(val epochMillis: Long, val value: Double, val label: String)

data class MonthCount(val year: Int, val month: Int, val count: Int)

data class CategoryStat(val name: String, val averageScore: Double, val count: Int)

data class CategoryCount(val name: String, val count: Int)

data class LovedCategories(val mostLoved: List<CategoryStat>, val leastLoved: List<CategoryStat>)

data class PersonStat(
    val name: String,
    val workCount: Int,
    val averageScore: Double,
    val works: List<String>,
    val imageUrl: String?,
)

/** A person credited on a work; the cast endpoint supplies these for actors. */
data class CreditedPerson(val name: String, val imageUrl: String?)

enum class ReviewerTitle { SOFTIE, HATER }

data class LeaderboardEntry(
    val member: Member,
    val averageScore: Double,
    val reviewCount: Int,
    val title: ReviewerTitle?,
)

data class Agreement(val work: Work, val scoreA: Double, val scoreB: Double) {
    val difference: Double get() = abs(scoreA - scoreB)
}

data class PairSimilarity(
    val memberA: Member,
    val memberB: Member,
    /** 0–100: 100 means identical scores on every shared work. */
    val similarityPercent: Double,
    val avgDifference: Double,
    val sharedCount: Int,
    val bestAgreements: List<Agreement>,
    val worstAgreements: List<Agreement>,
)

data class TasteSimilarity(val mostSimilar: PairSimilarity?, val leastSimilar: PairSimilarity?)

data class NamedScore(val member: Member, val score: Double)

data class ConsensusEntry(
    val work: Work,
    val average: Double,
    val stdDev: Double,
    val scores: List<NamedScore>,
)

data class Consensus(val mostAgreed: List<ConsensusEntry>, val mostDivisive: List<ConsensusEntry>)

data class OutlierWork(val work: Work, val memberScore: Double, val clubAverage: Double, val difference: Double)

data class MemberOutliers(val member: Member, val works: List<OutlierWork>)

data class DeviationEntry(val work: Work, val clubScore: Double, val externalScore: Double, val deviation: Double)

data class ExternalDeviation(val clubRatedHigher: List<DeviationEntry>, val clubRatedLower: List<DeviationEntry>)

/** Most- and least-watched genres, five each. */
data class GenreWatchCounts(val mostWatched: List<CategoryCount>, val leastWatched: List<CategoryCount>)

/** The best-scored work of one review year (UTC). */
data class YearBest(val year: Int, val work: Work, val average: Double, val workCount: Int)
