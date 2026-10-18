package cobresun.movieclub.app.club.domain

import cobresun.movieclub.app.core.domain.ApiError
import cobresun.movieclub.app.core.domain.Club
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.member.domain.Member

interface ClubRepository {
    /**
     * The club's members. Like [getFeatures], served from a recent answer shared by the
     * club's screens unless [refresh] (pull-to-refresh); this repository's writes renew it.
     */
    suspend fun getMembers(clubSlug: String, refresh: Boolean = false): Result<List<Member>, DataError.Remote>

    /** The members last kept on this device, without a request (null when none); see [getMembers]. */
    suspend fun savedMembers(clubSlug: String): List<Member>? = null

    /** The club's public name and type, also for clubs the user isn't in (shared pages). */
    suspend fun getClubPreview(clubSlug: String): Result<Club, DataError.Remote>

    /** Generates an invite token; see [ClubValidation.inviteLink]. */
    suspend fun generateInviteToken(clubSlug: String): Result<String, DataError.Remote>

    /** Removes the current user from the club. */
    suspend fun leaveClub(clubSlug: String): Result<Unit, DataError.Remote>

    suspend fun removeMember(clubSlug: String, memberId: String): Result<Unit, DataError.Remote>

    suspend fun updateName(clubSlug: String, name: String): Result<Unit, ApiError>

    /** Returns the new slug on success; server message on invalid/taken slugs. */
    suspend fun updateSlug(clubSlug: String, newSlug: String): Result<String, ApiError>

    suspend fun getFeatures(clubSlug: String, refresh: Boolean = false): Result<ClubFeatures, DataError.Remote>

    suspend fun updateFeatures(
        clubSlug: String,
        awards: Boolean? = null,
        discussionQuestions: Boolean? = null,
    ): Result<ClubFeatures, DataError.Remote>

    suspend fun getInviteInfo(token: String): Result<InviteInfo, ApiError>

    suspend fun joinClub(token: String): Result<Unit, ApiError>
}
