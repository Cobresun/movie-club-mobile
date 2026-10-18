package cobresun.movieclub.app.core.data

import cobresun.movieclub.app.core.domain.Constants.BASE_URL
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object HttpClientFactory {
    fun create(engine: HttpClientEngine, cookieStorage: SessionCookieStorage): HttpClient {
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

            // A cold start's first request can fail on a connection that isn't ready yet.
            // Retry reads that failed before any response or hit a gateway error; writes and
            // timeouts aren't retried.
            install(HttpRequestRetry) {
                maxRetries = 2
                retryIf { request, response ->
                    request.method == HttpMethod.Get && response.status.value in 502..504
                }
                retryOnExceptionIf { request, cause ->
                    request.method == HttpMethod.Get && cause !is HttpRequestTimeoutException
                }
                exponentialDelay(base = 2.0, maxDelayMs = 2_000L)
            }

            install(Logging) {
                logger = object : Logger {
                    override fun log(message: String) {
                        println(message)
                    }
                }
                // Method, URL and status only: bodies carry passwords and headers carry the
                // session cookie, neither of which may reach device logs.
                level = LogLevel.INFO
                sanitizeHeader { it.equals("Cookie", true) || it.equals("Set-Cookie", true) }
            }

            install(HttpCookies) {
                storage = cookieStorage
            }

            defaultRequest {
                contentType(ContentType.Application.Json)
                headers.append(HttpHeaders.Origin, BASE_URL)
            }
        }
    }
}
