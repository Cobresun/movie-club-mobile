package cobresun.movieclub.app.auth.data.mappers

import cobresun.movieclub.app.auth.data.dto.TokenDto
import cobresun.movieclub.app.auth.data.dto.UserDto
import cobresun.movieclub.app.auth.domain.Token
import cobresun.movieclub.app.auth.domain.User

fun UserDto.toUser(tokenDto: TokenDto): User {
    return User(
        id = id,
        email = email,
        name = userMetadata.fullName,
        avatarUrl = userMetadata.avatarUrl,
        token = tokenDto.toToken()
    )
}

fun TokenDto.toToken(): Token {
    return Token(
        accessToken = accessToken,
        tokenType = tokenType,
        expiresIn = expiresIn,
        refreshToken = refreshToken
    )
}
