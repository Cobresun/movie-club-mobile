package cobresun.movieclub.app.filters.domain

import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.domain.WorkDetails
import kotlin.math.roundToLong

/**
 * What a filter reads from: the work, plus its club average when it's a review
 * row (null on a watchlist, where nobody has scored it).
 */
data class FilterTarget(val work: Work, val averageScore: Double? = null)

/** Which glyph a filter section and its chips wear; presentation maps it to an icon. */
enum class FilterIcon { GENRE, SCORE, CALENDAR_RANGE, TIMER, DIRECTOR, CAST, STUDIO, REVIEWED_IN, AUTHOR, SUBJECT, BOOK_LENGTH }

/**
 * A single filter exposed by the search filter bar (web `clubType.ts`
 * `filterOptions`). A choice is a set of tappable values (genres, people); a
 * range is a span picked on a histogram of the values the club actually has.
 */
sealed interface FilterOption {
    /** Stable id, also the web's key ("genre", "average_score"). */
    val key: String
    val label: String
    val icon: FilterIcon
}

enum class ChoiceOrder {
    /** Most common values first. */
    POPULAR,

    /** Newest first (for years). */
    NEWEST,
}

data class ChoiceFilterOption(
    override val key: String,
    override val label: String,
    override val icon: FilterIcon,
    /** Placeholder for the search field a long list of values gets. */
    val searchPlaceholder: String,
    val order: ChoiceOrder,
    val values: (FilterTarget) -> List<String>,
) : FilterOption

data class RangeFilterOption(
    override val key: String,
    override val label: String,
    override val icon: FilterIcon,
    /** Increment the slider snaps to. Must divide [bucket]. */
    val step: Double,
    /** Width of one histogram bar (doubled for long spans). */
    val bucket: Double,
    val format: (Double) -> String,
    /** One-tap spans, given every value the club has. */
    val presets: (List<Double>) -> List<RangePreset>,
    val value: (FilterTarget) -> Double?,
) : FilterOption

/** The values picked from a choice filter, or a span picked on a range filter. */
sealed interface FilterSelection {
    /** A work matches if it has any of [values]. */
    data class Choice(val values: List<String>) : FilterSelection

    /**
     * An inclusive span. A missing end is open, so `from = 8` reads as "8 and
     * up" and survives the data growing past today's highest value.
     */
    data class Range(val from: Double? = null, val to: Double? = null) : FilterSelection
}

/** A one-tap shortcut for a range filter, e.g. "Short & sweet" -> up to 90 minutes. */
data class RangePreset(
    val label: String,
    /** The span in words, when the label alone doesn't say it ("8+"). */
    val detail: String? = null,
    val from: Double? = null,
    val to: Double? = null,
)

/** Per club type: the filters offered and the nouns the result button uses. */
object ClubFilters {
    fun options(clubType: ClubType): List<FilterOption> = when (clubType) {
        ClubType.MOVIE -> movieOptions
        ClubType.BOOK -> bookOptions
    }

    fun noun(clubType: ClubType): String = when (clubType) {
        ClubType.MOVIE -> "movie"
        ClubType.BOOK -> "book"
    }

    fun pluralNoun(clubType: ClubType): String = noun(clubType) + "s"

    /** Keys of the filters that only make sense on reviews (watchlists drop them). */
    val reviewOnlyKeys: Set<String> = setOf(KEY_SCORE, KEY_REVIEWED_IN)

    const val KEY_SCORE = "average_score"
    const val KEY_REVIEWED_IN = "review_date"

    private val clubScore = RangeFilterOption(
        key = KEY_SCORE,
        label = "Club score",
        icon = FilterIcon.SCORE,
        step = 0.5,
        bucket = 1.0,
        format = ::formatScore,
        presets = {
            listOf(
                RangePreset("Loved it", "8+", from = 8.0),
                RangePreset("Liked it", "6+", from = 6.0),
                RangePreset("Not for us", "5 and under", to = 5.0),
            )
        },
        value = { it.averageScore },
    )

    // The web slices the ISO instant, so the year is the UTC one.
    private val reviewedIn = ChoiceFilterOption(
        key = KEY_REVIEWED_IN,
        label = "Reviewed in",
        icon = FilterIcon.REVIEWED_IN,
        searchPlaceholder = "Find a year",
        order = ChoiceOrder.NEWEST,
        values = { t -> listOfNotNull(t.work.addedAt.take(4).takeIf { it.length == 4 }) },
    )

