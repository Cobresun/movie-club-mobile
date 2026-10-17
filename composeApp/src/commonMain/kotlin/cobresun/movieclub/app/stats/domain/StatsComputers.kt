package cobresun.movieclub.app.stats.domain

import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.domain.WorkDetails
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.reviews.domain.Review
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.time.Instant

/*
 * Pure statistics over a club's reviews, ported from the web's
 * `features/statistics/statsComputers.ts`. Every function takes the already
 * scored reviews (see [scoredReviews]) so the rules for which works count live
 * in one place, and returns plain data the UI renders as-is.
 */

private const val MIN_CATEGORY_COUNT = 2
private const val MAX_SUBJECTS = 5
private const val MIN_SHARED_REVIEWS = 3
private const val MIN_SCORES_FOR_SPREAD = 2
private const val OUTLIER_THRESHOLD = 2.0
private const val MAX_OUTLIERS_PER_MEMBER = 5

/** Works that count toward statistics: at least one member has scored them. */
fun scoredReviews(reviews: List<Review>): List<Review> =
    reviews.filter { it.scores.isNotEmpty() && it.average != null }

internal fun Double.round2(): Double = (this * 100).roundToInt() / 100.0

private val Review.avg: Double get() = average ?: 0.0
private val Review.scoreValues: List<Double> get() = scores.values.map { it.value }.filterNot { it.isNaN() }

private fun stdDev(values: List<Double>): Double {
    val mean = values.average()
    return sqrt(values.sumOf { (it - mean) * (it - mean) } / values.size)
}

private fun parseInstant(iso: String?): Instant? = iso?.let { runCatching { Instant.parse(it) }.getOrNull() }

private val Review.addedInstant: Instant? get() = parseInstant(work.addedAt)

private val Work.movie: WorkDetails.Movie? get() = details as? WorkDetails.Movie
private val Work.book: WorkDetails.Book? get() = details as? WorkDetails.Book

// --- Summary & records -----------------------------------------------------

fun computeSummary(reviews: List<Review>): StatsSummary = StatsSummary(
    workCount = reviews.size,
    scoreCount = reviews.sumOf { it.scores.size },
    clubAverage = reviews.takeIf { it.isNotEmpty() }?.map { it.avg }?.average()?.round2(),
    runtimeMinutes = reviews.sumOf { it.work.movie?.runtimeMinutes ?: 0 },
    pageCount = reviews.sumOf { it.work.book?.pageCount ?: 0 },
)

/** Best, worst and widest-spread work; null until there are two works to compare. */
fun computeClubRecords(reviews: List<Review>): ClubRecords? {
    if (reviews.size < 2) return null
    val byAverage = reviews.sortedByDescending { it.avg }
    val divisive = reviews
        .filter { it.scoreValues.size >= MIN_SCORES_FOR_SPREAD }
        .map { it to stdDev(it.scoreValues) }
        .maxByOrNull { it.second }
    return ClubRecords(
        highest = WorkStat(byAverage.first().work, byAverage.first().avg.round2()),
        lowest = WorkStat(byAverage.last().work, byAverage.last().avg.round2()),
        mostDivisive = divisive?.let { (r, sd) -> WorkStat(r.work, sd.round2()) },
    )
}

// --- Score charts ------------------------------------------------------------

/** Each member's scores bucketed by whole number (9.5 → 9; 10 → 10). */
fun computeScoreDistribution(reviews: List<Review>, members: List<Member>): ScoreDistribution {
    val bins = members.associate { it.id to IntArray(11) }
    for (review in reviews) {
        for ((userId, score) in review.scores) {
            val bin = floor(score.value).toInt().coerceIn(0, 10)
            bins[userId]?.let { it[bin]++ }
        }
    }
    return ScoreDistribution(bins.mapValues { it.value.toList() })
}

/** Rolling window of 20% of the data, clamped to 5..30 (mirrors the web). */
internal fun rollingWindow(size: Int): Int = max(5, min(30, (size * 0.2).roundToInt()))

/**
 * Per-member rolling average of their scores in the order they gave them.
 * Members with fewer scores than the window have no line.
 */
