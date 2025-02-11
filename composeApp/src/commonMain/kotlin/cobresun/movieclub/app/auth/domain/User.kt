package cobresun.movieclub.app.auth.domain

data class User(
    val id: String,
    val email: String,
    val name: String,
    val avatarUrl: String?,
)
