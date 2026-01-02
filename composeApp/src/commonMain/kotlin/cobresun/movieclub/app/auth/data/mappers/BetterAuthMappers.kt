package cobresun.movieclub.app.auth.data.mappers

import cobresun.movieclub.app.auth.data.dto.BetterAuthUserDto
import cobresun.movieclub.app.auth.domain.User

fun BetterAuthUserDto.toUser(): User {
    return User(
        id = id,
        email = email,
        name = name,
        avatarUrl = image
    )
}
