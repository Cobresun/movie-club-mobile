package cobresun.movieclub.app.member.data.network

import cobresun.movieclub.app.app.COBRESUN_CLUB_ID
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.member.data.dto.ClubDto

class MockMemberDataSource : MemberDataSource {
    override suspend fun getClubs(): Result<List<ClubDto>, DataError.Remote> {
        return Result.Success(
            listOf(
                COBRESUN_CLUB,
                JUNGAH_CLUB,
                VEDANT_CLUB,
                RAY_CLUB,
                SEAT_GEEKS_CLUB,
                MOVIE_POSTER_CLUB,
                HORROR_MOVIE_POST_CLUB,
                ORIANA_CLUB,
            )
        )
    }
}

val COBRESUN_CLUB = ClubDto(
    clubId = COBRESUN_CLUB_ID,
    clubName = "Cobresun",
)

val MOVIE_POSTER_CLUB = ClubDto(
    clubId = "946516463216623617",
    clubName = "Movie Poster Club",
)

val HORROR_MOVIE_POST_CLUB = ClubDto(
    clubId = "946516463218851841",
    clubName = "Horror Movie Poster",
)

val SEAT_GEEKS_CLUB = ClubDto(
    clubId = "957267395664969729",
    clubName = "Seat Geeks",
)

val ORIANA_CLUB = ClubDto(
    clubId ="962277841739808769",
    clubName = "OC’s Movie Club"
)

val JUNGAH_CLUB = ClubDto(
    clubId = "1038824677940559873",
    clubName = "4eva Evas \uD83D\uDC8B✨",
)

val VEDANT_CLUB = ClubDto(
    clubId = "960668027249262593",
    clubName = "Prickaxe",
)

val RAY_CLUB = ClubDto(
    clubId = "946516463244607489",
    clubName = "We're Watching Over Here",
)
