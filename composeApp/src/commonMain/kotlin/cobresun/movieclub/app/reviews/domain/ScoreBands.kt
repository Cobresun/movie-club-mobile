package cobresun.movieclub.app.reviews.domain

/**
 * A verdict band on the 0–10 scale (mirrors web `scoreBands.ts`): each score
 * maps to a label and an accent used for the dial's arc, its readout and the
 * clamp notice pill.
 *
 * @property min Inclusive lower bound; a score belongs to the last band it reaches.
 * @property argb Accent color as 0xAARRGGBB, tuned for the dark background.
 */
data class ScoreBand(val min: Double, val label: String, val argb: Long)

object ScoreBands {
    val all: List<ScoreBand> = listOf(
        ScoreBand(0.0, "Awful", 0xFFEF4444),
        ScoreBand(2.0, "Bad", 0xFFF97316),
        ScoreBand(4.0, "Meh", 0xFFEAB308),
        ScoreBand(5.5, "Decent", 0xFFCDDC39),
        ScoreBand(7.0, "Good", 0xFF84CC16),
        ScoreBand(8.5, "Great", 0xFF22C55E),
        ScoreBand(10.0, "Masterpiece", 0xFF00E054),
    )

    fun of(score: Double): ScoreBand = all.lastOrNull { score >= it.min } ?: all.first()
}
