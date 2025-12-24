package cobresun.movieclub.app.member.domain

import cobresun.movieclub.app.core.domain.Club
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result

interface MemberRepository {
    suspend fun getMember(): Result<Member, DataError.Remote>
    suspend fun getClubs(): Result<List<Club>, DataError.Remote>

    /**
     * Creates a new club with the given name and initial member.
     * @param name The name of the club
     * @param memberEmail Email of the initial member (current user)
     * @return Result containing the new club ID on success
     */
    suspend fun createClub(name: String, memberEmail: String): Result<String, DataError.Remote>
}
