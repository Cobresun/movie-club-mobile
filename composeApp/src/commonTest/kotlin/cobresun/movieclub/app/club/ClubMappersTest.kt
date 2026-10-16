package cobresun.movieclub.app.club

import cobresun.movieclub.app.club.data.dto.ClubFeaturesDto
import cobresun.movieclub.app.club.data.dto.ClubSettingsDto
import cobresun.movieclub.app.club.data.dto.MemberDto
import cobresun.movieclub.app.club.data.mappers.toDomain
import cobresun.movieclub.app.club.data.network.parseServerMessage
import cobresun.movieclub.app.club.domain.ClubFeatures
import cobresun.movieclub.app.club.domain.MemberRole
import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.member.data.dto.ClubDto
import cobresun.movieclub.app.member.data.mappers.toClub
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ClubMappersTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun memberRoleMapping() {
        val admin = MemberDto("1", "a@b.c", "A", image = "", role = "admin").toDomain()
        assertEquals(MemberRole.ADMIN, admin.role)
        assertNull(admin.imageUrl)
        val member = MemberDto("2", "b@b.c", "B", image = "https://x/y.png").toDomain()
        assertEquals(MemberRole.MEMBER, member.role)
        assertEquals("https://x/y.png", member.imageUrl)
    }

    @Test
    fun settingsDecodeWithDefaults() {
        val dto = json.decodeFromString(ClubSettingsDto.serializer(), """{"features":{"awards":true}}""")
        assertEquals(ClubFeatures(awards = true, discussionQuestions = false), dto.features.toDomain())
        assertEquals(ClubFeatures(), ClubFeaturesDto().toDomain())
    }

    @Test
    fun clubTypeMapping() {
        assertEquals(ClubType.BOOK, ClubDto("1", "Books", "books", "book").toClub().type)
        assertEquals(ClubType.MOVIE, ClubDto("1", "Films", "films", null).toClub().type)
    }

    @Test
    fun serverMessageParsing() {
        assertEquals("This url is already in use by another club", parseServerMessage("""{"error":"This url is already in use by another club"}"""))
        assertEquals("Invalid password", parseServerMessage("""{"message":"Invalid password","code":"INVALID_PASSWORD"}"""))
        assertEquals("Joined club successfully", parseServerMessage("Joined club successfully"))
        assertNull(parseServerMessage(""))
        assertNull(parseServerMessage("<html>oops</html>"))
    }
}
