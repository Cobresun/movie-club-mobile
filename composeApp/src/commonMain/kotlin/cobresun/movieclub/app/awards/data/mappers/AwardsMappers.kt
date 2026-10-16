package cobresun.movieclub.app.awards.data.mappers

import cobresun.movieclub.app.awards.data.dto.AwardDto
import cobresun.movieclub.app.awards.data.dto.ClubAwardsDto
import cobresun.movieclub.app.awards.data.dto.NominationDto
import cobresun.movieclub.app.awards.domain.AwardCategory
import cobresun.movieclub.app.awards.domain.AwardsSeason
import cobresun.movieclub.app.awards.domain.AwardsStep
import cobresun.movieclub.app.awards.domain.Nomination

fun ClubAwardsDto.toSeason(): AwardsSeason = AwardsSeason(
    year = year,
    step = AwardsStep.from(step),
    categories = awards.map { it.toCategory() },
)

fun AwardDto.toCategory(): AwardCategory = AwardCategory(
    title = title,
    nominations = nominations.map { it.toNomination() },
)

fun NominationDto.toNomination(): Nomination = Nomination(
    movieId = movieId,
    title = movieTitle?.takeIf { it.isNotBlank() } ?: "Movie #$movieId",
    // The server builds "<base>w154<poster_path>"; a missing poster_path yields ".../w154null".
    posterUrl = posterUrl?.takeUnless { it.isBlank() || it.endsWith("null") }
        ?.replace("/w154/", "/w342/"),
    releaseYear = movieData?.releaseDate?.take(4)?.toIntOrNull(),
    nominatedBy = nominatedBy,
    ranking = ranking,
)
