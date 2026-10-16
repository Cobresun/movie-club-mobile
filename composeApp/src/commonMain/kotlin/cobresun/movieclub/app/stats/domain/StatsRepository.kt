package cobresun.movieclub.app.stats.domain

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result

/**
 * Statistics-only data the reviews feed doesn't carry. Reviews and members
 * come from `ReviewsRepository` / `ClubRepository`.
 */
interface StatsRepository {
    /**
     * Billed cast of every reviewed movie, keyed by external (TMDB) id. Heavy,
     * so only fetched while the statistics screen is open.
     */
    suspend fun getReviewedCast(clubSlug: String): Result<Map<String, List<CreditedPerson>>, DataError.Remote>
}
