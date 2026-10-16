package cobresun.movieclub.app.auth

import cobresun.movieclub.app.auth.data.dto.SocialSignInResponseDto
import cobresun.movieclub.app.auth.data.mappers.toSocialSignInRequest
import cobresun.movieclub.app.auth.domain.AppleCredential
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class SocialSignInMappersTest {
    // Same configuration as HttpClientFactory's ContentNegotiation.
    private val json = Json { ignoreUnknownKeys = true }

    private fun encode(credential: AppleCredential): JsonObject =
        json.parseToJsonElement(json.encodeToString(credential.toSocialSignInRequest())).jsonObject

    @Test
    fun appleCredentialEncodesAsBetterAuthIdTokenBody() {
        val body = encode(AppleCredential("jwt", "raw-nonce", "Ada", "Lovelace"))

        assertEquals("apple", body["provider"]!!.jsonPrimitive.content)
        val idToken = body["idToken"]!!.jsonObject
        assertEquals("jwt", idToken["token"]!!.jsonPrimitive.content)
        assertEquals("raw-nonce", idToken["nonce"]!!.jsonPrimitive.content)
        val name = idToken["user"]!!.jsonObject["name"]!!.jsonObject
        assertEquals("Ada", name["firstName"]!!.jsonPrimitive.content)
        assertEquals("Lovelace", name["lastName"]!!.jsonPrimitive.content)
    }

    @Test
    fun omitsAbsentFieldsInsteadOfSendingNulls() {
        val idToken = encode(AppleCredential("jwt", null, "", null))["idToken"]!!.jsonObject

        assertFalse("user" in idToken)
        assertFalse("nonce" in idToken)
    }

    @Test
    fun parsesIdTokenSignInResponse() {
        val response = json.decodeFromString<SocialSignInResponseDto>(
            """{"redirect":false,"token":"t","user":{"id":"42","email":"a@privaterelay.appleid.com","name":"","emailVerified":true}}"""
        )

        assertFalse(response.redirect)
        assertEquals("42", response.user?.id)
        assertEquals("a@privaterelay.appleid.com", response.user?.email)
    }

    @Test
    fun parsesResponseWithoutUser() {
        val response = json.decodeFromString<SocialSignInResponseDto>("""{"url":"https://x","redirect":true}""")

        assertNull(response.user)
    }
}
