package cobresun.movieclub.app.stats.domain

import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.reviews.domain.Review
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.abs
import kotlin.math.min
import kotlin.time.Clock
import kotlin.time.Instant

/*
 * Club Wrapped: a year of one club's reviews, summed up as story cards. Ported
 * from the web's `features/wrapped/wrapped.ts`. Like the other stats, these
 * take already scored reviews (see [scoredReviews]).
 */

/**
 * The calendar year a work belongs to in Wrapped: the UTC year it was added to
 * the club's reviews.
 */
fun reviewYear(review: Review): Int? =
    runCatching { Instant.parse(review.work.addedAt) }.getOrNull()?.toLocalDateTime(TimeZone.UTC)?.year

private fun Instant.utcYear(): Int = toLocalDateTime(TimeZone.UTC).year

/**
 * Whether [year]'s Wrapped is out: it comes out on January 1 (UTC) of the next
 * year, once every review that belongs in it is in.
 */
fun isYearReleased(year: Int, now: Instant = Clock.System.now()): Boolean = year < now.utcYear()

/** The current UTC year, which is the one a not-yet-released Wrapped is waiting on. */
fun currentWrappedYear(now: Instant = Clock.System.now()): Int = now.utcYear()

/** Every year the club reviewed something in whose Wrapped is out, newest first. */
fun wrappedYears(reviews: List<Review>, now: Instant = Clock.System.now()): List<Int> =
    reviews.mapNotNull(::reviewYear).distinct().filter { isYearReleased(it, now) }.sortedDescending()

fun worksInYear(reviews: List<Review>, year: Int): List<Review> = reviews.filter { reviewYear(it) == year }

private val Review.validScores: List<Pair<String, Double>>
    get() = scores.values.filterNot { it.value.isNaN() }.map { it.userId to it.value }

/** Members who scored at least one of these works. */
fun activeMemberCount(reviews: List<Review>, members: List<Member>): Int =
    members.count { member -> reviews.any { it.scores.containsKey(member.id) } }

private const val MAX_PICKS = 3

data class WrappedPicks(val top: List<Review>, val bottom: List<Review>)

/**
 * The year's best- and worst-rated works. A short year splits its works between
 * the two lists rather than showing the same film as both a top and bottom pick.
 */
fun computePicks(reviews: List<Review>): WrappedPicks {
    val size = min(MAX_PICKS, reviews.size / 2)
    val byAverage = reviews.sortedWith(
        compareByDescending<Review> { it.average ?: 0.0 }
            .thenBy { it.work.title.lowercase() }
            .thenBy { it.work.title },
    )
    return WrappedPicks(
        top = byAverage.take(size),
        bottom = byAverage.takeLast(size).reversed(),
    )
}

data class HotTake(
    val member: Member,
    val review: Review,
    val memberScore: Double,
    /** Mean of everyone else's score on the same work. */
    val othersAverage: Double,
    /** `memberScore - othersAverage`: positive when they liked it more than the club did. */
    val gap: Double,
)

/**
 * Each member's single score furthest from the rest of the club, biggest gap
 * first. The comparison is against the *other* scorers so a member's own score
 * does not pull the club average towards it. A member who never differed from
 * anyone has no hot take.
 */
fun computeHotTakes(reviews: List<Review>, members: List<Member>): List<HotTake> {
    val takes = members.mapNotNull { member ->
        var best: HotTake? = null
        for (review in reviews) {
            val scores = review.validScores
            val own = scores.firstOrNull { it.first == member.id }?.second ?: continue
            val others = scores.filter { it.first != member.id }.map { it.second }
            if (others.isEmpty()) continue
            val othersAverage = others.average()
            val gap = own - othersAverage
            if (abs(gap) > abs(best?.gap ?: 0.0)) {
                best = HotTake(member, review, own, othersAverage, gap)
            }
        }
        best
    }
    return takes.sortedByDescending { abs(it.gap) }
}

data class ClosestMatch(val member: Member, val match: Member, val similarityPercent: Double)

/** Each member's most like-minded clubmate, closest pairs first. */
fun computeClosestMatches(reviews: List<Review>, members: List<Member>): List<ClosestMatch> =
    members.mapNotNull { member ->
        computeTasteSimilarity(reviews, members, member.id).mostSimilar?.let {
            ClosestMatch(it.memberA, it.memberB, it.similarityPercent)
        }
    }.sortedByDescending { it.similarityPercent }
