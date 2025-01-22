package cobresun.movieclub.app.auth.data.mappers

import cobresun.movieclub.app.auth.data.dto.UserDto
import cobresun.movieclub.app.auth.domain.User

fun UserDto.toUser(): User {
    return User(
        id = id,
        email = email,
        name = userMetadata.fullName,
        avatarUrl = userMetadata.avatarUrl
    )
}
