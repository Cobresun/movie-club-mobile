package cobresun.movieclub.app.filters

import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.domain.WorkDetails
import cobresun.movieclub.app.core.domain.WorkType
import cobresun.movieclub.app.filters.domain.ChoiceFilterOption
import cobresun.movieclub.app.filters.domain.ChoiceSection
import cobresun.movieclub.app.filters.domain.ClubFilters
import cobresun.movieclub.app.filters.domain.FilterQuery
import cobresun.movieclub.app.filters.domain.FilterSelection
import cobresun.movieclub.app.filters.domain.FilterTarget
import cobresun.movieclub.app.filters.domain.RangeFilterOption
import cobresun.movieclub.app.filters.domain.RangePreset
import cobresun.movieclub.app.filters.domain.RangeScale
import cobresun.movieclub.app.filters.domain.RangeSection
import cobresun.movieclub.app.filters.domain.computeFilters
import cobresun.movieclub.app.filters.domain.decadePresets
import cobresun.movieclub.app.filters.domain.describe
import cobresun.movieclub.app.filters.domain.filterItems
import cobresun.movieclub.app.filters.domain.formatScore
import cobresun.movieclub.app.filters.domain.includes
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import cobresun.movieclub.app.core.domain.formatRuntime

class WorkFiltersTest {

    private fun movie(
        id: String,
        title: String = id,
        genres: List<String> = emptyList(),
        release: String? = null,
        runtime: Int? = null,
        directors: List<String> = emptyList(),
        cast: List<String> = emptyList(),
        companies: List<String> = emptyList(),
        addedAt: String = "2024-05-01T00:00:00Z",
        score: Double? = null,
    ) = FilterTarget(
        Work(
            id = id, type = WorkType.MOVIE, title = title, addedAt = addedAt, addedDate = null,
            imageUrl = null, externalId = id, addedByUserId = null,
            details = WorkDetails.Movie(
                overview = null, tagline = null, releaseDate = release, runtimeMinutes = runtime,
                genres = genres, directors = directors, castNames = cast, voteAverage = null, backdropUrl = null,
                productionCompanies = companies,
            ),
        ),
        averageScore = score,
    )

    private fun book(
        id: String,
        authors: List<String> = emptyList(),
        subjects: List<String> = emptyList(),
        year: Int? = null,
        pages: Int? = null,
    ) = FilterTarget(
        Work(
            id = id, type = WorkType.BOOK, title = id, addedAt = "2023-01-01T00:00:00Z", addedDate = null,
            imageUrl = null, externalId = id, addedByUserId = null,
            details = WorkDetails.Book(null, authors, subjects, year, pages, null),
        ),
    )

    private val movieOptions = ClubFilters.options(ClubType.MOVIE)
    private val bookOptions = ClubFilters.options(ClubType.BOOK)
    private fun option(key: String, type: ClubType = ClubType.MOVIE) = ClubFilters.options(type).first { it.key == key }
    private fun range(key: String, type: ClubType = ClubType.MOVIE) = option(key, type) as RangeFilterOption
    private fun choice(key: String, type: ClubType = ClubType.MOVIE) = option(key, type) as ChoiceFilterOption

    private fun filter(targets: List<FilterTarget>, query: FilterQuery, type: ClubType = ClubType.MOVIE) =
        filterItems(targets, query, ClubFilters.options(type)) { it }.map { it.work.id }

    private fun FilterQuery.pick(key: String, vararg values: String) = with(key, FilterSelection.Choice(values.toList()))
    private fun FilterQuery.span(key: String, from: Double? = null, to: Double? = null) = with(key, FilterSelection.Range(from, to))

    // region registry

    @Test
    fun movieOptionsMatchTheWebInOrder() {
        assertEquals(
            listOf("genre", "average_score", "release_date", "runtime", "director", "actor", "company", "review_date"),
            movieOptions.map { it.key },
        )
        assertEquals(
            listOf("Genre", "Club score", "Release year", "Runtime", "Director", "Cast", "Studio", "Reviewed in"),
            movieOptions.map { it.label },
        )
    }

    @Test
    fun bookOptionsMatchTheWebInOrder() {
        assertEquals(
            listOf("author", "subject", "average_score", "first_publish_year", "pages", "review_date"),
            bookOptions.map { it.key },
        )
        assertEquals(
            listOf("Author", "Subject", "Club score", "First published", "Length", "Reviewed in"),
            bookOptions.map { it.label },
        )
    }