fun computeScoreTrend(reviews: List<Review>, members: List<Member>): Map<String, List<TimePoint>> {
    val result = linkedMapOf<String, List<TimePoint>>()
    for (member in members) {
        val dated = reviews.mapNotNull { review ->
            val score = review.scores[member.id] ?: return@mapNotNull null
            val at = parseInstant(score.createdAt) ?: return@mapNotNull null
            Triple(at, review.work.title, score.value)
        }.sortedBy { it.first }
        if (dated.isEmpty()) continue
        val window = rollingWindow(dated.size)
        val points = (window - 1 until dated.size).map { i ->
            val avg = dated.subList(i - window + 1, i + 1).map { it.third }.average()
            TimePoint(dated[i].first.toEpochMilliseconds(), avg.round2(), dated[i].second)
        }
        if (points.isNotEmpty()) result[member.id] = points
    }
    return result
}

/** Rolling average of how far apart members' scores are (std dev), over time. */
fun computeScoreSpreadTrend(reviews: List<Review>): List<TimePoint> {
    val perWork = reviews.mapNotNull { review ->
        val values = review.scoreValues
        val at = review.addedInstant
        if (values.size < MIN_SCORES_FOR_SPREAD || at == null) null else Triple(at, review.work.title, stdDev(values))
    }.sortedBy { it.first }
    if (perWork.isEmpty()) return emptyList()
    val window = rollingWindow(perWork.size)
    return (window - 1 until perWork.size).map { i ->
        val avg = perWork.subList(i - window + 1, i + 1).map { it.third }.average()
        TimePoint(perWork[i].first.toEpochMilliseconds(), avg.round2(), perWork[i].second)
    }
}

// --- Activity ----------------------------------------------------------------

/** Reviews per calendar month (UTC), zero-filled between the first and last. */
fun computeMonthlyActivity(reviews: List<Review>): List<MonthCount> {
    val counts = reviews.mapNotNull { it.addedInstant?.toLocalDateTime(TimeZone.UTC) }
        .groupingBy { it.year * 12 + (it.month.ordinal) }
        .eachCount()
    if (counts.isEmpty()) return emptyList()
    return (counts.keys.min()..counts.keys.max()).map { key ->
        MonthCount(year = key / 12, month = key % 12 + 1, count = counts[key] ?: 0)
    }
}

/** Each review year's best-scored work (the first one wins a tie), newest year first. */
fun computeHighestRatedByYear(reviews: List<Review>): List<YearBest> {
    val groups = linkedMapOf<Int, Pair<Review, Int>>()
    for (review in reviews) {
        val year = review.addedInstant?.toLocalDateTime(TimeZone.UTC)?.year ?: continue
        val group = groups[year]
        groups[year] = when {
            group == null -> review to 1
            review.avg > group.first.avg -> review to group.second + 1
            else -> group.first to group.second + 1
        }
    }
    return groups.map { (year, group) -> YearBest(year, group.first.work, group.first.avg.round2(), group.second) }
        .sortedByDescending { it.year }
}

/** Running total of works reviewed, in review order. */
fun computeCumulativeCounts(reviews: List<Review>): List<TimePoint> =
    reviews.mapNotNull { r -> r.addedInstant?.let { it to r.work.title } }
        .sortedBy { it.first }
        .mapIndexed { i, (at, title) -> TimePoint(at.toEpochMilliseconds(), (i + 1).toDouble(), title) }

// --- Categories (genres / subjects) ------------------------------------------

private fun categoryScores(
    reviews: List<Review>,
    categories: (Work) -> List<String>,
    memberId: String?,
): List<CategoryStat> {
    val totals = linkedMapOf<String, Pair<Double, Int>>()
    for (review in reviews) {
        val score = if (memberId != null) review.scores[memberId]?.value else review.average
        if (score == null) continue
        for (c in categories(review.work)) {
            val (sum, n) = totals[c] ?: (0.0 to 0)
            totals[c] = (sum + score) to (n + 1)
        }
    }
    return totals.filter { it.value.second >= MIN_CATEGORY_COUNT }
        .map { (name, v) -> CategoryStat(name, (v.first / v.second).round2(), v.second) }
}

private fun categoryCounts(reviews: List<Review>, categories: (Work) -> List<String>): List<CategoryCount> =
    reviews.flatMap { categories(it.work) }
        .groupingBy { it }.eachCount()
        .map { CategoryCount(it.key, it.value) }
        .sortedByDescending { it.count }

private val movieGenres: (Work) -> List<String> = { it.movie?.genres.orEmpty() }
private val bookSubjects: (Work) -> List<String> = { it.book?.subjects.orEmpty() }

