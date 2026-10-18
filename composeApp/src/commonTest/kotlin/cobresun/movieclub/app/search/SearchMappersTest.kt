package cobresun.movieclub.app.search

import cobresun.movieclub.app.core.domain.WorkType
import cobresun.movieclub.app.search.data.dto.GoogleBooksImageLinksDto
import cobresun.movieclub.app.search.data.dto.GoogleBooksVolumeDto
import cobresun.movieclub.app.search.data.dto.GoogleBooksVolumeInfoDto
import cobresun.movieclub.app.search.data.dto.TmdbMovieDto
import cobresun.movieclub.app.search.data.mappers.toSearchResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import cobresun.movieclub.app.search.data.mappers.similarToReason

class SearchMappersTest {
    @Test
    fun mapsTmdbMovie() {
        val result = TmdbMovieDto(id = 27205, title = "Inception", releaseDate = "2010-07-15", posterPath = "/p.jpg").toSearchResult()
        assertEquals(WorkType.MOVIE, result.type)
        assertEquals("27205", result.externalId)
        assertEquals("2010", result.subtitle)
        assertEquals("https://image.tmdb.org/t/p/w342/p.jpg", result.imageUrl)
    }

    @Test
    fun movieWithoutDateOrPoster() {
        val result = TmdbMovieDto(id = 1, title = "X", releaseDate = "").toSearchResult()
        assertNull(result.subtitle)
        assertNull(result.imageUrl)
    }

    @Test
    fun mapsGoogleBookAndSecuresCover() {
        val result = GoogleBooksVolumeDto(
            id = "vol1",
            volumeInfo = GoogleBooksVolumeInfoDto(
                title = "Dune",
                authors = listOf("Frank Herbert"),
                publishedDate = "1965-08-01",
                imageLinks = GoogleBooksImageLinksDto(thumbnail = "http://books.google.com/c?id=1&edge=curl&zoom=1"),
            ),
        ).toSearchResult()
        assertEquals(WorkType.BOOK, result.type)
        assertEquals("Frank Herbert · 1965", result.subtitle)
        assertEquals("https://books.google.com/c?id=1&zoom=1", result.imageUrl)
    }

    @Test
    fun untitledBook() {
        val result = GoogleBooksVolumeDto(id = "v").toSearchResult()
        assertEquals("Untitled", result.title)
        assertNull(result.subtitle)
    }
}

class SimilarToReasonTest {
    @Test
    fun formatsLikeIntlListFormat() {
        assertNull(similarToReason(emptyList()))
        assertEquals("Similar to Alien", similarToReason(listOf("Alien")))
        assertEquals("Similar to Alien and Heat", similarToReason(listOf("Alien", "Heat")))
        assertEquals(
            "Similar to Alien, Heat, and Up",
            similarToReason(listOf("Alien", "Heat", "Up")),
        )
    }
}
