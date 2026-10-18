package cobresun.movieclub.app.stats.presentation

import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.reviews.domain.Review
import cobresun.movieclub.app.stats.domain.*

/**
 * Everything the statistics widgets show, computed once per data change.
 *
 * The widgets used to compute their own data in composition, inside the
 * LazyColumn's items, so scrolling a card back into view redid its work on the
 * main thread mid-scroll. Built off the main thread by [computeStatsModel].
 */
internal class StatsModel(
    val summary: StatsSummary,
    val wrappedYears: List<Int>,
    val records: ClubRecords?,
    val distribution: ScoreDistribution,
    val trend: Map<String, List<TimePoint>>,
    val spread: List<TimePoint>,
    val monthly: List<MonthCount>,
    val cumulative: List<TimePoint>,
    val yearlyBest: List<YearBest>,
    /** Movie clubs. */
    val genreStats: LovedCategories?,
    val genreWatchCounts: GenreWatchCounts?,
    val directors: List<PersonStat>,
    /** Book clubs. */
    val subjectStats: List<CategoryStat>,
    val subjectCounts: List<CategoryCount>,
    val authors: List<PersonStat>,
    val leaderboard: List<LeaderboardEntry>,
    val tasteClub: TasteSimilarity?,
    val tasteYours: TasteSimilarity?,
    val guiltyPleasures: List<MemberOutliers>,
    val curmudgeons: List<MemberOutliers>,
    val consensus: Consensus,
    val tmdbDeviation: ExternalDeviation?,
)

internal fun computeStatsModel(
    reviews: List<Review>,
    members: List<Member>,
    clubType: ClubType,
    currentUserId: String?,
): StatsModel {
    val movie = clubType == ClubType.MOVIE
    val pairs = members.size > 2
    return StatsModel(
        summary = computeSummary(reviews),
        wrappedYears = wrappedYears(reviews),
        records = computeClubRecords(reviews),
        distribution = computeScoreDistribution(reviews, members),
        trend = computeScoreTrend(reviews, members),
        spread = computeScoreSpreadTrend(reviews),
        monthly = computeMonthlyActivity(reviews),
        cumulative = computeCumulativeCounts(reviews),
        yearlyBest = computeHighestRatedByYear(reviews),
        genreStats = if (movie) computeGenreStats(reviews) else null,
        genreWatchCounts = if (movie) computeGenreWatchCounts(reviews) else null,
        directors = if (movie) computeTopDirectors(reviews) else emptyList(),
        subjectStats = if (!movie) computeSubjectStats(reviews) else emptyList(),
        subjectCounts = if (!movie) computeSubjectCounts(reviews) else emptyList(),
        authors = if (!movie) computeTopAuthors(reviews) else emptyList(),
        leaderboard = if (members.size > 1) computeLeaderboard(reviews, members) else emptyList(),
        tasteClub = if (pairs) computeTasteSimilarity(reviews, members) else null,
        tasteYours = if (pairs) currentUserId?.let { computeTasteSimilarity(reviews, members, it) } else null,
        guiltyPleasures = if (members.size > 1) computeGuiltyPleasures(reviews, members) else emptyList(),
        curmudgeons = if (members.size > 1) computeCurmudgeons(reviews, members) else emptyList(),
        consensus = computeConsensus(reviews, members),
        tmdbDeviation = if (movie) computeTmdbDeviation(reviews) else null,
    )
}