/** Top and bottom three genres by average score (club or one member's). */
fun computeGenreStats(reviews: List<Review>, memberId: String? = null): LovedCategories {
    val sorted = categoryScores(reviews, movieGenres, memberId).sortedByDescending { it.averageScore }
    return LovedCategories(
        mostLoved = sorted.take(3),
        leastLoved = sorted.takeLast(3).sortedBy { it.averageScore },
    )
}

/** Most-watched genres, top five. */
fun computeGenreCounts(reviews: List<Review>): List<CategoryCount> = categoryCounts(reviews, movieGenres).take(5)

/** The five most- and five least-watched genres (least first in the latter). */
fun computeGenreWatchCounts(reviews: List<Review>): GenreWatchCounts {
    val sorted = categoryCounts(reviews, movieGenres)
    return GenreWatchCounts(
        mostWatched = sorted.take(5),
        leastWatched = sorted.takeLast(5).sortedBy { it.count },
    )
}

/** Best-scored book subjects (ties broken by count), top five. */
fun computeSubjectStats(reviews: List<Review>, memberId: String? = null): List<CategoryStat> =
    categoryScores(reviews, bookSubjects, memberId)
        .sortedWith(compareByDescending<CategoryStat> { it.averageScore }.thenByDescending { it.count })
        .take(MAX_SUBJECTS)

/** Most-read book subjects, top five. */
fun computeSubjectCounts(reviews: List<Review>): List<CategoryCount> = categoryCounts(reviews, bookSubjects).take(MAX_SUBJECTS)

// --- People ------------------------------------------------------------------

private fun topPeople(reviews: List<Review>, people: (Review) -> List<CreditedPerson>): List<PersonStat> {
    data class Acc(var total: Double, var count: Int, val works: MutableList<String>, var image: String?)
    val map = linkedMapOf<String, Acc>()
    for (review in reviews) {
        for (person in people(review).distinctBy { it.name }) {
            val acc = map.getOrPut(person.name) { Acc(0.0, 0, mutableListOf(), null) }
            acc.total += review.avg
            acc.count++
            acc.works += review.work.title
            if (acc.image == null) acc.image = person.imageUrl
        }
    }
    return map.map { (name, a) -> PersonStat(name, a.count, a.total / a.count, a.works, a.image) }
        .sortedWith(compareByDescending<PersonStat> { it.workCount }.thenByDescending { it.averageScore })
        .take(5)
}

fun computeTopDirectors(reviews: List<Review>): List<PersonStat> =
    topPeople(reviews) { r ->
        val movie = r.work.movie
        movie?.directors.orEmpty().map { CreditedPerson(it, movie?.directorImageUrls?.get(it)) }
    }

/** Cast isn't in the reviews payload; [castByExternalId] comes from the cast endpoint. */
fun computeTopActors(reviews: List<Review>, castByExternalId: Map<String, List<CreditedPerson>>): List<PersonStat> =
    topPeople(reviews) { r -> r.work.externalId?.let { castByExternalId[it] }.orEmpty() }

fun computeTopAuthors(reviews: List<Review>): List<PersonStat> =
    topPeople(reviews) { r -> r.work.book?.authors.orEmpty().map { CreditedPerson(it, null) } }

// --- Members -----------------------------------------------------------------

/** Members by average score given; with 2+ ranked, the top is the Softie and the bottom the Hater. */
fun computeLeaderboard(reviews: List<Review>, members: List<Member>): List<LeaderboardEntry> {
    val ranked = members.mapNotNull { member ->
        val values = reviews.mapNotNull { it.scores[member.id]?.value }.filterNot { it.isNaN() }
        if (values.isEmpty()) null else LeaderboardEntry(member, values.average().round2(), values.size, null)
    }.sortedByDescending { it.averageScore }
    if (ranked.size < 2) return ranked
    return ranked.mapIndexed { i, e ->
        when (i) {
            0 -> e.copy(title = ReviewerTitle.SOFTIE)
            ranked.lastIndex -> e.copy(title = ReviewerTitle.HATER)
            else -> e
        }
    }
}

/**
 * Closest and furthest-apart member pairs by average score difference over
 * works both scored (3+ needed). With [memberId], only that member's pairs,
 * oriented so they're always `memberA`.
 */
