package cobresun.movieclub.app.stats.presentation

import cobresun.movieclub.app.stats.domain.computeTopActors
import cobresun.movieclub.app.stats.domain.PersonStat
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.unit.Dp
import cobresun.movieclub.app.core.presentation.components.LocalTabBarInset
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.ui.Alignment
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material.icons.outlined.Share
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
import cobresun.movieclub.app.stats.domain.wrappedYears

/** Below this many scored works the page shows a "getting started" hint. */
private const val FEW_REVIEWS = 5

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    state: StatsState,
    onAction: (StatsAction) -> Unit,
    onGoToReviews: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    onOpenWrapped: (year: Int) -> Unit = {},
) {
    Column(modifier.fillMaxSize()) {
        // Share sits on the title row: the eyebrow row is shared with the shell's account avatar.
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 12.dp, bottom = 8.dp)) {
            ClubNameLabel(state.clubName)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Statistics", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
                IconButton(onClick = onShare) {
                    Icon(Icons.Outlined.Share, contentDescription = "Share statistics")
                }
            }
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
                StatsContent(state, data.data, onGoToReviews, onOpenWrapped)
            }
        }
    }
}

/**
 * The statistics widgets (web `InsightsView`). Shared pages pass null callbacks:
 * no Wrapped entry and no "Go to Reviews" for a club the user isn't in.
 */
@Composable
internal fun StatsContent(
    state: StatsState,
    data: StatsData,
    onGoToReviews: (() -> Unit)?,
    onOpenWrapped: ((Int) -> Unit)?,
    bottomInset: Dp = LocalTabBarInset.current,
) {
    val reviews = data.reviews
    val members = data.members
    val type = state.clubType
    val noun = if (type == ClubType.MOVIE) "movie" else "book"
    if (reviews.isEmpty()) {
        // Scrollable so pull-to-refresh still works on the empty state.
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            if (onGoToReviews != null) {
                EmptyState(
                    icon = Icons.Outlined.Insights,
                    title = "No statistics yet",
                    message = "Statistics appear once your club has scored a $noun. Score your first one to get started.",
                    actionLabel = "Go to Reviews",
                    onAction = onGoToReviews,
                )
            } else {
                EmptyState(
                    icon = Icons.Outlined.Insights,
                    title = "No statistics yet",
                    message = "Statistics will appear once this club has reviewed some ${noun}s.",
                )
            }
        }
        return
    }
    // Computed once per data change, off the main thread; the cards only render it.
    // Until the first model lands, show the skeleton.
    val currentUserId = state.currentUserId
    val model by produceState<StatsModel?>(null, reviews, members, type, currentUserId) {
        value = withContext(Dispatchers.Default) { computeStatsModel(reviews, members, type, currentUserId) }
    }
    val actors by produceState(emptyList<PersonStat>(), reviews, state.cast) {
        val cast = state.cast
        value = if (cast == null) emptyList() else withContext(Dispatchers.Default) { computeTopActors(reviews, cast) }
    }
    val m = model ?: run {
        StatsSkeleton()
        return
    }
    val summary = m.summary
    val wrappedYears = m.wrappedYears
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp + bottomInset),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (onOpenWrapped != null && wrappedYears.isNotEmpty()) item("wrapped") {
            WrappedEntryCard(wrappedYears, onOpenWrapped, Modifier.padding(bottom = 14.dp))
        }
        if (reviews.size < FEW_REVIEWS) item("hint") { Box(Modifier.padding(bottom = 14.dp)) { FewReviewsHint(reviews.size, type) } }
        item("summary") { Box(Modifier.padding(bottom = 14.dp)) { SummaryTiles(summary, type) } }
        item("records") { ClubRecordsCard(m) }
        item("scores") { ScoresCard(m, members) }
        item("activity") { ActivityCard(m, type) }
        item("categories") { GenresCard(m, reviews, members, type) }
        item("people") { PeopleCard(m, type, actors) }
        if (members.size > 1) item("reviewers") { ReviewerLeaderboardCard(m, members) }
        if (members.size > 2) item("taste") { TasteSimilarityCard(m, members, currentUserId) }
        if (members.size > 1) item("outliers") { MemberOutliersCard(m, members, type) }
        item("consensus") { ConsensusCard(m, members) }
        if (type == ClubType.MOVIE) item("tmdb") { TmdbDeviationCard(m) }
    }
}

@Composable
internal fun StatsSkeleton() {
    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        ShimmerBox(Modifier.fillMaxWidth().height(96.dp), MaterialTheme.shapes.large)
        ShimmerBox(Modifier.fillMaxWidth().height(220.dp), MaterialTheme.shapes.large)
        ShimmerBox(Modifier.fillMaxWidth().height(260.dp), MaterialTheme.shapes.large)
    }
}
