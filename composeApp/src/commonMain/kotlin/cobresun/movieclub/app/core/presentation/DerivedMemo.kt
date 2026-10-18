package cobresun.movieclub.app.core.presentation

/**
 * Caches values derived from a state data class across its `copy()`s.
 *
 * A `by lazy` on a data class is rebuilt with every copy, so derived work
 * (filtering, sorting, facets over every item) reran for each emission,
 * however unrelated (a refresh flag, a sheet opening). Pass the same memo
 * through copies (a constructor default does that) and each slot recomputes
 * only when its inputs change: by identity, which unchanged lists keep across
 * copies, else by equality.
 *
 * It never affects the state's equality or hash.
 */
class DerivedMemo {
    private class Slot(val inputs: List<Any?>, val value: Any?)

    private val slots = HashMap<String, Slot>()

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String, vararg inputs: Any?, compute: () -> T): T {
        val slot = slots[key]
        if (slot != null && slot.inputs.size == inputs.size &&
            slot.inputs.indices.all { slot.inputs[it] === inputs[it] || slot.inputs[it] == inputs[it] }
        ) return slot.value as T
        return compute().also { slots[key] = Slot(inputs.toList(), it) }
    }

    override fun equals(other: Any?): Boolean = other is DerivedMemo
    override fun hashCode(): Int = 0
}
