package cobresun.movieclub.app.auth.data.network

import cobresun.movieclub.app.auth.data.dto.TokenDto
import cobresun.movieclub.app.auth.data.dto.UserDto
import cobresun.movieclub.app.core.data.safeCall
import cobresun.movieclub.app.core.domain.Constants.BASE_URL
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import io.ktor.client.HttpClient
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post

class KtorIdentityDataSource(
    private val httpClient: HttpClient
) : IdentityDataSource {

    override suspend fun login(
        email: String,
        password: String
    ): Result<TokenDto, DataError.Remote> {
        return safeCall<TokenDto> {
            httpClient.post("$BASE_URL/.netlify/identity/token") {
                url {
                    parameters.append("username", email)
                    parameters.append("password", password)
                    parameters.append("grant_type", "password")
                }
            }
        }
    }

    override suspend fun refreshAccessToken(refreshToken: String): Result<TokenDto, DataError.Remote> {
        return safeCall<TokenDto> {
            httpClient.post("$BASE_URL/.netlify/identity/token") {
                url {
                    parameters.append("refresh_token", refreshToken)
                    parameters.append("grant_type", "refresh_token")
                }
            }
        }
    }

    override suspend fun getUser(token: String): Result<UserDto, DataError.Remote> {
        return safeCall<UserDto> {
            httpClient.get("$BASE_URL/.netlify/identity/user") {
                bearerAuth(token)
            }
        }
    }
}
