package cobresun.movieclub.app.filters.domain

import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Web `ChoiceFilterSection`'s list logic: ordering, folding and the in-section search. */
object ChoiceSection {
    /** Folded lists show this many values. */
    const val COLLAPSED_COUNT = 12

    /** A search box only earns its place once scanning the chips stops being quicker. */
    const val LONG_LIST = 20

    /** How many works have each value (a work counts once per value). */
    fun tally(option: ChoiceFilterOption, targets: List<FilterTarget>): Map<String, Int> {
        val counts = HashMap<String, Int>()
        for (t in targets) {
            for (value in option.values(t).toSet()) counts[value] = (counts[value] ?: 0) + 1
        }
        return counts
    }

    /**
     * Values ordered by the whole club rather than the narrowed counts, so chips
     * hold still while other filters change around them.
     */
    fun orderedValues(option: ChoiceFilterOption, all: List<FilterTarget>): List<String> {
        val overall = tally(option, all)
        return when (option.order) {
            ChoiceOrder.NEWEST -> overall.keys.sortedWith(localeOrder.reversed())
            ChoiceOrder.POPULAR -> overall.keys.sortedWith(
                compareByDescending<String> { overall[it] ?: 0 }.then(localeOrder)
            )
        }
    }

    fun isLong(ordered: List<String>): Boolean = ordered.size > LONG_LIST

    /**
     * The chips to show: search matches while searching; otherwise the whole list
     * when short or expanded, or the top [COLLAPSED_COUNT] with any picks from
     * further down pinned in front so they stay in view once the list folds.
     */
    fun visibleValues(ordered: List<String>, selected: List<String>, search: String, expanded: Boolean): List<String> {
        val q = search.trim()
        if (q.isNotEmpty()) return ordered.filter { it.contains(q, ignoreCase = true) }
        if (!isLong(ordered) || expanded) return ordered
        val top = ordered.take(COLLAPSED_COUNT)
        return selected.filter { it !in top } + top
    }

    /** Toggles [value] in [current]; null once nothing is left picked. */
    fun toggle(current: FilterSelection.Choice?, value: String): FilterSelection.Choice? {
        val values = current?.values.orEmpty()
        val next = if (value in values) values - value else values + value
        return next.takeIf { it.isNotEmpty() }?.let { FilterSelection.Choice(it) }
    }

    // Approximates the web's `localeCompare`: case-insensitive, then exact.
    private val localeOrder: Comparator<String> = compareBy<String, String>(String.CASE_INSENSITIVE_ORDER) { it }.thenBy { it }
}

/** One histogram bar: [start] is its lower edge and [count] the works that fall in it. */
data class HistogramBar(val start: Double, val count: Int, val selected: Boolean)

/**
 * The slider's rail for a range option (web `RangeFilterSection` `scale`): its
 * ends snap outwards to whole histogram bars so the two line up, and a long
 * span (books published across centuries) widens the bars rather than drawing
 * hundreds of slivers.
 */
data class RangeScale(val lo: Double, val hi: Double, val bucket: Double) {
    val barCount: Int get() = max(1, ((hi - lo) / bucket).roundToInt())

    fun clamp(value: Double): Double = min(hi, max(lo, value))

    /** Where a thumb rests for [selection]: an open end sits on the rail's end. */
    fun fromValue(selection: FilterSelection.Range?): Double = clamp(selection?.from ?: lo)
    fun toValue(selection: FilterSelection.Range?): Double = clamp(selection?.to ?: hi)

    /** Fraction along the rail (0..1) for [value]. */
    fun fraction(value: Double): Double = (value - lo) / max(1e-9, hi - lo)

    /** The nearest [step] on the rail to [fraction] of the way along it. */
    fun valueAt(fraction: Double, step: Double): Double {
        val raw = lo + fraction.coerceIn(0.0, 1.0) * (hi - lo)
        return clamp(lo + (((raw - lo) / step).roundToInt() * step))
    }

    /**
     * The selection the thumbs at [from]/[to] describe. A thumb resting on the
     * end of the rail leaves that side open; both open means no filter.
     */
    fun selectionFor(from: Double, to: Double): FilterSelection.Range? {
        val next = FilterSelection.Range(from = from.takeIf { it > lo }, to = to.takeIf { it < hi })
        return next.takeUnless { it.from == null && it.to == null }
    }

    /** Bars over [facet]; a bar is selected when it overlaps the picked span. */
    fun bars(option: RangeFilterOption, facet: List<FilterTarget>, selection: FilterSelection.Range?): List<HistogramBar> {
        val count = barCount
        val tallies = IntArray(count)
        for (t in facet) {
            val value = option.value(t) ?: continue
            tallies[min(count - 1, max(0, floor((value - lo) / bucket).toInt()))]++
        }
        val from = fromValue(selection)
        val to = toValue(selection)
        return tallies.mapIndexed { index, tally ->
            val start = lo + index * bucket
            HistogramBar(start, tally, selected = selection != null && start + bucket > from && start <= to)
        }
    }

    companion object {
        const val MAX_BARS = 30

        fun of(option: RangeFilterOption, values: List<Double>): RangeScale? {
            if (values.isEmpty()) return null
            val minValue = values.min()
            val maxValue = values.max()
            fun bounds(size: Double): Pair<Double, Double> {
                val lo = floor(minValue / size) * size
                return lo to max(ceil(maxValue / size) * size, lo + size)
            }
            var bucket = option.bucket
            while (true) {
                val (lo, hi) = bounds(bucket)
                if ((hi - lo) / bucket <= MAX_BARS) return RangeScale(lo, hi, bucket)
                bucket *= 2
            }
        }
    }
}

object RangeSection {
    fun values(option: RangeFilterOption, targets: List<FilterTarget>): List<Double> = targets.mapNotNull(option.value)

    fun isPresetActive(preset: RangePreset, selection: FilterSelection.Range?): Boolean =
        selection != null && selection.from == preset.from && selection.to == preset.to

    /** Whether picking [preset] would leave anything, given the other filters ([facet]). */
    fun presetHasMatches(option: RangeFilterOption, preset: RangePreset, facet: List<FilterTarget>): Boolean {
        val span = FilterSelection.Range(preset.from, preset.to)
        return facet.any { span.includes(option.value(it)) }
    }

    fun togglePreset(preset: RangePreset, selection: FilterSelection.Range?): FilterSelection.Range? =
        if (isPresetActive(preset, selection)) null else FilterSelection.Range(preset.from, preset.to)

    /** The section header's summary: "Any" or the applied span. */
    fun summary(option: RangeFilterOption, selection: FilterSelection.Range?): String =
        selection?.describe(option.format) ?: "Any"
}
