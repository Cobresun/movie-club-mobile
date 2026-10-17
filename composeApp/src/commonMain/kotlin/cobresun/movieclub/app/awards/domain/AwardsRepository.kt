package cobresun.movieclub.app.awards.domain

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result

/**
 * A movie club's yearly awards: categories, nominations, member rankings and
 * results. All operations address the club by slug and a season by year.
 */
interface AwardsRepository {
    /** Years that have an awards season, newest first. */
    suspend fun getYears(clubSlug: String): Result<List<Int>, DataError.Remote>

    /** Years the club reviewed movies in that have no awards yet, newest first. */
    suspend fun getAvailableYears(clubSlug: String): Result<List<Int>, DataError.Remote>

    /** Opens [year]'s awards in the Categories phase, starting with [categories]. */
    suspend fun createYear(clubSlug: String, year: Int, categories: List<String>): Result<Unit, DataError.Remote>

    /** Deletes [year]'s awards: every category, nomination and vote. */
    suspend fun deleteYear(clubSlug: String, year: Int): Result<Unit, DataError.Remote>

    /** One season with nominee titles and posters. */
    suspend fun getSeason(clubSlug: String, year: Int): Result<AwardsSeason, DataError.Remote>

    suspend fun setStep(clubSlug: String, year: Int, step: AwardsStep): Result<Unit, DataError.Remote>

    suspend fun addCategory(clubSlug: String, year: Int, title: String): Result<Unit, DataError.Remote>

    /** Sets the category order. [titles] must contain exactly the existing titles (or renames, see [renameCategory]). */
    suspend fun reorderCategories(clubSlug: String, year: Int, titles: List<String>): Result<Unit, DataError.Remote>

    /**
     * Renames a category in place. The server has no rename, so this adds the
     * new title and reorders it into the old one's slot, which drops the old
     * category and any nominations it had.
     */
    suspend fun renameCategory(
        clubSlug: String,
        year: Int,
        order: List<String>,
        from: String,
        to: String,
    ): Result<Unit, DataError.Remote>

    suspend fun deleteCategory(clubSlug: String, year: Int, title: String): Result<Unit, DataError.Remote>

    /** Nominates a movie (TMDB id) in [category] on behalf of [userId]. */
    suspend fun nominate(clubSlug: String, year: Int, category: String, movieId: Int, userId: String): Result<Unit, DataError.Remote>

    /** Withdraws [userId]'s nomination; the nominee disappears once nobody nominates it. */
    suspend fun withdrawNomination(clubSlug: String, year: Int, category: String, movieId: Int, userId: String): Result<Unit, DataError.Remote>

    /** Saves [userId]'s ranking for [category]; [movieIds] best first. */
    suspend fun submitRanking(clubSlug: String, year: Int, category: String, movieIds: List<Int>, userId: String): Result<Unit, DataError.Remote>
}
