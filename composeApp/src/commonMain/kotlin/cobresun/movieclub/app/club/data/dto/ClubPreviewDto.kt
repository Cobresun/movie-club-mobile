package cobresun.movieclub.app.club.data.dto

import kotlinx.serialization.Serializable

/** `GET /api/club/<slug>`: public, so shared pages can name a club the user isn't in. */
@Serializable
data class ClubPreviewDto(
    val clubId: String,
    val clubName: String,
    val slug: String,
    val type: String? = null,
)
