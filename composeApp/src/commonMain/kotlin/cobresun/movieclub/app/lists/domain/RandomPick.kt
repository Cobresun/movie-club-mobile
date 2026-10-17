package cobresun.movieclub.app.lists.domain

/**
 * Timing of the random picker's reel (web `useRandomPicker`): [CYCLES] random
 * flicks whose gaps ease out from [MIN_INTERVAL_MS] to [MAX_INTERVAL_MS], then
 * the winner, then a [REVEAL_PAUSE_MS] beat before the choices appear.
 */
object RandomPick {
    const val CYCLES = 20
    const val MIN_INTERVAL_MS = 80L
    const val MAX_INTERVAL_MS = 400L
    const val REVEAL_PAUSE_MS = 1500L

    /** The pause after flick [cycle] (1-based): quadratic ease-out deceleration. */
    fun intervalAfter(cycle: Int): Long {
        val t = cycle.toDouble() / CYCLES
        return MIN_INTERVAL_MS + ((MAX_INTERVAL_MS - MIN_INTERVAL_MS) * t * t).toLong()
    }
}