fun computeTasteSimilarity(reviews: List<Review>, members: List<Member>, memberId: String? = null): TasteSimilarity {
    val pairs = mutableListOf<PairSimilarity>()
    for (i in members.indices) for (j in i + 1 until members.size) {
        var a = members[i]
        var b = members[j]
        if (memberId != null && b.id == memberId) a = b.also { b = a }
        if (memberId != null && a.id != memberId) continue
        val shared = reviews.mapNotNull { r ->
            val sa = r.scores[a.id]?.value ?: return@mapNotNull null
            val sb = r.scores[b.id]?.value ?: return@mapNotNull null
            Agreement(r.work, sa, sb)
        }
        if (shared.size < MIN_SHARED_REVIEWS) continue
        val avgDiff = shared.map { it.difference }.average().round2()
        val sorted = shared.sortedBy { it.difference }
        pairs += PairSimilarity(
            memberA = a,
            memberB = b,
            similarityPercent = max(0.0, ((1 - avgDiff / 10) * 100).round2()),
            avgDifference = avgDiff,
            sharedCount = shared.size,
            bestAgreements = sorted.take(3),
            worstAgreements = sorted.takeLast(3).sortedByDescending { it.difference },
        )
    }
    if (pairs.isEmpty()) return TasteSimilarity(null, null)
    val sorted = pairs.sortedByDescending { it.similarityPercent }
    return TasteSimilarity(sorted.first(), sorted.last())
}

/** Works the club agreed on most (lowest spread) and fought over most. */
fun computeConsensus(reviews: List<Review>, members: List<Member>): Consensus {
    val byId = members.associateBy { it.id }
    val entries = reviews.mapNotNull { r ->
        val scores = r.scores.mapNotNull { (id, s) -> byId[id]?.let { NamedScore(it, s.value) } }
        if (scores.size < MIN_SCORES_FOR_SPREAD) null
        else ConsensusEntry(r.work, r.avg.round2(), stdDev(scores.map { it.score }).round2(), scores.sortedByDescending { it.score })
    }.sortedBy { it.stdDev }
    return Consensus(
        mostAgreed = entries.take(3),
        mostDivisive = entries.takeLast(3).sortedByDescending { it.stdDev },
    )
}

private fun outliers(
    reviews: List<Review>,
    members: List<Member>,
    isOutlier: (score: Double, average: Double) -> Boolean,
    order: Comparator<OutlierWork>,
): List<MemberOutliers> {
    val byId = members.associateBy { it.id }
    val perMember = linkedMapOf<String, MutableList<OutlierWork>>()
    for (r in reviews) {
        val valid = r.scores.values.filter { byId.containsKey(it.userId) && !it.value.isNaN() }
        if (valid.size < MIN_SCORES_FOR_SPREAD) continue
        val out = valid.filter { isOutlier(it.value, r.avg) }
        if (out.size != 1) continue
        val o = out.single()
        perMember.getOrPut(o.userId) { mutableListOf() } +=
            OutlierWork(r.work, o.value, r.avg.round2(), (o.value - r.avg).round2())
    }
    return perMember.map { (id, works) -> MemberOutliers(byId.getValue(id), works.sortedWith(order).take(MAX_OUTLIERS_PER_MEMBER)) }
        .sortedByDescending { it.works.size }
}

/** Works one member alone scored 2+ points above the club average. */
fun computeGuiltyPleasures(reviews: List<Review>, members: List<Member>): List<MemberOutliers> =
    outliers(reviews, members, { s, avg -> s - avg >= OUTLIER_THRESHOLD }, compareByDescending { it.difference })

/** Works one member alone scored 2+ points below the club average. */
fun computeCurmudgeons(reviews: List<Review>, members: List<Member>): List<MemberOutliers> =
    outliers(reviews, members, { s, avg -> avg - s >= OUTLIER_THRESHOLD }, compareBy { it.difference })

/** Where the club's average disagrees most with TMDB's rating. */
fun computeTmdbDeviation(reviews: List<Review>): ExternalDeviation {
    val entries = reviews.mapNotNull { r ->
        val tmdb = r.work.movie?.voteAverage?.takeIf { it > 0 } ?: return@mapNotNull null
        DeviationEntry(r.work, r.avg.round2(), tmdb.round2(), (r.avg - tmdb).round2())
    }.sortedByDescending { it.deviation }
    return ExternalDeviation(
        clubRatedHigher = entries.take(5).filter { it.deviation > 0 },
        clubRatedLower = entries.takeLast(5).filter { it.deviation < 0 }.sortedBy { it.deviation },
    )
}
