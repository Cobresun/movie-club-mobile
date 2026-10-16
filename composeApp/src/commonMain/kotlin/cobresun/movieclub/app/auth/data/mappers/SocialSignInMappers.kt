package cobresun.movieclub.app.auth.data.mappers

import cobresun.movieclub.app.auth.data.dto.SocialIdTokenDto
import cobresun.movieclub.app.auth.data.dto.SocialIdTokenNameDto
import cobresun.movieclub.app.auth.data.dto.SocialIdTokenUserDto
import cobresun.movieclub.app.auth.data.dto.SocialSignInRequestDto
import cobresun.movieclub.app.auth.domain.AppleCredential

fun AppleCredential.toSocialSignInRequest(): SocialSignInRequestDto {
    val first = givenName?.takeIf { it.isNotBlank() }
    val last = familyName?.takeIf { it.isNotBlank() }
    val user = if (first != null || last != null) {
        SocialIdTokenUserDto(name = SocialIdTokenNameDto(firstName = first, lastName = last))
    } else {
        null
    }
    return SocialSignInRequestDto(
        provider = "apple",
        idToken = SocialIdTokenDto(token = identityToken, nonce = rawNonce, user = user),
    )
}