    @Test
    fun rangeStepsAndBucketsMatchTheWeb() {
        assertEquals(0.5 to 1.0, range("average_score").let { it.step to it.bucket })
        assertEquals(1.0 to 5.0, range("release_date").let { it.step to it.bucket })
        assertEquals(5.0 to 15.0, range("runtime").let { it.step to it.bucket })
        assertEquals(1.0 to 10.0, range("first_publish_year", ClubType.BOOK).let { it.step to it.bucket })
        assertEquals(10.0 to 50.0, range("pages", ClubType.BOOK).let { it.step to it.bucket })
    }

    @Test
    fun presetsMatchTheWeb() {
        assertEquals(
            listOf(
                RangePreset("Loved it", "8+", from = 8.0),
                RangePreset("Liked it", "6+", from = 6.0),
                RangePreset("Not for us", "5 and under", to = 5.0),
            ),
            range("average_score").presets(emptyList()),
        )
        assertEquals(
            listOf(
                RangePreset("Short & sweet", "under 1h 30m", to = 90.0),
                RangePreset("Feature length", "1h 30m – 2h", from = 90.0, to = 120.0),
                RangePreset("Epic", "2h 30m+", from = 150.0),
            ),
            range("runtime").presets(emptyList()),
        )
        assertEquals(
            listOf("Classics", "Modern", "This century"),
            range("first_publish_year", ClubType.BOOK).presets(emptyList()).map { it.label },
        )
        assertEquals(1949.0, range("first_publish_year", ClubType.BOOK).presets(emptyList())[0].to)
        assertEquals(
            listOf(RangePreset("Quick read", "under 200 pages", to = 200.0), RangePreset("Doorstopper", "500+ pages", from = 500.0)),
            range("pages", ClubType.BOOK).presets(emptyList()),
        )
    }

    @Test
    fun decadePresetsReadTheWayPeopleSayThem() {
        val presets = decadePresets(listOf(1994.0, 2015.0, 1999.0, 1987.0, 2011.0))
        assertEquals(listOf("’80s", "’90s", "2010s"), presets.map { it.label })
        assertEquals(1990.0 to 1999.0, presets[1].from to presets[1].to)
    }

    @Test
    fun formatsValuesLikeTheWeb() {
        assertEquals("8", formatScore(8.0))
        assertEquals("7.5", formatScore(7.5))
        assertEquals("6.67", formatScore(6.666666))
        assertEquals("1h 30m", formatRuntime(90))
        assertEquals("2h", formatRuntime(120))
        assertEquals("45m", formatRuntime(45))
        assertEquals("200 pages", range("pages", ClubType.BOOK).format(200.0))
        assertEquals("1999", range("release_date").format(1999.0))
    }

    // endregion

    // region matching

    @Test
    fun rangeKeepsBothEndsAndTreatsMissingEndsAsOpen() {
        val span = FilterSelection.Range(1990.0, 1999.0)
        assertTrue(span.includes(1990.0))
        assertTrue(span.includes(1999.0))
        assertFalse(span.includes(2000.0))
        assertTrue(FilterSelection.Range(from = 8.0).includes(10.0))
        assertTrue(FilterSelection.Range(to = 5.0).includes(1.0))
        assertFalse(span.includes(null))
    }

    @Test
    fun describesRanges() {
        val fmt: (Double) -> String = { formatScore(it) }
        assertEquals("1990 – 1999", FilterSelection.Range(1990.0, 1999.0).describe(fmt))
        assertEquals("8", FilterSelection.Range(8.0, 8.0).describe(fmt))
        assertEquals("8+", FilterSelection.Range(from = 8.0).describe(fmt))
        assertEquals("Up to 5", FilterSelection.Range(to = 5.0).describe(fmt))
        assertEquals("Any", FilterSelection.Range().describe(fmt))
    }

    private val alien = movie("alien", "Alien", listOf("Horror", "Sci-Fi"), "1979-05-25", 117, listOf("Ridley Scott"),
        listOf("Sigourney Weaver"), listOf("20th Century Fox"), "2023-02-01T00:00:00Z", 9.0)
    private val brazil = movie("brazil", "Brazil", listOf("Comedy", "Sci-Fi"), "1985-02-20", 142, listOf("Terry Gilliam"),
        listOf("Jonathan Pryce"), listOf("Embassy"), "2024-03-01T00:00:00Z", 6.5)
    private val heat = movie("heat", "Heat", listOf("Crime"), "1995-12-15", 170, listOf("Michael Mann"),
        listOf("Al Pacino", "Robert De Niro"), listOf("Warner Bros."), "2024-06-01T00:00:00Z", 8.0)
    private val unscored = movie("x", "Unscored Thing", listOf("Horror"))
    private val movies = listOf(alien, brazil, heat, unscored)

