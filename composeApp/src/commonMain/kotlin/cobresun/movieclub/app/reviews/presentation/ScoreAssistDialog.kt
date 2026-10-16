package cobresun.movieclub.app.reviews.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import cobresun.movieclub.app.core.domain.WorkType
import cobresun.movieclub.app.core.presentation.components.Poster
import cobresun.movieclub.app.reviews.domain.ComparisonAnswer
import cobresun.movieclub.app.reviews.domain.ScoreAssist

/**
 * Score Assist: "which did you like more?" comparisons against the user's own scored works
 * until a score is found. Nothing is saved; [onSuggest] pre-fills the score entry.
 */
@Composable
fun ScoreAssistDialog(
    target: ScoreAssist.Target,
    targetTitle: String,
    targetImageUrl: String?,
    pool: List<cobresun.movieclub.app.reviews.domain.ScoredWork>,
    onSuggest: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var session by remember(target) { mutableStateOf(ScoreAssist.start(target, pool)) }
    val result = session.result
    LaunchedEffect(result) {
        if (result != null) onSuggest(result.suggestedScore)
    }
    val noun = if (target.type == WorkType.BOOK) "book" else "movie"

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = MaterialTheme.shapes.extraLarge, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Column(
                Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    "Which $noun did you like more?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "Tap the one you liked more",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val pivot = session.pivot
                if (pivot != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Choice(targetTitle, targetImageUrl, Modifier.weight(1f)) {
                            session = ScoreAssist.answer(session, ComparisonAnswer.MORE)
                        }
                        AnimatedContent(
                            targetState = pivot,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            modifier = Modifier.weight(1f),
                        ) { p ->
                            Choice(p.title, p.imageUrl, Modifier.fillMaxWidth()) {
                                session = ScoreAssist.answer(session, ComparisonAnswer.LESS)
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { session = ScoreAssist.answer(session, ComparisonAnswer.SAME) }) {
                            Text("Too close to call")
                        }
                        TextButton(onClick = { session = ScoreAssist.answer(session, ComparisonAnswer.SKIP) }) {
                            Text("Skip this one", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(
                        "Comparison ${session.comparisons + 1} of up to ${ScoreAssist.MAX_COMPARISONS}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun Choice(title: String, imageUrl: String?, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(MaterialTheme.shapes.medium).clickable(onClickLabel = "I liked $title more", onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Poster(imageUrl, title, Modifier.fillMaxWidth().aspectRatio(2f / 3f), shape = MaterialTheme.shapes.medium)
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 2.dp, vertical = 2.dp),
        )
    }
}
