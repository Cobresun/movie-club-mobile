package cobresun.movieclub.app.core.domain

data class User(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val email: String
)