    @Test
    fun anyValueInOneChoiceWidensIt() {
        assertEquals(listOf("alien", "brazil", "x"), filter(movies, FilterQuery().pick("genre", "Horror", "Comedy")))
    }

    @Test
    fun separateFiltersAndTogether() {
        assertEquals(listOf("brazil"), filter(movies, FilterQuery().pick("genre", "Sci-Fi").span("release_date", from = 1980.0)))
    }

    @Test
    fun filtersByScoreRuntimeDirectorCastStudioAndReviewYear() {
        assertEquals(listOf("alien", "heat"), filter(movies, FilterQuery().span("average_score", from = 8.0)))
        assertEquals(listOf("heat"), filter(movies, FilterQuery().span("runtime", from = 150.0)))
        assertEquals(listOf("brazil"), filter(movies, FilterQuery().pick("director", "Terry Gilliam")))
        assertEquals(listOf("heat"), filter(movies, FilterQuery().pick("actor", "Robert De Niro")))
        assertEquals(listOf("alien"), filter(movies, FilterQuery().pick("company", "20th Century Fox")))
        assertEquals(listOf("brazil", "heat", "x"), filter(movies, FilterQuery().pick("review_date", "2024")))
    }

    @Test
    fun anActiveRangeDropsWorksWithoutTheValue() {
        assertFalse("x" in filter(movies, FilterQuery().span("average_score", to = 10.0)))
    }

    @Test
    fun freeTextMatchesTitleOnly() {
        assertEquals(listOf("alien"), filter(movies, FilterQuery(text = "  ALI ")))
        // A director's name is not a title.
        assertEquals(emptyList(), filter(movies, FilterQuery(text = "Ridley")))
    }

    @Test
    fun ignoresKeysTheClubTypeDoesNotOffer() {
        assertEquals(movies.map { it.work.id }, filter(movies, FilterQuery().pick("author", "Nobody")))
    }

    @Test
    fun filtersBooks() {
        val dune = book("dune", listOf("Frank Herbert"), listOf("Science fiction"), 1965, 412)
        val emma = book("emma", listOf("Jane Austen"), listOf("Romance"), 1815, 180)
        val books = listOf(dune, emma)
        assertEquals(listOf("emma"), filter(books, FilterQuery().pick("author", "Jane Austen"), ClubType.BOOK))
        assertEquals(listOf("dune"), filter(books, FilterQuery().pick("subject", "Science fiction"), ClubType.BOOK))
        assertEquals(listOf("emma"), filter(books, FilterQuery().span("first_publish_year", to = 1949.0), ClubType.BOOK))
        assertEquals(listOf("emma"), filter(books, FilterQuery().span("pages", to = 200.0), ClubType.BOOK))
    }

    // endregion

    // region computeFilters

    private fun compute(targets: List<FilterTarget>, query: FilterQuery, exclude: Set<String> = emptySet()) =
        computeFilters(targets, query, ClubType.MOVIE, exclude, id = { it.work.id }, target = { it })

    @Test
    fun offersOnlyOptionsSomeWorkHasAValueFor() {
        val bare = movie("bare", genres = listOf("Drama"))
        assertEquals(listOf("genre", "review_date"), compute(listOf(bare), FilterQuery()).options.map { it.key })
    }

    @Test
    fun excludedKeysDropOutAndTheirSelectionsStopApplying() {
        val results = compute(movies, FilterQuery().span("average_score", from = 9.0), ClubFilters.reviewOnlyKeys)
        assertFalse(results.options.any { it.key in ClubFilters.reviewOnlyKeys })
        assertEquals(4, results.filtered.size)
        assertTrue(results.chips.isEmpty())
    }

    @Test
    fun facetsCountAgainstTheOtherFilters() {
        val query = FilterQuery().pick("genre", "Horror").span("average_score", from = 8.0)
        val results = compute(movies, query)
        assertEquals(listOf("alien"), results.filtered.map { it.work.id })
        // Genre counts ignore the genre pick but respect the score span.
        assertEquals(listOf("alien", "heat"), results.facet("genre").map { it.work.id })
        // Score histogram ignores the score span but respects the genre pick.
        assertEquals(listOf("alien", "x"), results.facet("average_score").map { it.work.id })
    }

