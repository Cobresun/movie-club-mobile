package cobresun.movieclub.app.awards.domain

/**
 * The phases of a club's awards season. Wire values match the web's
 * `AwardsStep` enum (0–4).
 */
enum class AwardsStep(val value: Int, val title: String) {
    CATEGORIES(0, "Categories"),
    NOMINATIONS(1, "Nominations"),
    RANKINGS(2, "Rankings"),
    PRESENTATION(3, "Results"),
    COMPLETED(4, "Results");

    /**
     * The step members can manually advance to, or null. Presentation only
     * finishes by revealing every category (see [AwardsSeason.allRevealed]).
     */
    val manualNext: AwardsStep?
        get() = when (this) {
            CATEGORIES -> NOMINATIONS
            NOMINATIONS -> RANKINGS
            RANKINGS -> PRESENTATION
            PRESENTATION, COMPLETED -> null
        }

    val showsResults: Boolean get() = this == PRESENTATION || this == COMPLETED

    companion object {
        fun from(value: Int): AwardsStep = entries.firstOrNull { it.value == value } ?: CATEGORIES

        /** Steps shown in the progress indicator (Presentation and Completed share "Results"). */
        val visible = listOf(CATEGORIES, NOMINATIONS, RANKINGS, PRESENTATION)
    }
}

/** One year's awards. Categories are in display order. */
data class AwardsSeason(
    val year: Int,
    val step: AwardsStep,
    val categories: List<AwardCategory>,
)

data class AwardCategory(
    val title: String,
    val nominations: List<Nomination>,
)

/** A movie nominated in a category. [ranking] maps voter user id to rank (1 = best). */
data class Nomination(
    val movieId: Int,
    val title: String,
    val posterUrl: String?,
    val releaseYear: Int?,
    val nominatedBy: List<String>,
    val ranking: Map<String, Int>,
)

/** A reviewed movie that can be nominated. */
data class NominationCandidate(
    val movieId: Int,
    val title: String,
    val posterUrl: String?,
    val releaseYear: Int?,
)
