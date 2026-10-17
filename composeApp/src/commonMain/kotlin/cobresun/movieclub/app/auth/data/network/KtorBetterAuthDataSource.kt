package cobresun.movieclub.app.auth.data.network

import cobresun.movieclub.app.auth.data.dto.BetterAuthUserDto
import cobresun.movieclub.app.auth.data.dto.GetSessionResponseDto
import cobresun.movieclub.app.auth.data.dto.SendVerificationEmailRequestDto
import cobresun.movieclub.app.auth.data.dto.SignInRequestDto
import cobresun.movieclub.app.auth.data.dto.SignInResponseDto
import cobresun.movieclub.app.auth.data.dto.SignUpRequestDto
import cobresun.movieclub.app.auth.data.dto.SignUpResponseDto
import cobresun.movieclub.app.auth.data.dto.SocialSignInRequestDto
import cobresun.movieclub.app.auth.data.dto.SocialSignInResponseDto
import cobresun.movieclub.app.core.data.classifyNetworkException
import cobresun.movieclub.app.core.data.safeCall
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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

private val sessionJson = Json { ignoreUnknownKeys = true }

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

    override suspend fun signInWithIdToken(
        request: SocialSignInRequestDto
    ): Result<BetterAuthUserDto, DataError.Remote> {
        val result = safeCall<SocialSignInResponseDto> {
            httpClient.post("$BASE_URL/api/auth/sign-in/social") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
        return when (result) {
            is Result.Success -> result.data.user?.let { Result.Success(it) }
                ?: Result.Error(DataError.Remote.SERIALIZATION)
            is Result.Error -> result
        }
    }

    override suspend fun signOut(): Result<Unit, DataError.Remote> {
        return safeCall<Unit> {
            httpClient.post("$BASE_URL/api/auth/sign-out")
        }
    }

    /**
     * Success(null) only when the server says there is no session; anything that
     * stops us from asking (offline, timeout, server error) is an Error, so a cold
     * start without a connection doesn't read as "signed out".
     */
    override suspend fun getSession(): Result<BetterAuthUserDto?, DataError.Remote> {
        val response = try {
            httpClient.get("$BASE_URL/api/auth/get-session")
        } catch (e: Exception) {
            currentCoroutineContext().ensureActive()
            return Result.Error(classifyNetworkException(e))
        }
        return when (response.status.value) {
            in 200..299 -> try {
                val bodyText = response.body<String>().trim()
                // Better Auth returns a literal "null" when no session exists.
                if (bodyText.isEmpty() || bodyText == "null") {
                    Result.Success(null)
                } else {
                    Result.Success(sessionJson.decodeFromString<GetSessionResponseDto>(bodyText).user)
                }
            } catch (e: Exception) {
                currentCoroutineContext().ensureActive()
                Result.Error(DataError.Remote.SERIALIZATION)
            }
            401, 403 -> Result.Success(null)
            408 -> Result.Error(DataError.Remote.REQUEST_TIMEOUT)
            429 -> Result.Error(DataError.Remote.TOO_MANY_REQUESTS)
            in 500..599 -> Result.Error(DataError.Remote.SERVER)
            else -> Result.Error(DataError.Remote.UNKNOWN)
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