    @Test
    fun facetsRespectTheTitleSearch() {
        val results = compute(movies, FilterQuery(text = "heat"))
        assertEquals(listOf("heat"), results.facet("genre").map { it.work.id })
    }

    @Test
    fun countsAWorkOnSeveralListsOnceButKeepsEveryRow() {
        data class Row(val listId: String, val target: FilterTarget)
        val rows = listOf(Row("a", alien), Row("b", alien), Row("b", heat))
        val results = computeFilters(rows, FilterQuery().pick("genre", "Horror"), ClubType.MOVIE, id = { it.target.work.id }, target = { it.target })
        assertEquals(listOf("a", "b"), results.filtered.map { it.listId })
        assertEquals("Show 1 movie", results.resultLabel)
        assertEquals(2, results.all.size)
    }

    @Test
    fun resultLabelsMatchTheWeb() {
        assertEquals("Show 4 movies", compute(movies, FilterQuery()).resultLabel)
        assertEquals("No movies match", compute(movies, FilterQuery(text = "zzz")).resultLabel)
        val books = computeFilters(listOf(book("a", year = 2001), book("b", year = 1990)), FilterQuery(), ClubType.BOOK, id = { it.work.id }, target = { it })
        assertEquals("Show 2 books", books.resultLabel)
    }

    @Test
    fun chipsFollowTheRegistryOrderAndPickOrder() {
        val query = FilterQuery()
            .span("runtime", to = 90.0)
            .pick("genre", "Sci-Fi", "Crime")
        val chips = compute(movies, query).chips
        assertEquals(listOf("genre:Sci-Fi", "genre:Crime", "runtime"), chips.map { it.id })
        assertEquals("Up to 1h 30m", chips.last().text)
        assertEquals("Genre", chips.first().label)
    }

    @Test
    fun removingAChipDropsOneValueOrTheWholeRange() {
        val query = FilterQuery().pick("genre", "Sci-Fi", "Crime").span("runtime", to = 90.0)
        val chips = compute(movies, query).chips
        val afterGenre = query.without(chips[0])
        assertEquals(FilterSelection.Choice(listOf("Crime")), afterGenre.selections["genre"])
        val afterLastGenre = afterGenre.without(chips[1])
        assertNull(afterLastGenre.selections["genre"])
        assertNull(query.without(chips[2]).selections["runtime"])
    }

    @Test
    fun isActiveWithAFilterOrSearchText() {
        assertFalse(compute(movies, FilterQuery(text = "  ")).isActive)
        assertTrue(compute(movies, FilterQuery(text = "a")).isActive)
        assertTrue(compute(movies, FilterQuery().pick("genre", "Crime")).isActive)
        assertFalse(compute(movies, FilterQuery().pick("genre", "Crime").cleared()).isActive)
    }

    // endregion

    // region choice section

    @Test
    fun popularValuesOrderByOverallCountThenName() {
        assertEquals(listOf("Horror", "Sci-Fi", "Comedy", "Crime"), ChoiceSection.orderedValues(choice("genre"), movies))
    }

    @Test
    fun yearsOrderNewestFirst() {
        assertEquals(listOf("2024", "2023"), ChoiceSection.orderedValues(choice("review_date"), movies))
    }

    @Test
    fun tallyCountsAWorkOncePerValue() {
        val dup = movie("d", genres = listOf("Horror", "Horror"))
        assertEquals(mapOf("Horror" to 1), ChoiceSection.tally(choice("genre"), listOf(dup)))
    }

    @Test
    fun longListsFoldAndPinPicksFromFurtherDown() {
        val values = (1..25).map { "v$it" }
        assertTrue(ChoiceSection.isLong(values))
        assertFalse(ChoiceSection.isLong(values.take(20)))
        val folded = ChoiceSection.visibleValues(values, selected = listOf("v20", "v3"), search = "", expanded = false)
        assertEquals(listOf("v20") + values.take(12), folded)
        assertEquals(values, ChoiceSection.visibleValues(values, emptyList(), "", expanded = true))
        assertEquals(values.take(20), ChoiceSection.visibleValues(values.take(20), emptyList(), "", expanded = false))
        assertEquals(listOf("v2", "v20", "v21", "v22", "v23", "v24", "v25"), ChoiceSection.visibleValues(values, emptyList(), " V2 ", false))
    }

