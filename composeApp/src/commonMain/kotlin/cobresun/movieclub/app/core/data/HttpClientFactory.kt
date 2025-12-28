package cobresun.movieclub.app.core.data

import cobresun.movieclub.app.auth.data.dto.TokenDto
import cobresun.movieclub.app.core.domain.Constants.TOKEN_EXCHANGE_URL
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.authProvider
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.forms.submitForm
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object HttpClientFactory {
    fun create(engine: HttpClientEngine, bearerTokenStorage: BearerTokenStorage): HttpClient {
        return HttpClient(engine) {
            install(ContentNegotiation) {
                json(
                    json = Json {
                        ignoreUnknownKeys = true
                    }
                )
            }

            install(HttpTimeout) {
                socketTimeoutMillis = 20_000L
                requestTimeoutMillis = 20_000L
            }

            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println(message)
                    }
                }
                level = LogLevel.ALL
            }

            install(Auth) {
                bearer {
                    loadTokens {
                        bearerTokenStorage.getToken()
                    }

                    refreshTokens {
                        val refreshTokenInfo: TokenDto = client.submitForm(
                            url = TOKEN_EXCHANGE_URL,
                            formParameters = parameters {
                                append("grant_type", "refresh_token")
                                append("refresh_token", oldTokens?.refreshToken ?: "")
                            }
                        ) { markAsRefreshTokenRequest() }.body()

                        bearerTokenStorage.updateToken(
                            BearerTokens(
                                refreshTokenInfo.accessToken,
                                refreshTokenInfo.refreshToken
                            )
                        )

                        bearerTokenStorage.getToken()
                    }

                    sendWithoutRequest { request ->
                        request.url.host == "cobresun-movie-club.netlify.app"
                    }
                }
            }

            defaultRequest {
                contentType(ContentType.Application.Json)
            }
        }
    }
}

/**
 * Workaround for token caching [not working](https://youtrack.jetbrains.com/issue/KTOR-4759/Auth-BearerAuthProvider-caches-result-of-loadToken-until-process-death) when logging in
 */
fun HttpClient.invalidateBearerTokens() {
    authProvider<BearerAuthProvider>()?.clearToken()
}
