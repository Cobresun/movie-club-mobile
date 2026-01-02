package cobresun.movieclub.app.auth.data.network

import cobresun.movieclub.app.auth.data.dto.BetterAuthUserDto
import cobresun.movieclub.app.auth.data.dto.GetSessionResponseDto
import cobresun.movieclub.app.auth.data.dto.SendVerificationEmailRequestDto
import cobresun.movieclub.app.auth.data.dto.SignInRequestDto
import cobresun.movieclub.app.auth.data.dto.SignInResponseDto
import cobresun.movieclub.app.auth.data.dto.SignUpRequestDto
import cobresun.movieclub.app.auth.data.dto.SignUpResponseDto
import cobresun.movieclub.app.core.data.safeCall
import cobresun.movieclub.app.core.domain.Constants.BASE_URL
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.map
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.Json

class KtorBetterAuthDataSource(
    private val httpClient: HttpClient
) : BetterAuthDataSource {

    override suspend fun signUp(
        email: String,
        password: String,
        name: String
    ): Result<BetterAuthUserDto, DataError.Remote> {
        return safeCall<SignUpResponseDto> {
            httpClient.post("$BASE_URL/api/auth/sign-up/email") {
                contentType(ContentType.Application.Json)
                setBody(SignUpRequestDto(email, password, name))
            }
        }.map { response -> response.user }
    }

    override suspend fun signIn(
        email: String,
        password: String
    ): Result<BetterAuthUserDto, DataError.Remote> {
        return safeCall<SignInResponseDto> {
            httpClient.post("$BASE_URL/api/auth/sign-in/email") {
                contentType(ContentType.Application.Json)
                setBody(SignInRequestDto(email, password))
            }
        }.map { response -> response.user }
    }

    override suspend fun signOut(): Result<Unit, DataError.Remote> {
        return safeCall<Unit> {
            httpClient.post("$BASE_URL/api/auth/sign-out")
        }
    }

    override suspend fun getSession(): Result<BetterAuthUserDto?, DataError.Remote> {
        return try {
            val response = httpClient.get("$BASE_URL/api/auth/get-session")
            val bodyText = response.body<String>()

            // Better Auth returns literal "null" when no session exists
            if (bodyText.trim() == "null") {
                Result.Success(null)
            } else {
                // Parse the response as GetSessionResponseDto
                val json = Json { ignoreUnknownKeys = true }
                val sessionResponse = json.decodeFromString<GetSessionResponseDto>(bodyText)
                Result.Success(sessionResponse.user)
            }
        } catch (e: Exception) {
            Result.Error(DataError.Remote.UNKNOWN)
        }
    }

    override suspend fun sendVerificationEmail(email: String): Result<Unit, DataError.Remote> {
        return safeCall<Unit> {
            httpClient.post("$BASE_URL/api/auth/send-verification-email") {
                contentType(ContentType.Application.Json)
                setBody(SendVerificationEmailRequestDto(email))
            }
        }
    }
}
