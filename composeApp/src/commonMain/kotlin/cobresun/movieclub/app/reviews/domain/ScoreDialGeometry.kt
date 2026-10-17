package cobresun.movieclub.app.reviews.domain

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.sin

/**
 * Layout of the score dial's semicircular track in a 200×112 design space
 * (mirrors web `scoreDialGeometry.ts`). The arc spans 180° from the left end
 * (score 0) over the top (score 5) to the right end (score 10), centered on
 * the bottom of the drawing area. Callers scale by `width / VIEWPORT_WIDTH`.
 */
object ScoreDialGeometry {
    const val VIEWPORT_WIDTH = 200f
    const val VIEWPORT_HEIGHT = 112f
    const val CENTER_X = 100f
    const val CENTER_Y = 100f
    const val RADIUS = 84f
    const val TRACK_WIDTH = 12f
    const val HANDLE_RADIUS = 11f

    data class Point(val x: Float, val y: Float)

    /** 0 at the left end of the arc, 1 at the right end. */
    fun scoreToFraction(score: Double): Float = (ScoreScale.clamp(score) / ScoreScale.MAX).toFloat()

    /** Design-space position of the handle for a fraction along the arc. */
    fun handlePosition(fraction: Float): Point {
        val angle = PI * (1 - fraction)
        return Point(
            x = (CENTER_X + RADIUS * cos(angle)).toFloat(),
            y = (CENTER_Y - RADIUS * sin(angle)).toFloat(),
        )
    }

    /**
     * Maps a pointer position (design space) to the nearest score, snapped to
     * [ScoreScale.STEP]. Only the angle around the arc's center matters, so
     * drags don't have to stay on the track; points at or below the baseline
     * clamp to the nearest end rather than wrapping around.
     */
    fun scoreFromPoint(x: Float, y: Float): Double {
        val dx = (x - CENTER_X).toDouble()
        val dy = (CENTER_Y - y).toDouble()
        if (dy <= 0) return if (dx < 0) ScoreScale.MIN else ScoreScale.MAX
        val fraction = 1 - atan2(dy, dx) / PI
        return ScoreScale.clamp(round(fraction * ScoreScale.MAX / ScoreScale.STEP) * ScoreScale.STEP)
    }
}
