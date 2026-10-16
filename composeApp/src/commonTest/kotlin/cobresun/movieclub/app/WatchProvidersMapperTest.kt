package cobresun.movieclub.app

import cobresun.movieclub.app.providers.data.WatchProviderDto
import cobresun.movieclub.app.providers.data.WatchProvidersRegionDto
import cobresun.movieclub.app.providers.data.WatchProvidersResponseDto
import cobresun.movieclub.app.providers.data.toAvailability
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WatchProvidersMapperTest {
    private val dto = WatchProvidersResponseDto(
        results = mapOf(
            "US" to WatchProvidersRegionDto(
                link = "https://jw/us",
                flatrate = listOf(
                    WatchProviderDto(2, "Max", "/max.png", displayPriority = 5),
                    WatchProviderDto(1, "Netflix", "/nf.png", displayPriority = 1),
                ),
            ),
            "CA" to WatchProvidersRegionDto(link = "https://jw/ca", flatrate = emptyList()),
        ),
    )

    @Test
    fun sortsFlatrateByPriorityForRegion() {
        val a = dto.toAvailability("us")!!
        assertEquals(listOf("Netflix", "Max"), a.providers.map { it.name })
        assertEquals("https://image.tmdb.org/t/p/w92/nf.png", a.providers.first().logoUrl)
        assertEquals("https://jw/us", a.link)
    }

    @Test
    fun noFallbackToOtherRegions() {
        assertNull(dto.toAvailability("CA"))
        assertNull(dto.toAvailability("GB"))
    }
}
