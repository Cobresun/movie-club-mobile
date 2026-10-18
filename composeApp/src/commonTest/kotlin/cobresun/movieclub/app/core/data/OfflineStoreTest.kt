package cobresun.movieclub.app.core.data

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import okio.FileSystem
import kotlin.random.Random
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OfflineStoreTest {
    @Serializable
    private data class Item(val id: String, val score: Double? = null)

    private val directory = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "offline-store-test-${Random.nextLong()}"
    private val store = OfflineStore(directory)
    private val items = ListSerializer(Item.serializer())

    @AfterTest
    fun cleanUp() {
        FileSystem.SYSTEM.deleteRecursively(directory)
    }

    @Test
    fun savedValueLoadsBack() = runTest {
        val value = listOf(Item("a", 7.5), Item("b"))
        store.save("club/mobile-test-club/reviews", items, value)
        assertEquals(value, store.load("club/mobile-test-club/reviews", items))
    }

    @Test
    fun missingKeyLoadsNull() = runTest {
        assertNull(store.load("club/x/reviews", items))
    }

    @Test
    fun saveReplacesPreviousValue() = runTest {
        store.save("k", String.serializer(), "old")
        store.save("k", String.serializer(), "new")
        assertEquals("new", store.load("k", String.serializer()))
    }

    @Test
    fun undecodableFileLoadsNullAndIsDeleted() = runTest {
        store.save("k", String.serializer(), "text")
        // An older shape: the stored JSON is a string, not a list.
        assertNull(store.load("k", items))
        assertNull(store.load("k", String.serializer()))
    }

    @Test
    fun removePrefixDropsOnlyThatClub() = runTest {
        store.save("club/a/reviews", String.serializer(), "a1")
        store.save("club/a/lists", String.serializer(), "a2")
        store.save("club/ab/reviews", String.serializer(), "ab")
        store.save("clubs", String.serializer(), "all")

        store.removePrefix("club/a/")

        assertNull(store.load("club/a/reviews", String.serializer()))
        assertNull(store.load("club/a/lists", String.serializer()))
        assertEquals("ab", store.load("club/ab/reviews", String.serializer()))
        assertEquals("all", store.load("clubs", String.serializer()))
    }

    @Test
    fun clearForgetsEverythingAndStoreStaysUsable() = runTest {
        store.save("k", String.serializer(), "v")
        store.clear()
        assertNull(store.load("k", String.serializer()))
        store.save("k", String.serializer(), "again")
        assertEquals("again", store.load("k", String.serializer()))
    }

    @Test
    fun keyEncodingIsSafeDistinctAndPrefixPreserving() {
        val encoded = OfflineStore.encodeKey("club/my club/list/ü?")
        assertTrue(encoded.all { it.isLetterOrDigit() || it in "-_~" }, encoded)
        assertNotEquals(OfflineStore.encodeKey("a/b"), OfflineStore.encodeKey("a_b"))
        assertTrue(OfflineStore.encodeKey("club/a/reviews").startsWith(OfflineStore.encodeKey("club/a/")))
    }

    @Test
    fun readThroughStoresSuccess() = runTest {
        val result = store.readThrough("k", String.serializer()) { Result.Success("fresh") }
        assertEquals(Result.Success("fresh"), result)
        assertEquals("fresh", store.load("k", String.serializer()))
    }

    @Test
    fun readThroughServesStoredAnswerWhenOffline() = runTest {
        store.save("k", String.serializer(), "saved")
        for (error in listOf(DataError.Remote.NO_INTERNET, DataError.Remote.REQUEST_TIMEOUT, DataError.Remote.SERVER)) {
            val result = store.readThrough("k", String.serializer()) { Result.Error(error) }
            assertEquals(Result.Success("saved"), result, "for $error")
        }
    }

    @Test
    fun readThroughKeepsTheErrorWithoutAStoredAnswer() = runTest {
        val result = store.readThrough("k", String.serializer()) { Result.Error(DataError.Remote.NO_INTERNET) }
        assertEquals(Result.Error(DataError.Remote.NO_INTERNET), result)
    }

    @Test
    fun readThroughDoesNotHideRealAnswers() = runTest {
        // "Not found" / "forbidden" are the server's answer (e.g. removed from the club).
        store.save("k", String.serializer(), "saved")
        val result = store.readThrough("k", String.serializer()) { Result.Error(DataError.Remote.UNKNOWN) }
        assertEquals(Result.Error(DataError.Remote.UNKNOWN), result)
    }
}

class OfflineStoreDtoTest {
    private val directory = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "offline-dto-test-${Random.nextLong()}"

    @AfterTest
    fun cleanUp() {
        FileSystem.SYSTEM.deleteRecursively(directory)
    }

    @Test
    fun reviewsAsDecodedFromTheServerSurviveTheStore() = runTest {
        val serverJson = """
            [{"id":"w1","type":"movie","title":"Jaws","createdDate":"2026-10-16T00:00:00.000Z",
              "externalId":"578","unknownField":1,
              "scores":{"u1":{"id":"s1","score":7.0,"created_date":"2026-10-16"},"average":{"id":"average","score":7.0}},
              "externalData":{"kind":"movie","runtime":124,"genres":["Horror"]}}]
        """.trimIndent()
        val serializer = ListSerializer(cobresun.movieclub.app.reviews.data.dto.ReviewItemDto.serializer())
        val decoded = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }.decodeFromString(serializer, serverJson)

        val store = OfflineStore(directory)
        store.save("club/c/reviews", serializer, decoded)

        assertEquals(decoded, store.load("club/c/reviews", serializer))
    }
}
