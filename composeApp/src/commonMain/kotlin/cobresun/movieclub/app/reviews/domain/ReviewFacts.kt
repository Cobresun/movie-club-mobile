package cobresun.movieclub.app.reviews.domain

import cobresun.movieclub.app.core.domain.WorkDetails
import cobresun.movieclub.app.core.domain.WorkType
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * One spotlight fact about a scored review ("highest-rated movie in club
 * history", "your club's 50th movie", …). Port of the web's `reviewFacts.ts`:
 * same generators, thresholds, priority order and copy.
 */
data class ReviewFact(val kind: FactKind, val label: String, val text: String)

enum class FactKind {
    ALL_TIME_HIGH, ALL_TIME_LOW, CLUB_MILESTONE, WATCH_TIME_MILESTONE, PAGES_MILESTONE, DIVISIVE,
    YEAR_HIGH, YEAR_LOW, PERSON_HIGH, PERSON_LOW, ACTOR_MILESTONE, TIME_TRAVEL, LONGEST_RUNTIME,
    LONGEST_BOOK, DECADE_FIRST, FIRST_GENRE, TMDB_DEVIATION,
}

object ReviewFacts {
    private const val MIN_WORKS_FOR_ALL_TIME_RECORD = 10
    private const val MIN_WORKS_FOR_DIVISIVE_RECORD = 10
    private const val DIVISIVE_MIN_SPREAD = 4.0
    private const val MIN_WORKS_FOR_YEAR_RECORD = 5
    private const val MIN_WORKS_FOR_PERSON_RECORD = 3
    private const val MIN_PRIOR_WORKS_FOR_FIRSTS = 10
    private const val TMDB_DEVIATION_THRESHOLD = 2.5
    private const val PAGES_OF_WAR_AND_PEACE = 1225
    private val WATCH_HOUR_MILESTONES = listOf(24, 48, 100, 250, 500, 1000, 2000)
    private val PAGE_MILESTONES = listOf(5000, 10000, 25000, 50000, 100000, 250000)

    private class Ctx(
        val target: Review,
        /** Every review, oldest first. */
        val works: List<Review>,
        /** Works up to and including the target, so count facts stay true forever. */
        val worksThrough: List<Review>,
        /** Works with a club average, oldest first (includes the target). */
        val scored: List<Review>,
        val position: Int,
        val targetAverage: Double,
        val targetYear: Int?,
        val noun: String,
    ) {
        val prior get() = worksThrough.filter { it.work.id != target.work.id }
        fun others(list: List<Review>) = list.filter { it.work.id != target.work.id }
    }

    private typealias Generator = (Ctx) -> ReviewFact?

    /**
     * Picks the single most interesting fact about [workId], or null. Only needs
     * the target to have a club average (at least one member scored it).
     */
    fun compute(reviews: List<Review>, workId: String): ReviewFact? {
        val target = reviews.find { it.work.id == workId } ?: return null
        val targetAverage = target.average ?: return null
        val works = reviews.sortedBy { it.work.addedAt }
        val position = works.indexOfFirst { it.work.id == workId } + 1
        val ctx = Ctx(
            target = target,
            works = works,
            worksThrough = works.take(position),
            scored = works.filter { it.average != null },
            position = position,
            targetAverage = targetAverage,
            targetYear = utcYear(target.work.addedAt),
            noun = if (target.work.type == WorkType.BOOK) "book" else "movie",
        )
        val generators = when (target.work.type) {
            WorkType.MOVIE -> MOVIE_GENERATORS
            WorkType.BOOK -> BOOK_GENERATORS
        }
        return generators.firstNotNullOfOrNull { it(ctx) }
    }

    // --- helpers -----------------------------------------------------------

    private fun Review.movie() = work.details as? WorkDetails.Movie
    private fun Review.book() = work.details as? WorkDetails.Book
    private fun Review.memberScores() = scores.values.map { it.value }.filterNot { it.isNaN() }
    private fun Review.runtime() = movie()?.runtimeMinutes?.takeIf { it > 0 }
    private fun Review.pages() = book()?.pageCount?.takeIf { it > 0 }
    private fun Review.releaseYear() = movie()?.releaseDate?.takeIf { it.length >= 4 }?.take(4)?.toIntOrNull()
    private fun Review.spread(): Double? = memberScores().takeIf { it.size >= 2 }?.let { it.max() - it.min() }

    /** Timestamps are UTC ISO-8601, so the calendar year is the leading digits. */
    private fun utcYear(iso: String) = iso.take(4).toIntOrNull()

    /** Like JS `String(Math.round(x * 10) / 10)`: "7.5", "8" (no trailing ".0"). */
    internal fun formatScore(score: Double): String {
        val tenths = (score * 10).roundToLong()
        return if (tenths % 10 == 0L) "${tenths / 10}" else "${tenths / 10.0}"
    }

    internal fun ordinal(n: Int): String {
        val rem100 = n % 100
        if (rem100 in 11..13) return "${n}th"
        return when (n % 10) {
            1 -> "${n}st"
            2 -> "${n}nd"
            3 -> "${n}rd"
            else -> "${n}th"
        }
    }

    /** "2h 28m" / "45m" (domain-local copy of the web's formatRuntime). */
    internal fun formatRuntime(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0 -> "${m}m"
            m == 0 -> "${h}h"
            else -> "${h}h ${m}m"
        }
    }

    internal fun groupThousands(n: Int): String =
        n.toString().reversed().chunked(3).joinToString(",").reversed()

    // --- generators (same order and copy as the web) ------------------------

    private val allTimeRecord: Generator = gen@{ ctx ->
        if (ctx.scored.size < MIN_WORKS_FOR_ALL_TIME_RECORD) return@gen null
        val others = ctx.others(ctx.scored).mapNotNull { it.average }
        if (others.isEmpty()) return@gen null
        val s = formatScore(ctx.targetAverage)
        when {
            ctx.targetAverage > others.max() -> ReviewFact(FactKind.ALL_TIME_HIGH, "Club record", "At $s, this is the highest-rated ${ctx.noun} in club history.")
            ctx.targetAverage < others.min() -> ReviewFact(FactKind.ALL_TIME_LOW, "Club record", "At $s, this is the lowest-rated ${ctx.noun} in club history.")
            else -> null
        }
    }

    private fun isCountMilestone(n: Int) = n == 10 || n == 25 || (n >= 50 && n % 50 == 0)

    private val clubMilestone: Generator = { ctx ->
        if (!isCountMilestone(ctx.position)) null
        else ReviewFact(FactKind.CLUB_MILESTONE, "Milestone", "This is the ${ordinal(ctx.position)} ${ctx.noun} your club has reviewed!")
    }

    /** Largest milestone crossed by the target's amount, anchored to worksThrough. */
    private fun crossedMilestone(ctx: Ctx, amountOf: (Review) -> Double?, milestones: List<Int>): Int? {
        val targetAmount = amountOf(ctx.target) ?: return null
        val before = ctx.prior.mapNotNull(amountOf).sum()
        val through = before + targetAmount
        return milestones.reversed().find { before < it && through >= it }
    }

    private val watchTimeMilestone: Generator = gen@{ ctx ->
        val hours = crossedMilestone(ctx, { r -> r.runtime()?.let { it / 60.0 } }, WATCH_HOUR_MILESTONES) ?: return@gen null
        val days = hours / 24
        val stretch = if (days == 1) "a full day on the couch" else "over $days full days on the couch"
        ReviewFact(FactKind.WATCH_TIME_MILESTONE, "Couch time", "This one tipped your club past $hours hours of movies watched together — $stretch.")
    }

    private val pagesMilestone: Generator = gen@{ ctx ->
        val pages = crossedMilestone(ctx, { it.pages()?.toDouble() }, PAGE_MILESTONES) ?: return@gen null
        val copies = (pages.toDouble() / PAGES_OF_WAR_AND_PEACE).roundToInt()
        ReviewFact(FactKind.PAGES_MILESTONE, "Bookworms", "This one pushed your club past ${groupThousands(pages)} pages read together — about $copies copies of War and Peace.")
    }

    private val divisiveRecord: Generator = gen@{ ctx ->
        if (ctx.scored.size < MIN_WORKS_FOR_DIVISIVE_RECORD) return@gen null
        val scores = ctx.target.memberScores()
        val spread = ctx.target.spread()
        if (spread == null || spread < DIVISIVE_MIN_SPREAD) return@gen null
        val otherSpreads = ctx.others(ctx.scored).mapNotNull { it.spread() }
        if (otherSpreads.isEmpty() || spread <= otherSpreads.max()) return@gen null
        ReviewFact(FactKind.DIVISIVE, "Divisive", "Scores ran from ${formatScore(scores.min())} to ${formatScore(scores.max())} — the most divisive pick in club history.")
    }

    private val yearRecord: Generator = gen@{ ctx ->
        val year = ctx.targetYear ?: return@gen null
        val cohort = ctx.scored.filter { utcYear(it.work.addedAt) == year }
        if (cohort.size < MIN_WORKS_FOR_YEAR_RECORD) return@gen null
        val others = ctx.others(cohort).mapNotNull { it.average }
        if (others.isEmpty()) return@gen null
        when {
            ctx.targetAverage > others.max() -> ReviewFact(FactKind.YEAR_HIGH, "Best of $year", "The highest-rated of the ${cohort.size} ${ctx.noun}s your club has reviewed in $year.")
            ctx.targetAverage < others.min() -> ReviewFact(FactKind.YEAR_LOW, "Rough one", "The lowest-rated of the ${cohort.size} ${ctx.noun}s your club has reviewed in $year.")
            else -> null
        }
    }

    private fun personRecord(
        ctx: Ctx,
        targetPeople: List<String>,
        peopleOf: (Review) -> List<String>,
        high: (Int, String) -> String,
        low: (Int, String) -> String,
    ): ReviewFact? {
        for (name in targetPeople) {
            val others = ctx.others(ctx.scored).filter { name in peopleOf(it) }.mapNotNull { it.average }
            if (others.size + 1 < MIN_WORKS_FOR_PERSON_RECORD) continue
            val count = others.size + 1
            if (ctx.targetAverage > others.max()) return ReviewFact(FactKind.PERSON_HIGH, "A new favorite", high(count, name))
            if (ctx.targetAverage < others.min()) return ReviewFact(FactKind.PERSON_LOW, "A low point", low(count, name))
        }
        return null
    }

    private val directorRecord: Generator = gen@{ ctx ->
        val movie = ctx.target.movie() ?: return@gen null
        personRecord(
            ctx, movie.directors, { it.movie()?.directors.orEmpty() },
            { c, n -> "Your club's highest-rated of $c films directed by $n." },
            { c, n -> "Your club's lowest-rated of $c films directed by $n." },
        )
    }

    private val authorRecord: Generator = gen@{ ctx ->
        val book = ctx.target.book() ?: return@gen null
        personRecord(
            ctx, book.authors, { it.book()?.authors.orEmpty() },
            { c, n -> "Your club's highest-rated of $c books by $n." },
            { c, n -> "Your club's lowest-rated of $c books by $n." },
        )
    }

    private fun isActorMilestone(n: Int) = n >= 5 && n % 5 == 0

    /** Counts an actor only across movies where they were major cast, never a cameo. */
    private val actorMilestone: Generator = gen@{ ctx ->
        val movie = ctx.target.movie() ?: return@gen null
        var best: Pair<String, Int>? = null
        for (name in movie.majorCastNames) {
            val count = ctx.worksThrough.count { it.movie()?.majorCastNames?.contains(name) == true }
            if (isActorMilestone(count) && (best == null || count > best.second)) best = name to count
        }
        best?.let { (name, count) ->
            ReviewFact(FactKind.ACTOR_MILESTONE, "Familiar face", "That's the ${ordinal(count)} movie your club has watched featuring $name.")
        }
    }

    private fun otherValues(ctx: Ctx, valueOf: (Review) -> Int?) = ctx.others(ctx.works).mapNotNull(valueOf)

    private val oldestMovie: Generator = gen@{ ctx ->
        if (ctx.works.size < MIN_WORKS_FOR_ALL_TIME_RECORD) return@gen null
        val year = ctx.target.releaseYear() ?: return@gen null
        val others = otherValues(ctx) { it.releaseYear() }
        if (others.isEmpty() || year >= others.min()) return@gen null
        ReviewFact(FactKind.TIME_TRAVEL, "Time travel", "Released in $year, this is the oldest movie your club has ever watched.")
    }

    private val oldestBook: Generator = gen@{ ctx ->
        if (ctx.works.size < MIN_WORKS_FOR_ALL_TIME_RECORD) return@gen null
        val year = ctx.target.book()?.firstPublishYear ?: return@gen null
        val others = otherValues(ctx) { it.book()?.firstPublishYear }
        if (others.isEmpty() || year >= others.min()) return@gen null
        ReviewFact(FactKind.TIME_TRAVEL, "Time travel", "First published in $year, this is the oldest book your club has ever read.")
    }

    private val longestRuntime: Generator = gen@{ ctx ->
        if (ctx.works.size < MIN_WORKS_FOR_ALL_TIME_RECORD) return@gen null
        val runtime = ctx.target.runtime() ?: return@gen null
        val others = otherValues(ctx) { it.runtime() }
        if (others.isEmpty() || runtime <= others.max()) return@gen null
        ReviewFact(FactKind.LONGEST_RUNTIME, "Settle in", "At ${formatRuntime(runtime)}, this is the longest movie your club has ever sat through.")
    }

    private val longestBook: Generator = gen@{ ctx ->
        if (ctx.works.size < MIN_WORKS_FOR_ALL_TIME_RECORD) return@gen null
        val pages = ctx.target.pages() ?: return@gen null
        val others = otherValues(ctx) { it.pages() }
        if (others.isEmpty() || pages <= others.max()) return@gen null
        ReviewFact(FactKind.LONGEST_BOOK, "Doorstopper", "At ${groupThousands(pages)} pages, this is the longest book your club has ever conquered.")
    }

    private val decadeFirst: Generator = gen@{ ctx ->
        val year = ctx.target.releaseYear() ?: return@gen null
        val prior = ctx.prior
        if (prior.size < MIN_PRIOR_WORKS_FOR_FIRSTS) return@gen null
        fun decadeOf(y: Int) = y.floorDiv(10) * 10
        val seen = prior.mapNotNull { it.releaseYear() }.map(::decadeOf).toSet()
        if (seen.isEmpty() || decadeOf(year) in seen) return@gen null
        ReviewFact(FactKind.DECADE_FIRST, "Time capsule", "Your club's first trip to the ${decadeOf(year)}s.")
    }

    private val firstGenre: Generator = gen@{ ctx ->
        val movie = ctx.target.movie() ?: return@gen null
        if (movie.genres.isEmpty()) return@gen null
        val prior = ctx.prior
        if (prior.size < MIN_PRIOR_WORKS_FOR_FIRSTS) return@gen null
        val seen = prior.flatMap { it.movie()?.genres.orEmpty() }.toSet()
        val newGenre = movie.genres.find { it !in seen }?.takeIf { it.isNotEmpty() } ?: return@gen null
        ReviewFact(FactKind.FIRST_GENRE, "New territory", "Your club's first $newGenre movie.")
    }

    private val tmdbDeviation: Generator = gen@{ ctx ->
        val tmdb = ctx.target.movie()?.voteAverage ?: return@gen null
        if (tmdb == 0.0) return@gen null
        val deviation = ctx.targetAverage - tmdb
        if (abs(deviation) < TMDB_DEVIATION_THRESHOLD) return@gen null
        val direction = if (deviation > 0) "higher" else "lower"
        ReviewFact(FactKind.TMDB_DEVIATION, "Hot take", "Your club scored this ${formatScore(abs(deviation))} points $direction than its ${formatScore(tmdb)} TMDB rating.")
    }

    private val MOVIE_GENERATORS = listOf(
        allTimeRecord, clubMilestone, watchTimeMilestone, divisiveRecord, yearRecord, directorRecord,
        actorMilestone, oldestMovie, longestRuntime, decadeFirst, firstGenre, tmdbDeviation,
    )
    private val BOOK_GENERATORS = listOf(
        allTimeRecord, clubMilestone, pagesMilestone, divisiveRecord, yearRecord, authorRecord,
        oldestBook, longestBook,
    )
}