    private val movieOptions: List<FilterOption> = listOf(
        ChoiceFilterOption(
            key = "genre", label = "Genre", icon = FilterIcon.GENRE,
            searchPlaceholder = "Find a genre", order = ChoiceOrder.POPULAR,
            values = { it.movie?.genres.orEmpty() },
        ),
        clubScore,
        RangeFilterOption(
            key = "release_date", label = "Release year", icon = FilterIcon.CALENDAR_RANGE,
            step = 1.0, bucket = 5.0, format = ::formatWhole, presets = ::decadePresets,
            value = { t -> t.movie?.releaseDate?.take(4)?.toIntOrNull()?.toDouble() },
        ),
        RangeFilterOption(
            key = "runtime", label = "Runtime", icon = FilterIcon.TIMER,
            step = 5.0, bucket = 15.0, format = { formatRuntime(it.toInt()) },
            presets = {
                listOf(
                    RangePreset("Short & sweet", "under 1h 30m", to = 90.0),
                    RangePreset("Feature length", "1h 30m – 2h", from = 90.0, to = 120.0),
                    RangePreset("Epic", "2h 30m+", from = 150.0),
                )
            },
            value = { t -> t.movie?.runtimeMinutes?.toDouble() },
        ),
        ChoiceFilterOption(
            key = "director", label = "Director", icon = FilterIcon.DIRECTOR,
            searchPlaceholder = "Find a director", order = ChoiceOrder.POPULAR,
            values = { it.movie?.directors.orEmpty() },
        ),
        ChoiceFilterOption(
            key = "actor", label = "Cast", icon = FilterIcon.CAST,
            searchPlaceholder = "Find an actor", order = ChoiceOrder.POPULAR,
            // Bulk payloads carry names only; full actors come from the details endpoint.
            values = { it.movie?.castNames.orEmpty() },
        ),
        ChoiceFilterOption(
            key = "company", label = "Studio", icon = FilterIcon.STUDIO,
            searchPlaceholder = "Find a studio", order = ChoiceOrder.POPULAR,
            values = { it.movie?.productionCompanies.orEmpty() },
        ),
        reviewedIn,
    )

    private val bookOptions: List<FilterOption> = listOf(
        ChoiceFilterOption(
            key = "author", label = "Author", icon = FilterIcon.AUTHOR,
            searchPlaceholder = "Find an author", order = ChoiceOrder.POPULAR,
            values = { it.book?.authors.orEmpty() },
        ),
        ChoiceFilterOption(
            key = "subject", label = "Subject", icon = FilterIcon.SUBJECT,
            searchPlaceholder = "Find a subject", order = ChoiceOrder.POPULAR,
            values = { it.book?.subjects.orEmpty() },
        ),
        clubScore,
        RangeFilterOption(
            key = "first_publish_year", label = "First published", icon = FilterIcon.CALENDAR_RANGE,
            step = 1.0, bucket = 10.0, format = ::formatWhole,
            presets = {
                listOf(
                    RangePreset("Classics", "before 1950", to = 1949.0),
                    RangePreset("Modern", "1950 – 1999", from = 1950.0, to = 1999.0),
                    RangePreset("This century", "2000+", from = 2000.0),
                )
            },
            value = { t -> t.book?.firstPublishYear?.toDouble() },
        ),
        RangeFilterOption(
            key = "pages", label = "Length", icon = FilterIcon.BOOK_LENGTH,
            step = 10.0, bucket = 50.0, format = { "${formatWhole(it)} pages" },
            presets = {
                listOf(
                    RangePreset("Quick read", "under 200 pages", to = 200.0),
                    RangePreset("Doorstopper", "500+ pages", from = 500.0),
                )
            },
            value = { t -> t.book?.pageCount?.toDouble() },
        ),
        reviewedIn,
    )
}

private val FilterTarget.movie: WorkDetails.Movie? get() = work.details as? WorkDetails.Movie
private val FilterTarget.book: WorkDetails.Book? get() = work.details as? WorkDetails.Book

/** "’90s" for last century, "2010s" for this one — the way people say them. */
internal fun decadePresets(years: List<Double>): List<RangePreset> =
    years.map { (it.toInt() / 10) * 10 }.distinct().sorted().map { decade ->
        RangePreset(
            label = if (decade < 2000) "’${decade.toString().takeLast(2)}s" else "${decade}s",
            from = decade.toDouble(),
            to = decade + 9.0,
        )
    }

/** Web `formatScore`: rounded to two places with no trailing zeros ("8", "7.5", "6.67"). */
internal fun formatScore(score: Double): String {
    val hundredths = (score * 100).roundToLong()
    val whole = hundredths / 100
    val frac = kotlin.math.abs(hundredths % 100)
    return when {
        frac == 0L -> whole.toString()
        frac % 10 == 0L -> "$whole.${frac / 10}"
        else -> "$whole.${frac.toString().padStart(2, '0')}"
    }
}

internal fun formatWhole(value: Double): String = value.roundToLong().toString()

/** "2h 28m" / "45m" / "2h" (web `formatRuntime`). */
internal fun formatRuntime(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h == 0 -> "${m}m"
        m == 0 -> "${h}h"
        else -> "${h}h ${m}m"
    }
}
