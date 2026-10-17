package cobresun.movieclub.app.stats.domain

import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.core.domain.WorkDetails
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.reviews.domain.Review
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/*
 * The cards of one year's Wrapped, ported from the web's
 * `features/wrapped/wrappedDeck.ts`. Each slot decides from the year's data
 * whether it has anything to say, and how many cards it needs to say it.
 */

enum class WrappedTone { VIOLET, BLUE, EMERALD, ROSE, AMBER, SKY }

/** What a card shows; the UI picks a layout per variant. */
sealed interface WrappedCardContent {
    data class Intro(
        val year: Int,
        val clubType: ClubType,
        val workCount: Int,
        /** "movies watched" / "books read". */
        val countLabel: String,
        val memberCount: Int,
    ) : WrappedCardContent

    data class RankedWorks(val works: List<Review>) : WrappedCardContent

    data class Divisive(
        val review: Review,
        val average: Double,
        val spread: Double,
        val scores: List<NamedScore>,
    ) : WrappedCardContent

    data class HotTakes(val takes: List<HotTake>) : WrappedCardContent

    data class TasteTwins(
        val twins: PairSimilarity,
        val opposites: PairSimilarity?,
        /** "movies" / "books". */
        val countNoun: String,
    ) : WrappedCardContent

    data class ClosestMatches(val matches: List<ClosestMatch>) : WrappedCardContent

    data class Person(
        val name: String,
        val imageUrl: String?,
        val workCount: Int,
        val countNoun: String,
        val averageScore: Double,
        val works: List<String>,
    ) : WrappedCardContent

    data class Volume(
        val unit: VolumeUnit,
        val value: Long,
        /** "hours watched" / "pages read". */
        val unitLabel: String,
        val detail: String,
    ) : WrappedCardContent
}

enum class VolumeUnit { HOURS, PAGES }

data class WrappedCard(
    val id: String,
    val label: String,
    val tone: WrappedTone,
    val content: WrappedCardContent,
)

/** One year of one club, as every card builder sees it. */
data class WrappedContext(
    val year: Int,
    val clubType: ClubType,
    /** The year's scored reviews. */
    val reviews: List<Review>,
    val members: List<Member>,
) {
    val movies: List<Review> get() = reviews.filter { it.work.details is WorkDetails.Movie }
    val books: List<Review> get() = reviews.filter { it.work.details is WorkDetails.Book }
}

/**
 * A slot in a club type's Wrapped. [cards] returns the content for each card
 * the slot contributes: none when the year lacks the data for it, several when
 * a list is too long for one card.
 */
private class CardDef(
    val key: String,
    val label: String,
    val tone: WrappedTone,
    val cards: (WrappedContext) -> List<WrappedCardContent>,
)

// How many rows of each list fit on one card before it continues on the next.
private const val HOT_TAKES_PER_CARD = 4
private const val MATCHES_PER_CARD = 5
/** "Most divisive" is only a superlative when there was something to compare. */
private const val MIN_WORKS_FOR_DIVISIVE = 2
/** A "most-watched" director needs to have been watched more than once. */
private const val MIN_PERSON_WORKS = 2

private fun countNoun(type: ClubType): String = if (type == ClubType.BOOK) "books" else "movies"
private fun countLabel(type: ClubType): String = if (type == ClubType.BOOK) "books read" else "movies watched"

/** JavaScript's `toFixed(1)`: always one decimal place ("7.0", "-2.5"). */
fun Double.toFixed1(): String {
    val tenths = (abs(this) * 10).roundToLong()
    val sign = if (this < 0 && tenths != 0L) "-" else ""
    return "$sign${tenths / 10}.${tenths % 10}"
}

private val intro = CardDef("intro", "Club Wrapped", WrappedTone.VIOLET) { ctx ->
    listOf(
        WrappedCardContent.Intro(
            year = ctx.year,
            clubType = ctx.clubType,
            workCount = ctx.reviews.size,
            countLabel = countLabel(ctx.clubType),
            memberCount = activeMemberCount(ctx.reviews, ctx.members),
        )
    )
}

private val topPicks = CardDef("top-picks", "Top picks", WrappedTone.EMERALD) { ctx ->
    val top = computePicks(ctx.reviews).top
    if (top.isNotEmpty()) listOf(WrappedCardContent.RankedWorks(top)) else emptyList()
}

private val bottomPicks = CardDef("bottom-picks", "Bottom picks", WrappedTone.ROSE) { ctx ->
    val bottom = computePicks(ctx.reviews).bottom
    if (bottom.isNotEmpty()) listOf(WrappedCardContent.RankedWorks(bottom)) else emptyList()
}

private val mostDivisive = CardDef("most-divisive", "Most divisive", WrappedTone.AMBER) { ctx ->
    if (ctx.reviews.size < MIN_WORKS_FOR_DIVISIVE) return@CardDef emptyList()
    val entry = computeConsensus(ctx.reviews, ctx.members).mostDivisive.firstOrNull()
    if (entry == null || entry.stdDev == 0.0) return@CardDef emptyList()
    val review = ctx.reviews.first { it.work.id == entry.work.id }
    listOf(WrappedCardContent.Divisive(review, review.average ?: entry.average, entry.stdDev, entry.scores))
}

