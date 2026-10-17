package cobresun.movieclub.app.reviews.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** A work on the reviews list with a score map keyed by user id plus a synthetic `"average"`. */
@Serializable
data class ReviewItemDto(
    val id: String,
    val type: String = "movie",
    val title: String,
    val createdDate: String,
    val imageUrl: String? = null,
    val externalId: String? = null,
    val scores: Map<String, ScoreDto> = emptyMap(),
    val externalData: JsonElement? = null,
)

@Serializable
data class ScoreDto(
    val id: String,
    val score: Double,
    @SerialName("created_date") val createdDate: String? = null,
)

@Serializable
data class ScoreCreateDto(val score: Double, val workId: String)

@Serializable
data class ScoreUpdateDto(val score: Double)

@Serializable
data class ListInsertBodyDto(
    val type: String,
    val title: String,
    val externalId: String,
    val imageUrl: String? = null,
)

@Serializable
data class ReviewsListIdDto(val id: String)

@Serializable
data class CommentDto(
    val id: String,
    val workId: String,
    val userId: String,
    val userName: String = "",
    val userImage: String? = null,
    val content: String,
    val createdDate: String,
    val spoiler: Boolean = false,
)

@Serializable
data class CommentBodyDto(val content: String, val spoiler: Boolean)

@Serializable
data class ClubSettingsDto(val features: ClubFeaturesDto = ClubFeaturesDto())

@Serializable
data class ClubFeaturesDto(val discussionQuestions: Boolean = false)

@Serializable
data class DiscussionQuestionsDto(val questions: List<String> = emptyList())

@Serializable
data class AddedDateBodyDto(val addedDate: String)

/** One work the signed-in member has scored, in any of their clubs (`GET /api/member/scores`). */
@Serializable
data class MemberScoredWorkDto(
    val workId: String,
    val clubId: String,
    val clubName: String = "",
    val clubSlug: String = "",
    val type: String = "movie",
    val title: String,
    val imageUrl: String? = null,
    val externalId: String? = null,
    val externalData: JsonElement? = null,
    val score: Double,
    val scoredDate: String = "",
)

/** `GET /api/club/:slug/reviews/:workId/shared` (public). */
@Serializable
data class SharedReviewDto(
    /** Review rows left-joined on the work: `user_id`/`score` are null when nobody scored. */
    val reviews: List<SharedReviewRowDto> = emptyList(),
    val members: List<SharedMemberDto> = emptyList(),
    val comments: List<CommentDto> = emptyList(),
    val work: SharedWorkDto,
    val clubName: String = "Movie Club",
)

@Serializable
data class SharedReviewRowDto(
    @SerialName("user_id") val userId: String? = null,
    /** Numeric, but tolerated as a string in case the driver serialises it that way. */
    val score: JsonElement? = null,
    @SerialName("created_date") val createdDate: String? = null,
)

@Serializable
data class SharedMemberDto(val id: String, val name: String = "", val image: String? = null)

@Serializable
data class SharedWorkDto(
    val id: String,
    val title: String,
    val type: String = "movie",
    val imageUrl: String? = null,
    val externalId: String? = null,
    val externalData: JsonElement? = null,
)
