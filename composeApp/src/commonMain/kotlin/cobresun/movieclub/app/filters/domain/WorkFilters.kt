package cobresun.movieclub.app.filters.domain

import cobresun.movieclub.app.core.domain.ClubType

/** What the user has asked for: picked filters by option key, plus the free-text title search. */
data class FilterQuery(
    val selections: Map<String, FilterSelection> = emptyMap(),
    val text: String = "",
) {
    fun with(key: String, selection: FilterSelection?): FilterQuery =
        copy(selections = if (selection == null) selections - key else selections + (key to selection))

    fun cleared(): FilterQuery = copy(selections = emptyMap())

    /** Drops one active chip: a single choice value, or a whole range. */
    fun without(chip: ActiveFilterChip): FilterQuery {
        val selection = selections[chip.optionKey]
        if (chip.value == null || selection !is FilterSelection.Choice) return with(chip.optionKey, null)
        val values = selection.values - chip.value
        return with(chip.optionKey, values.takeIf { it.isNotEmpty() }?.let { FilterSelection.Choice(it) })
    }
}

/** One removable chip under the search bar ("Horror", "8+"). [value] is null for a range. */
data class ActiveFilterChip(
    val optionKey: String,
    val icon: FilterIcon,
    val label: String,
    val text: String,
    val value: String?,
) {
    val id: String get() = if (value == null) optionKey else "$optionKey:$value"
}

/** Web `includesCaseInsensitive` over the title: the free text never searches metadata. */
fun FilterTarget.matchesText(text: String): Boolean = work.title.contains(text.trim(), ignoreCase = true)

/** A work with no value for a range filter never matches an active one. */
fun FilterSelection.Range.includes(value: Double?): Boolean {
    if (value == null) return false
    return (from == null || value >= from) && (to == null || value <= to)
}

fun FilterOption.matches(target: FilterTarget, selection: FilterSelection): Boolean = when (this) {
    // Picking several values from one filter widens it: any of them will do.
    is ChoiceFilterOption -> selection is FilterSelection.Choice && values(target).any { it in selection.values }
    is RangeFilterOption -> selection is FilterSelection.Range && selection.includes(value(target))
}

/** Whether any of [targets] has a value for this option (an option nobody has would be an empty section). */
fun FilterOption.hasValues(targets: List<FilterTarget>): Boolean = targets.any { t ->
    when (this) {
        is ChoiceFilterOption -> values(t).isNotEmpty()
        is RangeFilterOption -> value(t) != null
    }
}

/** "1990 – 1999", "8+", "Up to 1h 30m" — how an applied range reads back. */
fun FilterSelection.Range.describe(format: (Double) -> String): String = when {
    from != null && to != null -> if (from == to) format(from) else "${format(from)} – ${format(to)}"
    from != null -> "${format(from)}+"
    to != null -> "Up to ${format(to)}"
    else -> "Any"
}

/**
 * Filters [items] down to those matching every selection plus the title search
 * (web `filterWorks`). Filters AND together; values within one choice OR, so
 * "Horror" + "Comedy" shows both.
 */
fun <T> filterItems(
    items: List<T>,
    query: FilterQuery,
    options: List<FilterOption>,
    target: (T) -> FilterTarget,
): List<T> {
    val active = options.mapNotNull { option -> query.selections[option.key]?.let { option to it } }
    val text = query.text.trim()
    if (active.isEmpty() && text.isEmpty()) return items
    return items.filter { item ->
        val t = target(item)
        active.all { (option, selection) -> option.matches(t, selection) } && (text.isEmpty() || t.matchesText(text))
    }
}

/**
 * Everything the search filter bar and its sheet show for one set of works
 * (web `SearchFilterBar`'s computed state).
 */
class FilterResults<T>(
    /** The filters worth offering: not excluded, and with at least one work having a value. */
    val options: List<FilterOption>,
    /** Every (deduplicated) work: which values exist, their order, the slider ends. */
    val all: List<FilterTarget>,
    /** Per option key: the works every *other* filter leaves, so a dead-end value counts zero. */
    val facets: Map<String, List<FilterTarget>>,
    /** The matching items, in their incoming order. */
    val filtered: List<T>,
    val chips: List<ActiveFilterChip>,
    /** Any filter or search text in play. */
    val isActive: Boolean,
    /** "Show 12 movies", "Show 1 book", "No movies match". */
    val resultLabel: String,
) {
    fun facet(key: String): List<FilterTarget> = facets[key].orEmpty()
}

/**
 * Builds [FilterResults] for [items]. The same work can sit on several lists,
 * so works are counted and filtered once, keyed by [id]; [filtered] keeps every
 * incoming item whose work matches.
 */
fun <T> computeFilters(
    items: List<T>,
    query: FilterQuery,
    clubType: ClubType,
    excludeKeys: Set<String> = emptySet(),
    id: (T) -> String,
    target: (T) -> FilterTarget,
): FilterResults<T> {
    val unique = items.distinctBy(id)
    val all = unique.map(target)
    val registry = ClubFilters.options(clubType)
    val options = registry.filter { it.key !in excludeKeys && it.hasValues(all) }
    // Selections for options that aren't on offer can't be seen or cleared, so they don't apply.
    val applied = query.copy(selections = query.selections.filterKeys { key -> options.any { it.key == key } })

    val matchingIds = filterItems(unique, applied, options, target).mapTo(HashSet(), id)
    val filtered = items.filter { id(it) in matchingIds }

    val facets = options.associate { option ->
        option.key to filterItems(all, applied.with(option.key, null), options) { it }
    }

    val chips = options.flatMap { option ->
        when (val selection = applied.selections[option.key]) {
            null -> emptyList()
            is FilterSelection.Range -> listOfNotNull(
                (option as? RangeFilterOption)?.let {
                    ActiveFilterChip(option.key, option.icon, option.label, selection.describe(it.format), null)
                }
            )
            is FilterSelection.Choice -> selection.values.map { value ->
                ActiveFilterChip(option.key, option.icon, option.label, value, value)
            }
        }
    }

    val count = matchingIds.size
    val resultLabel = when (count) {
        0 -> "No ${ClubFilters.pluralNoun(clubType)} match"
        1 -> "Show 1 ${ClubFilters.noun(clubType)}"
        else -> "Show $count ${ClubFilters.pluralNoun(clubType)}"
    }

    return FilterResults(
        options = options,
        all = all,
        facets = facets,
        filtered = filtered,
        chips = chips,
        isActive = chips.isNotEmpty() || query.text.isNotBlank(),
        resultLabel = resultLabel,
    )
}
