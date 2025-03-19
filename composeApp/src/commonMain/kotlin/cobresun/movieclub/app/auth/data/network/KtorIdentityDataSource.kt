package cobresun.movieclub.app.auth.data.network

import cobresun.movieclub.app.auth.data.dto.TokenDto
import cobresun.movieclub.app.auth.data.dto.UserDto
import cobresun.movieclub.app.core.data.invalidateBearerTokens
import cobresun.movieclub.app.core.data.safeCall
import cobresun.movieclub.app.core.domain.Constants.BASE_URL
import cobresun.movieclub.app.core.domain.Constants.TOKEN_EXCHANGE_URL
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.post

class KtorIdentityDataSource(
    private val httpClient: HttpClient
) : IdentityDataSource {

    override suspend fun login(
        email: String,
        password: String
    ): Result<TokenDto, DataError.Remote> {
        httpClient.invalidateBearerTokens()

        return safeCall<TokenDto> {
            httpClient.post(TOKEN_EXCHANGE_URL) {
                url {
                    parameters.append("username", email)
                    parameters.append("password", password)
                    parameters.append("grant_type", "password")
                }
            }
        }
    }

    override suspend fun getUser(): Result<UserDto, DataError.Remote> {
        return safeCall<UserDto> {
            httpClient.get("$BASE_URL/.netlify/identity/user")
        }
    }
}
