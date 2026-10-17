package cobresun.movieclub.app.reviews

import cobresun.movieclub.app.reviews.data.dto.SharedReviewDto
import cobresun.movieclub.app.reviews.data.mappers.toSharedReview
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SharedReviewMapperTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun mapsMembersWithTheirScoresAndKeepsUnscoredMembers() {
        val dto = json.decodeFromString<SharedReviewDto>(
            """
            {
              "reviews": [
                {"review_id": "r1", "score": 8.5, "user_id": "u1", "created_date": "2026-01-02T10:00:00.000Z"},
                {"review_id": "r2", "score": "6", "user_id": "u2", "created_date": "2026-01-03T10:00:00.000Z"}
              ],
              "members": [
                {"id": "u1", "name": "Ann", "image": null, "email": "a@x", "role": "owner"},
                {"id": "u2", "name": "Bo"},
                {"id": "u3", "name": "Cy"}
              ],
              "comments": [
                {"id": "c1", "workId": "w1", "userId": "u1", "userName": "Ann", "content": "Great", "createdDate": "2026-01-04T10:00:00.000Z", "spoiler": false}
              ],
              "work": {"id": "w1", "title": "Alien", "type": "movie", "externalId": "348"},
              "clubName": "Mobile Test Club"
            }
            """.trimIndent()
        )
        val review = dto.toSharedReview(TimeZone.UTC)
        assertEquals("Mobile Test Club", review.clubName)
        assertEquals("Alien", review.work.title)
        assertEquals(listOf(8.5, 6.0, null), review.members.map { it.score })
        assertNull(review.members[2].scoredAt)
        assertEquals(7.25, review.average)
        assertEquals("Great", review.comments.single().content)
    }

    @Test
    fun nobodyScoredWhenTheOnlyRowIsTheEmptyJoin() {
        val dto = json.decodeFromString<SharedReviewDto>(
            """{"reviews": [{"review_id": null, "score": null, "user_id": null, "created_date": null}],
               "members": [{"id": "u1", "name": "Ann"}], "work": {"id": "w1", "title": "Alien"}}"""
        )
        val review = dto.toSharedReview(TimeZone.UTC)
        assertNull(review.average)
        assertNull(review.members.single().score)
    }
}