    @Test
    fun togglingAChoice() {
        val one = ChoiceSection.toggle(null, "Horror")
        assertEquals(FilterSelection.Choice(listOf("Horror")), one)
        assertEquals(FilterSelection.Choice(listOf("Horror", "Crime")), ChoiceSection.toggle(one, "Crime"))
        assertNull(ChoiceSection.toggle(one, "Horror"))
    }

    // endregion

    // region range section

    @Test
    fun scaleSnapsOutwardsToWholeBars() {
        val scale = RangeScale.of(range("release_date"), listOf(1979.0, 1985.0, 1995.0))!!
        assertEquals(RangeScale(1975.0, 1995.0, 5.0), scale)
        assertEquals(4, scale.barCount)
    }

    @Test
    fun aSingleValueStillGetsOneBar() {
        val scale = RangeScale.of(range("average_score"), listOf(8.0))!!
        assertEquals(8.0 to 9.0, scale.lo to scale.hi)
        assertNull(RangeScale.of(range("average_score"), emptyList()))
    }

    @Test
    fun longSpansWidenTheBarsToAtMostThirty() {
        val scale = RangeScale.of(range("first_publish_year", ClubType.BOOK), listOf(1600.0, 2020.0))!!
        assertEquals(20.0, scale.bucket)
        assertTrue(scale.barCount <= RangeScale.MAX_BARS)
        assertEquals(1600.0, scale.lo)
        assertEquals(2020.0, scale.hi)
    }

    @Test
    fun barsTallyTheFacetAndMarkTheSelectedSpan() {
        val option = range("release_date")
        val scale = RangeScale.of(option, listOf(1979.0, 1985.0, 1995.0))!!
        val unselected = scale.bars(option, movies, null)
        assertEquals(listOf(1, 0, 1, 1), unselected.map { it.count })
        assertTrue(unselected.none { it.selected })
        // The top value sits on the rail's end and lands in the last bar.
        val all = scale.bars(option, movies, FilterSelection.Range(from = 1985.0))
        assertEquals(listOf(false, false, true, true), all.map { it.selected })
        val withHeat = RangeScale(1975.0, 2000.0, 5.0).bars(option, movies, null)
        assertEquals(listOf(1, 0, 1, 0, 1), withHeat.map { it.count })
    }

    @Test
    fun thumbsOnTheRailEndsLeaveThatSideOpen() {
        val scale = RangeScale(0.0, 10.0, 1.0)
        assertNull(scale.selectionFor(0.0, 10.0))
        assertEquals(FilterSelection.Range(from = 6.0), scale.selectionFor(6.0, 10.0))
        assertEquals(FilterSelection.Range(to = 5.0), scale.selectionFor(0.0, 5.0))
        assertEquals(FilterSelection.Range(2.0, 7.5), scale.selectionFor(2.0, 7.5))
    }

    @Test
    fun thumbPositionsClampToTheRail() {
        val scale = RangeScale(1.0, 10.0, 1.0)
        assertEquals(1.0, scale.fromValue(null))
        assertEquals(10.0, scale.toValue(FilterSelection.Range(from = 8.0)))
        assertEquals(1.0, scale.fromValue(FilterSelection.Range(to = 0.0, from = -3.0)))
        assertEquals(7.5, scale.valueAt(0.72, 0.5))
        assertEquals(10.0, scale.valueAt(2.0, 0.5))
    }

    @Test
    fun presetsToggleAndKnowWhenTheyLeadNowhere() {
        val option = range("average_score")
        val loved = option.presets(emptyList())[0]
        val notForUs = option.presets(emptyList())[2]
        assertEquals(FilterSelection.Range(from = 8.0), RangeSection.togglePreset(loved, null))
        assertNull(RangeSection.togglePreset(loved, FilterSelection.Range(from = 8.0)))
        assertTrue(RangeSection.isPresetActive(loved, FilterSelection.Range(from = 8.0)))
        assertFalse(RangeSection.isPresetActive(loved, FilterSelection.Range(from = 8.0, to = 9.0)))
        assertTrue(RangeSection.presetHasMatches(option, loved, movies))
        assertFalse(RangeSection.presetHasMatches(option, notForUs, movies))
        assertEquals("Any", RangeSection.summary(option, null))
        assertEquals("8+", RangeSection.summary(option, FilterSelection.Range(from = 8.0)))
    }

    // endregion
}
