package cobresun.movieclub.app.club.domain

data class ClubFeatures(
    val awards: Boolean = false,
    val discussionQuestions: Boolean = false,
)

data class InviteInfo(
    val clubId: String,
    val clubName: String,
    val slug: String,
)

enum class MemberRole { ADMIN, MEMBER }
