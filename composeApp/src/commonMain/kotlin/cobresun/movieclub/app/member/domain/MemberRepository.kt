package cobresun.movieclub.app.member.domain

import cobresun.movieclub.app.core.domain.Club
import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result

interface MemberRepository {
    suspend fun getMember(): Result<Member, DataError.Remote>
    suspend fun getClubs(): Result<List<Club>, DataError.Remote>

    /**
     * Creates a new club; [memberEmail] (the current user) becomes its admin.
     * @return Result containing the new club slug on success
     */
    suspend fun createClub(
        name: String,
        memberEmail: String,
        type: ClubType = ClubType.MOVIE,
    ): Result<String, DataError.Remote>
}
