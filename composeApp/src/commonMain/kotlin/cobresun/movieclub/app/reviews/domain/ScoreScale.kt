package cobresun.movieclub.app.reviews.domain

import kotlin.math.round

/**
 * The 0–10 review score scale (mirrors the web's `scoreScale.ts`). The slider
 * snaps to [STEP]; typed input may carry up to two decimals (e.g. 8.25).
 */
object ScoreScale {
    const val MIN = 0.0
    const val MAX = 10.0
    const val STEP = 0.5

    fun clamp(score: Double): Double = score.coerceIn(MIN, MAX)

    fun isValid(score: Double?): Boolean = score != null && !score.isNaN() && score in MIN..MAX

    /** Snaps a slider position to the nearest [STEP]. */
    fun snap(score: Double): Double = clamp(round(score / STEP) * STEP)

    /** "7.5", "8", "8.25" — trims float noise and trailing zeros. */
    fun format(score: Double): String {
        val rounded = round(score * 100) / 100
        return if (rounded == rounded.toLong().toDouble()) {
            rounded.toLong().toString()
        } else {
            rounded.toString().trimEnd('0').trimEnd('.')
        }
    }

    /**
     * Normalises typed score text: drops non-numeric characters and leading
     * zeros, keeps at most two decimals and clamps to the scale. Partial input
     * the user is still typing ("", "8.") passes through.
     */
    fun sanitizeInput(raw: String): String {
        var value = raw.replace(',', '.').filter { it.isDigit() || it == '.' }
        val firstDot = value.indexOf('.')
        if (firstDot >= 0) {
            value = value.substring(0, firstDot + 1) + value.substring(firstDot + 1).replace(".", "")
        }
        value = value.replace(Regex("^0+(\\d)"), "$1")
        Regex("^(\\d*\\.\\d{2})\\d+").find(value)?.let { value = it.groupValues[1] }
        val parsed = value.toDoubleOrNull() ?: return value
        return when {
            parsed > MAX -> format(MAX)
            parsed < MIN -> format(MIN)
            else -> value
        }
    }
}
