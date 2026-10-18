package cobresun.movieclub.app.member.data.network

import cobresun.movieclub.app.club.data.dto.MemberDto
import cobresun.movieclub.app.club.data.dto.NameRequestDto
import cobresun.movieclub.app.core.data.apiCall
import cobresun.movieclub.app.core.domain.ApiError
import cobresun.movieclub.app.core.data.safeCall
import cobresun.movieclub.app.core.domain.Constants.BASE_URL
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.member.data.dto.ChangePasswordRequestDto
import cobresun.movieclub.app.member.data.dto.ClubDto
import cobresun.movieclub.app.member.data.dto.CreateClubRequestDto
import cobresun.movieclub.app.member.data.dto.CreateClubResponseDto
import cobresun.movieclub.app.member.data.dto.PasswordResetRequestDto
import io.ktor.client.HttpClient
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders

class KtorMemberDataSource(
    private val httpClient: HttpClient
) : MemberDataSource {
    override suspend fun getMember(): Result<MemberDto, DataError.Remote> {
        // The signed-in member is BetterAuth's session user; there is no /api/member.
        // Profile edits write the user row directly, so the signed session_data cookie
        // keeps the old name/avatar until it expires; `disableCookieCache` reads the
        // database (and rewrites the cookie), as the web app does after profile edits.
        return when (val result = safeCall<SessionUserDto> {
            httpClient.get("$BASE_URL/api/auth/get-session?disableCookieCache=true")
        }) {
            is Result.Success -> result.data.user
                ?.let { Result.Success(MemberDto(it.id, it.email, it.name, it.image)) }
                ?: Result.Error(DataError.Remote.UNKNOWN)
            is Result.Error -> result
        }
    }

    override suspend fun getClubs(): Result<List<ClubDto>, DataError.Remote> {
        return safeCall<List<ClubDto>> {
            httpClient.get("$BASE_URL/api/member/clubs")
        }
    }

    override suspend fun createClub(
        request: CreateClubRequestDto
    ): Result<CreateClubResponseDto, DataError.Remote> {
        return safeCall<CreateClubResponseDto> {
            httpClient.post("$BASE_URL/api/club") {
                setBody(request)
            }
        }
    }

    override suspend fun updateName(name: String): Result<Unit, ApiError> =
        apiCall { httpClient.put("$BASE_URL/api/member/name") { setBody(NameRequestDto(name)) } }

    override suspend fun uploadAvatar(bytes: ByteArray, mimeType: String): Result<Unit, ApiError> {
        val extension = when (mimeType) {
            "image/png" -> "png"
            "image/heic" -> "heic"
            else -> "jpg"
        }
        return apiCall {
            httpClient.post("$BASE_URL/api/member/avatar") {
                // The default request sets JSON; the multipart body supplies its own boundary type.
                headers.remove(HttpHeaders.ContentType)
                setBody(
                    MultiPartFormDataContent(
                        formData {
                            append(
                                key = "file",
                                value = bytes,
                                headers = Headers.build {
                                    append(HttpHeaders.ContentType, mimeType)
                                    append(
                                        HttpHeaders.ContentDisposition,
                                        "filename=\"avatar.$extension\""
                                    )
                                }
                            )
                        }
                    )
                )
            }
        }
    }

    override suspend fun removeAvatar(): Result<Unit, ApiError> =
        apiCall { httpClient.delete("$BASE_URL/api/member/avatar") }

    override suspend fun changePassword(request: ChangePasswordRequestDto): Result<Unit, ApiError> =
        apiCall { httpClient.post("$BASE_URL/api/auth/change-password") { setBody(request) } }

    override suspend fun requestPasswordReset(email: String): Result<Unit, ApiError> =
        apiCall {
            httpClient.post("$BASE_URL/api/auth/request-password-reset") {
                setBody(PasswordResetRequestDto(email = email, redirectTo = "$BASE_URL/reset-password"))
            }
        }
}

@kotlinx.serialization.Serializable
private data class SessionUserDto(val user: MemberDto? = null)
