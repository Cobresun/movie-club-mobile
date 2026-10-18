package cobresun.movieclub.app.member.domain

import cobresun.movieclub.app.core.domain.Club
import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result

interface MemberRepository {
    /**
     * The signed-in member and their clubs. Screens opening together share one request
     * and a recent answer; [refresh] asks the server again (pull-to-refresh).
     */
    suspend fun getMember(refresh: Boolean = false): Result<Member, DataError.Remote>
    suspend fun getClubs(refresh: Boolean = false): Result<List<Club>, DataError.Remote>

    /**
     * The last answers kept on this device (null when there are none), without asking the
     * server: paint them first, then call [getMember] / [getClubs]. Offline, those return
     * the same kept answers instead of failing.
     */
    suspend fun savedMember(): Member? = null
    suspend fun savedClubs(): List<Club>? = null

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
