package cobresun.movieclub.app.club.presentation

import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.member.presentation.components.label

/**
 * The one-line club summary shown under a club's name: "Movie club · 6 members"
 * (web `clubMetaLine`). Either half may still be loading, so the line degrades
 * to whichever half has arrived.
 */
fun clubMetaLine(type: ClubType?, memberCount: Int?): String {
    if (type == null) return ""
    val label = type.label()
    if (memberCount == null) return label
    return "$label · $memberCount ${if (memberCount == 1) "member" else "members"}"
}
