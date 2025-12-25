package cobresun.movieclub.app.auth.data.network

import cobresun.movieclub.app.auth.data.dto.SignupDataDto
import cobresun.movieclub.app.auth.data.dto.SignupRequestDto
import cobresun.movieclub.app.auth.data.dto.TokenDto
import cobresun.movieclub.app.auth.data.dto.UserDto
import cobresun.movieclub.app.core.data.invalidateBearerTokens
import cobresun.movieclub.app.core.data.safeCall
import cobresun.movieclub.app.core.domain.Constants.BASE_URL
import cobresun.movieclub.app.core.domain.Constants.TOKEN_EXCHANGE_URL
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.parameters

class KtorIdentityDataSource(
    private val httpClient: HttpClient
) : IdentityDataSource {

    override suspend fun login(
        email: String,
        password: String
    ): Result<TokenDto, DataError.Remote> {
        httpClient.invalidateBearerTokens()

        return safeCall<TokenDto> {
            httpClient.submitForm(
                url = TOKEN_EXCHANGE_URL,
                formParameters = parameters {
                    append("grant_type", "password")
                    append("username", email)
                    append("password", password)
                }
            )
        }
    }

    override suspend fun signup(
        email: String,
        password: String,
        fullName: String
    ): Result<TokenDto, DataError.Remote> {
        httpClient.invalidateBearerTokens()

        // Step 1: Create user account (returns UserDto)
        val signupResult = safeCall<UserDto> {
            httpClient.post("$BASE_URL/.netlify/identity/signup") {
                contentType(ContentType.Application.Json)
                setBody(
                    SignupRequestDto(
                        email = email,
                        password = password,
                        data = SignupDataDto(fullName = fullName)
                    )
                )
            }
        }

        // Step 2: Authenticate the newly created user (returns TokenDto)
        return when (signupResult) {
            is Result.Error -> Result.Error(signupResult.error)
            is Result.Success -> login(email, password)
        }
    }

    override suspend fun getUser(): Result<UserDto, DataError.Remote> {
        return safeCall<UserDto> {
            httpClient.get("$BASE_URL/.netlify/identity/user")
        }
    }
}
