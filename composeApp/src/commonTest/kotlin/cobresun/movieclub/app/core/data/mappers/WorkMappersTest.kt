package cobresun.movieclub.app.core.data.mappers

import cobresun.movieclub.app.core.data.dto.WorkListItemDto
import cobresun.movieclub.app.core.domain.WorkDetails
import cobresun.movieclub.app.core.domain.WorkType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class WorkMappersTest {
    private val json = Json { ignoreUnknownKeys = true }

    private val movieJson = """
        {"id":"1","title":"The Matrix","type":"movie","createdDate":"2026-10-16T13:38:10.585Z",
         "imageUrl":"https://image.tmdb.org/t/p/w154/f89.jpg","externalId":"603","addedBy":"u1",
         "externalData":{"kind":"movie","castNames":["Keanu Reeves"],"majorCastNames":[],
           "directors":[{"name":"Lana Wachowski","profilePath":"/a.jpg"},{"name":"Lilly Wachowski","profilePath":null}],
           "genres":["Action","Science Fiction"],"production_companies":[],"production_countries":[],
           "backdrop_path":"/back.jpg","poster_path":"/poster.jpg","release_date":"1999-03-31",
           "runtime":136,"tagline":"Welcome to the Real World.","vote_average":8.2,"overview":"A hacker…"}}
    """.trimIndent()

    @Test
    fun mapsMovieSummaryIntoDomain() {
        val work = json.decodeFromString<WorkListItemDto>(movieJson).toWork(TimeZone.UTC)

        assertEquals(WorkType.MOVIE, work.type)
        assertEquals(LocalDate(2026, 10, 16), work.addedDate)
        assertEquals("u1", work.addedByUserId)
        // Prefers a sharper poster from poster_path over the w154 thumbnail.
        assertEquals("https://image.tmdb.org/t/p/w342/poster.jpg", work.imageUrl)
        assertEquals(1999, work.year)
        val details = assertIs<WorkDetails.Movie>(work.details)
        assertEquals(listOf("Lana Wachowski", "Lilly Wachowski"), details.directors)
        assertEquals(listOf("Action", "Science Fiction"), details.genres)
        assertEquals(136, details.runtimeMinutes)
        assertEquals(8.2, details.voteAverage)
        assertEquals("https://image.tmdb.org/t/p/w780/back.jpg", details.backdropUrl)
        assertEquals(emptyList(), details.cast)
    }

    @Test
    fun mapsFullDetailsWithCast() {
        val element = json.parseToJsonElement(
            """{"kind":"movie","genres":[],"directors":[],"castNames":[],
               "actors":[{"name":"Leonardo DiCaprio","character":"Dom Cobb","profilePath":"/leo.jpg"},
                         {"name":"Nobody","character":null,"profilePath":null}]}"""
        )
        val details = assertIs<WorkDetails.Movie>(element.toWorkDetails())
        assertEquals(2, details.cast.size)
        assertEquals("Dom Cobb", details.cast[0].character)
        assertEquals("https://image.tmdb.org/t/p/w185/leo.jpg", details.cast[0].profileImageUrl)
        assertNull(details.cast[1].profileImageUrl)
    }

    @Test
    fun mapsBookSummary() {
        val dto = json.decodeFromString<WorkListItemDto>(
            """{"id":"2","title":"Dune","type":"book","createdDate":"2026-01-02T00:00:00.000Z",
               "imageUrl":"http://books.google.com/cover.jpg",
               "externalData":{"kind":"book","title":"Dune","authors":["Frank Herbert"],
                 "subjects":["Fiction"],"firstPublishYear":1965,"numberOfPages":412}}"""
        )
        val work = dto.toWork(TimeZone.UTC)

        assertEquals(WorkType.BOOK, work.type)
        assertEquals("https://books.google.com/cover.jpg", work.imageUrl)
        assertEquals(1965, work.year)
        val details = assertIs<WorkDetails.Book>(work.details)
        assertEquals(listOf("Frank Herbert"), details.authors)
        assertEquals(412, details.pageCount)
    }

    @Test
    fun toleratesMissingOrUnknownExternalData() {
        val dto = json.decodeFromString<WorkListItemDto>(
            """{"id":"3","title":"Mystery","createdDate":"not-a-date","externalData":{"kind":"podcast"}}"""
        )
        val work = dto.toWork(TimeZone.UTC)
        assertNull(work.details)
        assertNull(work.addedDate)
        assertNull(work.imageUrl)
        assertNull(work.year)
    }

    @Test
    fun treatsNullAsNoDetails() {
        assertNull(json.parseToJsonElement("null").toWorkDetails())
    }
}
