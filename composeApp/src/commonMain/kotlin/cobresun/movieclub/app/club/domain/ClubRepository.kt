package cobresun.movieclub.app.club.domain

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.member.domain.Member

interface ClubRepository {
    /**
     * Gets members for a club from the network.
     * Returns error if network request fails.
     */
    suspend fun getMembers(clubSlug: String): Result<List<Member>, DataError.Remote>

    /**
     * Generates an invite token for the club.
     * Token should be appended to base URL as /join-club/{token}
     */
    suspend fun generateInviteToken(clubSlug: String): Result<String, DataError.Remote>

    /**
     * Removes the current user from the club.
     */
    suspend fun leaveClub(clubSlug: String): Result<Unit, DataError.Remote>
}
