package cobresun.movieclub.app.stats.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.presentation.components.ClubNameLabel
import cobresun.movieclub.app.core.presentation.components.EmptyState
import cobresun.movieclub.app.core.presentation.components.ErrorState
import cobresun.movieclub.app.core.presentation.components.ShimmerBox
import cobresun.movieclub.app.core.presentation.toUserMessage
import cobresun.movieclub.app.stats.domain.computeSummary

/** Below this many scored works the page shows a "getting started" hint. */
private const val FEW_REVIEWS = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    state: StatsState,
    onAction: (StatsAction) -> Unit,
    onGoToReviews: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize()) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp)) {
            ClubNameLabel(state.clubName)
            Text("Statistics", style = MaterialTheme.typography.headlineMedium)
        }
        when (val data = state.data) {
            is AsyncResult.Loading -> StatsSkeleton()
            is AsyncResult.Error -> ErrorState(
                message = (data.dataError as? DataError.Remote)?.toUserMessage("load statistics") ?: "Couldn't load statistics.",
                onRetry = { onAction(StatsAction.Refresh) },
            )
            is AsyncResult.Success -> PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = { onAction(StatsAction.Refresh) },
                modifier = Modifier.fillMaxSize(),
            ) {
                StatsContent(state, data.data, onGoToReviews)
            }
        }
    }
}

@Composable
private fun StatsContent(state: StatsState, data: StatsData, onGoToReviews: () -> Unit) {
    val reviews = data.reviews
    val members = data.members
    val type = state.clubType
    val noun = if (type == ClubType.MOVIE) "movie" else "book"
    if (reviews.isEmpty()) {
        // Scrollable so pull-to-refresh still works on the empty state.
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            EmptyState(
                icon = Icons.Outlined.Insights,
                title = "No statistics yet",
                message = "Statistics appear once your club has scored a $noun. Score your first one to get started.",
                actionLabel = "Go to Reviews",
                onAction = onGoToReviews,
            )
        }
        return
    }
    val summary = remember(reviews) { computeSummary(reviews) }
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (reviews.size < FEW_REVIEWS) item("hint") { Box(Modifier.padding(bottom = 14.dp)) { FewReviewsHint(reviews.size, type) } }
        item("summary") { Box(Modifier.padding(bottom = 14.dp)) { SummaryTiles(summary, type) } }
        item("records") { ClubRecordsCard(reviews) }
        item("scores") { ScoresCard(reviews, members) }
        item("activity") { ActivityCard(reviews, type) }
        item("categories") { GenresCard(reviews, members, type) }
        item("people") { PeopleCard(reviews, type, state.cast) }
        if (members.size > 1) item("reviewers") { ReviewerLeaderboardCard(reviews, members) }
        if (members.size > 2) item("taste") { TasteSimilarityCard(reviews, members, state.currentUserId) }
        if (members.size > 1) item("outliers") { MemberOutliersCard(reviews, members, type) }
        item("consensus") { ConsensusCard(reviews, members) }
        if (type == ClubType.MOVIE) item("tmdb") { TmdbDeviationCard(reviews) }
    }
}

@Composable
private fun StatsSkeleton() {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ShimmerBox(Modifier.fillMaxWidth().height(96.dp), MaterialTheme.shapes.large)
        ShimmerBox(Modifier.fillMaxWidth().height(220.dp), MaterialTheme.shapes.large)
        ShimmerBox(Modifier.fillMaxWidth().height(260.dp), MaterialTheme.shapes.large)
    }
}