private val hotTakes = CardDef("hot-takes", "Hot takes", WrappedTone.ROSE) { ctx ->
    computeHotTakes(ctx.reviews, ctx.members).chunked(HOT_TAKES_PER_CARD).map { WrappedCardContent.HotTakes(it) }
}

private val tasteTwins = CardDef("taste-twins", "Who agreed with whom", WrappedTone.SKY) { ctx ->
    val (most, least) = computeTasteSimilarity(ctx.reviews, ctx.members)
    if (most == null) return@CardDef emptyList()
    // A two-member club has one pair, which would be both twins and opposites.
    val samePair = least != null && least.memberA.id == most.memberA.id && least.memberB.id == most.memberB.id
    listOf(WrappedCardContent.TasteTwins(most, if (samePair) null else least, countNoun(ctx.clubType)))
}

private val closestMatches = CardDef("closest-matches", "Closest matches", WrappedTone.VIOLET) { ctx ->
    // With two members the taste-twins card already says everything this would.
    if (ctx.members.size <= 2) return@CardDef emptyList()
    computeClosestMatches(ctx.reviews, ctx.members).chunked(MATCHES_PER_CARD).map { WrappedCardContent.ClosestMatches(it) }
}

private fun personCard(person: PersonStat?, noun: String): List<WrappedCardContent> {
    if (person == null || person.workCount < MIN_PERSON_WORKS) return emptyList()
    return listOf(
        WrappedCardContent.Person(
            name = person.name,
            imageUrl = person.imageUrl,
            workCount = person.workCount,
            countNoun = noun,
            averageScore = person.averageScore,
            works = person.works,
        )
    )
}

private val hoursWatched = CardDef("hours-watched", "Time together", WrappedTone.BLUE) { ctx ->
    val movies = ctx.movies
    val minutes = movies.sumOf { (it.work.details as WorkDetails.Movie).runtimeMinutes ?: 0 }
    if (minutes == 0) return@CardDef emptyList()
    val days = minutes / (60.0 * 24)
    listOf(
        WrappedCardContent.Volume(
            unit = VolumeUnit.HOURS,
            value = (minutes / 60.0).roundToInt().toLong(),
            unitLabel = "hours watched",
            detail = if (days >= 1) "${movies.size} movies, or ${days.toFixed1()} days of non-stop viewing."
            else "Across ${movies.size} movies.",
        )
    )
}

private val pagesRead = CardDef("pages-read", "Time together", WrappedTone.BLUE) { ctx ->
    val books = ctx.books
    val pages = books.sumOf { (it.work.details as WorkDetails.Book).pageCount ?: 0 }
    if (pages == 0) return@CardDef emptyList()
    listOf(
        WrappedCardContent.Volume(
            unit = VolumeUnit.PAGES,
            value = pages.toLong(),
            unitLabel = "pages read",
            detail = "Across ${books.size} books.",
        )
    )
}

private val topDirector = CardDef("top-director", "Most-watched director", WrappedTone.AMBER) { ctx ->
    personCard(computeTopDirectors(ctx.movies).firstOrNull(), countNoun(ctx.clubType))
}

private val topAuthor = CardDef("top-author", "Most-read author", WrappedTone.AMBER) { ctx ->
    personCard(computeTopAuthors(ctx.books).firstOrNull(), countNoun(ctx.clubType))
}

/**
 * The card order per club type. The slots that differ — how much the club got
 * through, and whose work it kept coming back to — are the only type-specific
 * entries; everything score-based is shared.
 */
private fun deckFor(type: ClubType): List<CardDef> = when (type) {
    ClubType.MOVIE -> listOf(intro, hoursWatched, topPicks, bottomPicks, mostDivisive, topDirector, hotTakes, tasteTwins, closestMatches)
    ClubType.BOOK -> listOf(intro, pagesRead, topPicks, bottomPicks, mostDivisive, topAuthor, hotTakes, tasteTwins, closestMatches)
}

fun buildWrappedDeck(ctx: WrappedContext): List<WrappedCard> =
    deckFor(ctx.clubType).flatMap { def ->
        val cards = def.cards(ctx)
        cards.mapIndexed { index, content ->
            val many = cards.size > 1
            WrappedCard(
                id = if (many) "${def.key}-${index + 1}" else def.key,
                label = if (many) "${def.label} (${index + 1} of ${cards.size})" else def.label,
                tone = def.tone,
                content = content,
            )
        }
    }

/** The deck for [year] of a club's scored [reviews]; empty when nothing was scored that year. */
fun buildWrappedDeck(year: Int, clubType: ClubType, reviews: List<Review>, members: List<Member>): List<WrappedCard> {
    val works = worksInYear(reviews, year)
    if (works.isEmpty()) return emptyList()
    return buildWrappedDeck(WrappedContext(year, clubType, works, members))
}
