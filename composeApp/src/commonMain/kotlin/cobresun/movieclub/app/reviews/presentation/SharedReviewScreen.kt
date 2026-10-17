package cobresun.movieclub.app.reviews.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.WorkDetails
import cobresun.movieclub.app.core.presentation.components.Avatar
import cobresun.movieclub.app.core.presentation.components.ErrorState
import cobresun.movieclub.app.core.presentation.components.SectionLabel
import cobresun.movieclub.app.core.presentation.components.ShimmerBox
import cobresun.movieclub.app.core.presentation.components.WorkHero
import cobresun.movieclub.app.core.presentation.formatShort
import cobresun.movieclub.app.core.presentation.toUserMessage
import cobresun.movieclub.app.reviews.domain.ReviewFact
import cobresun.movieclub.app.reviews.domain.ScoreScale
import cobresun.movieclub.app.reviews.domain.SharedReview
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.round
import kotlin.time.Instant

@Composable
fun SharedReviewScreenRoot(onBack: () -> Unit, viewModel: SharedReviewViewModel = koinViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SharedReviewScreen(state, onBack = onBack, onRetry = viewModel::load)
}

/**
 * Web `SharedReviewView`: the club average and TMDB score, each member's score
 * with when they gave it, the spotlight fact and the comments, read-only.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedReviewScreen(state: SharedReviewState, onBack: () -> Unit, onRetry: () -> Unit) {
    val review = (state.review as? AsyncResult.Success)?.data
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(review?.clubName.orEmpty(), fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val r = state.review) {
                is AsyncResult.Loading -> Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    ShimmerBox(Modifier.fillMaxWidth().height(200.dp))
                    repeat(3) { ShimmerBox(Modifier.fillMaxWidth().height(64.dp)) }
                }
                is AsyncResult.Error -> ErrorState(
                    message = (r.dataError as? DataError.Remote)?.toUserMessage("load this review") ?: "Failed to load review",
                    onRetry = onRetry,
                )
                is AsyncResult.Success -> SharedReviewContent(r.data, state.fact)
            }
        }
    }
}

@Composable
private fun SharedReviewContent(review: SharedReview, fact: ReviewFact?) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        WorkHero(review.work, facts = review.work.reviewMetaLine()) {
            (review.work.details as? WorkDetails.Movie)?.tagline?.takeIf { it.isNotBlank() }?.let {
                Text("“$it”", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            RatingsBox(review)
            AnimatedVisibility(visible = fact != null, enter = fadeIn()) { fact?.let { ReviewFactCard(it) } }
            review.members.forEach { member ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Avatar(member.name, member.imageUrl, size = 48.dp)
                    Column(Modifier.weight(1f)) {
                        Text(member.name, style = MaterialTheme.typography.titleSmall)
                        member.scoredAt?.let { iso ->
                            runCatching { Instant.parse(iso).toLocalDateTime(TimeZone.currentSystemDefault()).date.formatShort() }
                                .getOrNull()?.let {
                                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                        }
                    }
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            member.score?.let(ScoreScale::format) ?: "-",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(" / 10", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (review.comments.isNotEmpty()) {
                SectionLabel("Reviews", Modifier.padding(top = 8.dp))
                CommentsReadOnly(review.comments)
            }
        }
    }
}

@Composable
private fun RatingsBox(review: SharedReview) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Filled.Star, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(32.dp))
            Text(oneDecimal(review.average ?: 0.0), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Club Average", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("(${review.scored.size} ratings)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        (review.work.details as? WorkDetails.Movie)?.voteAverage?.takeIf { it > 0 }?.let { vote ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.Movie, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(32.dp))
                Text(oneDecimal(vote), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text("TMDB Score", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun oneDecimal(value: Double): String {
    val r = round(value * 10) / 10
    return if (r == r.toLong().toDouble()) "${r.toLong()}.0" else r.toString()
}
