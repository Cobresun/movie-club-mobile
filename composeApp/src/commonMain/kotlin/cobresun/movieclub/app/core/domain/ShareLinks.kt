package cobresun.movieclub.app.core.domain

import cobresun.movieclub.app.core.domain.Constants.BASE_URL

/** Public, sign-in-free web pages for sharing club content (web router `/share/...`). */
object ShareLinks {
    fun review(clubSlug: String, workId: String) = "$BASE_URL/share/club/$clubSlug/review/$workId"
    fun list(clubSlug: String, listId: String) = "$BASE_URL/share/club/$clubSlug/list/$listId"
    fun statistics(clubSlug: String) = "$BASE_URL/share/club/$clubSlug/statistics"
}
