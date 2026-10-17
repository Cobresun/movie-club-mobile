package cobresun.movieclub.app.reviews.domain

import cobresun.movieclub.app.reviews.domain.ScoreDialGeometry.CENTER_X
import cobresun.movieclub.app.reviews.domain.ScoreDialGeometry.CENTER_Y
import cobresun.movieclub.app.reviews.domain.ScoreDialGeometry.RADIUS
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals

class ScoreDialTest {
    private fun close(expected: Float, actual: Float) = assertEquals(expected, actual, 0.001f)

    @Test
    fun bandsLabelScoresByLowerBound() {
        listOf(
            0.0 to "Awful", 1.99 to "Awful", 2.0 to "Bad", 4.4 to "Meh", 5.5 to "Decent",
            7.1 to "Good", 8.5 to "Great", 9.99 to "Great", 10.0 to "Masterpiece",
        ).forEach { (score, label) -> assertEquals(label, ScoreBands.of(score).label, "score $score") }
    }

    @Test
    fun mapsScoresAcrossTheSemicircleClampingOutOfRange() {
        assertEquals(0f, ScoreDialGeometry.scoreToFraction(0.0))
        assertEquals(0.5f, ScoreDialGeometry.scoreToFraction(5.0))
        assertEquals(1f, ScoreDialGeometry.scoreToFraction(10.0))
        assertEquals(1f, ScoreDialGeometry.scoreToFraction(12.0))
        assertEquals(0f, ScoreDialGeometry.scoreToFraction(-3.0))
    }

    @Test
    fun placesTheHandleAlongTheArc() {
        val left = ScoreDialGeometry.handlePosition(0f)
        close(CENTER_X - RADIUS, left.x); close(CENTER_Y, left.y)
        val top = ScoreDialGeometry.handlePosition(0.5f)
        close(CENTER_X, top.x); close(CENTER_Y - RADIUS, top.y)
        val right = ScoreDialGeometry.handlePosition(1f)
        close(CENTER_X + RADIUS, right.x); close(CENTER_Y, right.y)
    }

    @Test
    fun mapsPointerAnglesBackToHalfStepScores() {
        assertEquals(5.0, ScoreDialGeometry.scoreFromPoint(CENTER_X, CENTER_Y - RADIUS))
        // The angle for 7.1 snaps to the nearest half step.
        val angle = PI * (1 - 0.71)
        assertEquals(
            7.0,
            ScoreDialGeometry.scoreFromPoint((CENTER_X + RADIUS * cos(angle)).toFloat(), (CENTER_Y - RADIUS * sin(angle)).toFloat()),
        )
        // Only the angle matters, not the distance: 45° above center is three quarters.
        assertEquals(7.5, ScoreDialGeometry.scoreFromPoint(CENTER_X + 10, CENTER_Y - 10))
        assertEquals(7.5, ScoreDialGeometry.scoreFromPoint(CENTER_X + 300, CENTER_Y - 300))
    }

    @Test
    fun roundTripsEveryDetent() {
        var score = 0.0
        while (score <= 10.0) {
            val p = ScoreDialGeometry.handlePosition(ScoreDialGeometry.scoreToFraction(score))
            assertEquals(score, ScoreDialGeometry.scoreFromPoint(p.x, p.y))
            score += ScoreScale.STEP
        }
    }

    @Test
    fun clampsPointsAtOrBelowTheBaselineToTheNearestEnd() {
        assertEquals(0.0, ScoreDialGeometry.scoreFromPoint(CENTER_X - 50, CENTER_Y + 30))
        assertEquals(10.0, ScoreDialGeometry.scoreFromPoint(CENTER_X + 50, CENTER_Y + 30))
        assertEquals(0.0, ScoreDialGeometry.scoreFromPoint(CENTER_X - RADIUS, CENTER_Y))
        assertEquals(10.0, ScoreDialGeometry.scoreFromPoint(CENTER_X + RADIUS, CENTER_Y))
    }

    @Test
    fun sanitizeLeavesLegalInputUntouched() {
        listOf("", "0", "0.5", "8", "8.5", "8.25", "10", "8.").forEach {
            assertEquals(ScoreScale.Sanitized(it, corrected = false), ScoreScale.sanitize(it))
        }
        // A comma decimal separator is normalised without counting as a correction.
        assertEquals(ScoreScale.Sanitized("7.5", corrected = false), ScoreScale.sanitize("7,5"))
    }

    @Test
    fun sanitizeFlagsCorrections() {
        assertEquals(ScoreScale.Sanitized("0", corrected = true), ScoreScale.sanitize("00"))
        assertEquals(ScoreScale.Sanitized("8", corrected = true), ScoreScale.sanitize("08"))
        assertEquals(ScoreScale.Sanitized("0.5", corrected = true), ScoreScale.sanitize("00.5"))
        assertEquals(ScoreScale.Sanitized("8.55", corrected = true), ScoreScale.sanitize("8.555"))
        assertEquals(ScoreScale.Sanitized("7.55", corrected = true), ScoreScale.sanitize("007.555"))
        assertEquals(ScoreScale.Sanitized("8", corrected = true), ScoreScale.sanitize("8a"))
    }

    @Test
    fun sanitizeClampsToTheMaximum() {
        assertEquals(ScoreScale.Sanitized("10", corrected = true, clampedTo = ScoreScale.Bound.MAX), ScoreScale.sanitize("12"))
        assertEquals(ScoreScale.Sanitized("10", corrected = true, clampedTo = ScoreScale.Bound.MAX), ScoreScale.sanitize("10.5"))
    }
}
