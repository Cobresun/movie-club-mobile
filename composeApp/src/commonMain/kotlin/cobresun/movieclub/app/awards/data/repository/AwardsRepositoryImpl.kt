package cobresun.movieclub.app.awards.data.repository

import cobresun.movieclub.app.core.domain.onError
import cobresun.movieclub.app.awards.data.dto.AddCategoryBodyDto
import cobresun.movieclub.app.awards.data.dto.CreateAwardsYearBodyDto
import cobresun.movieclub.app.awards.data.dto.NominationBodyDto
import cobresun.movieclub.app.awards.data.dto.RankingBodyDto
import cobresun.movieclub.app.awards.data.dto.ReorderCategoriesBodyDto
import cobresun.movieclub.app.awards.data.dto.StepBodyDto
import cobresun.movieclub.app.awards.data.mappers.toSeason
import cobresun.movieclub.app.awards.data.network.AwardsDataSource
import cobresun.movieclub.app.awards.domain.AwardsRepository
import cobresun.movieclub.app.awards.domain.AwardsSeason
import cobresun.movieclub.app.awards.domain.AwardsStep
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import cobresun.movieclub.app.core.domain.flatMap
import cobresun.movieclub.app.core.domain.map

class AwardsRepositoryImpl(
    private val dataSource: AwardsDataSource,
) : AwardsRepository {
    override suspend fun getYears(clubSlug: String): Result<List<Int>, DataError.Remote> =
        dataSource.getYears(clubSlug).map { years -> years.sortedDescending() }

    override suspend fun getAvailableYears(clubSlug: String): Result<List<Int>, DataError.Remote> =
        dataSource.getAvailableYears(clubSlug).map { years -> years.sortedDescending() }

    override suspend fun createYear(clubSlug: String, year: Int, categories: List<String>): Result<Unit, DataError.Remote> =
        dataSource.createYear(clubSlug, CreateAwardsYearBodyDto(year, categories))

    override suspend fun deleteYear(clubSlug: String, year: Int): Result<Unit, DataError.Remote> =
        dataSource.deleteYear(clubSlug, year)

    override suspend fun getSeason(clubSlug: String, year: Int): Result<AwardsSeason, DataError.Remote> =
        dataSource.getAwards(clubSlug, year).map { it.toSeason() }

    override suspend fun setStep(clubSlug: String, year: Int, step: AwardsStep): Result<Unit, DataError.Remote> =
        dataSource.putStep(clubSlug, year, StepBodyDto(step.value))

    override suspend fun addCategory(clubSlug: String, year: Int, title: String): Result<Unit, DataError.Remote> =
        dataSource.addCategory(clubSlug, year, AddCategoryBodyDto(title.trim()))

    override suspend fun reorderCategories(clubSlug: String, year: Int, titles: List<String>): Result<Unit, DataError.Remote> =
        dataSource.reorderCategories(clubSlug, year, ReorderCategoriesBodyDto(titles))

    override suspend fun renameCategory(
        clubSlug: String,
        year: Int,
        order: List<String>,
        from: String,
        to: String,
    ): Result<Unit, DataError.Remote> {
        val newTitle = to.trim()
        // Delete first: if the server refuses (its delete route can't match some
        // titles), nothing has changed, instead of leaving both titles behind.
        // Should the add then fail, put the old title back. The server only
        // reorders when sent every category exactly once, so the new one is
        // moved into the old one's slot last.
        return deleteCategory(clubSlug, year, from)
            .flatMap {
                addCategory(clubSlug, year, newTitle).onError { addCategory(clubSlug, year, from) }
            }
            .flatMap { reorderCategories(clubSlug, year, order.map { if (it == from) newTitle else it }) }
    }

    override suspend fun deleteCategory(clubSlug: String, year: Int, title: String): Result<Unit, DataError.Remote> =
        dataSource.deleteCategory(clubSlug, year, title)

    override suspend fun nominate(
        clubSlug: String,
        year: Int,
        category: String,
        movieId: Int,
        userId: String,
    ): Result<Unit, DataError.Remote> =
        dataSource.addNomination(clubSlug, year, NominationBodyDto(category, movieId, userId))

    override suspend fun withdrawNomination(
        clubSlug: String,
        year: Int,
        category: String,
        movieId: Int,
        userId: String,
    ): Result<Unit, DataError.Remote> =
        dataSource.deleteNomination(clubSlug, year, category, movieId, userId)

    override suspend fun submitRanking(
        clubSlug: String,
        year: Int,
        category: String,
        movieIds: List<Int>,
        userId: String,
    ): Result<Unit, DataError.Remote> =
        dataSource.submitRanking(clubSlug, year, RankingBodyDto(category, movieIds, userId))
}
