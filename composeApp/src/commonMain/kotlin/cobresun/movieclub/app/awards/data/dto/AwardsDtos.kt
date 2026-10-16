package cobresun.movieclub.app.awards.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ClubAwardsDto(
    val year: Int,
    val step: Int,
    val awards: List<AwardDto> = emptyList(),
)

@Serializable
data class AwardDto(
    val title: String,
    val nominations: List<NominationDto> = emptyList(),
)

@Serializable
data class NominationDto(
    val movieId: Int,
    val nominatedBy: List<String> = emptyList(),
    val ranking: Map<String, Int> = emptyMap(),
    val movieTitle: String? = null,
    val posterUrl: String? = null,
    val movieData: NominationMovieDataDto? = null,
)

@Serializable
data class NominationMovieDataDto(
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
)

@Serializable
data class StepBodyDto(val step: Int)

@Serializable
data class AddCategoryBodyDto(val title: String)

@Serializable
data class ReorderCategoriesBodyDto(val categories: List<String>)

@Serializable
data class NominationBodyDto(val awardTitle: String, val movieId: Int, val nominatedBy: String)

@Serializable
data class RankingBodyDto(val awardTitle: String, val movies: List<Int>, val voter: String)
