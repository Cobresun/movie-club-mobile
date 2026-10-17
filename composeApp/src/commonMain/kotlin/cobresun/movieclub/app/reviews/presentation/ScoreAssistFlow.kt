package cobresun.movieclub.app.reviews.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import cobresun.movieclub.app.core.domain.WorkType
import cobresun.movieclub.app.core.platform.HapticFeedback
import cobresun.movieclub.app.core.presentation.components.Poster
import cobresun.movieclub.app.reviews.domain.ComparisonAnswer
import cobresun.movieclub.app.reviews.domain.ScoreAssist
import org.koin.compose.koinInject

/**
 * Score Assist, content only (port of web `ScoreAssistFlow.vue`): "which did you
 * like more?" comparisons against the user's own scored works until a score is
 * found. Hosts swap it into the surface that's already open instead of stacking
 * another overlay. Nothing is saved; [onSuggest] pre-fills the host's dial.
 */
@Composable
fun ScoreAssistFlow(
    input: ScoreAssistInput,
    /** The club being reviewed in; its own works need no "from …" label. */
    currentClubSlug: String?,
    onSuggest: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = koinInject<HapticFeedback>()
    var session by remember(input.target) { mutableStateOf(ScoreAssist.start(input.target, input.pool)) }
    val noun = if (input.target.type == WorkType.BOOK) "book" else "movie"

    fun answer(choice: ComparisonAnswer) {
        haptics.performTick()
        val next = ScoreAssist.answer(session, choice)
        session = next
        next.result?.let {
            haptics.performSuccess()
            onSuggest(it.suggestedScore)
        }
    }

    val pivot = session.pivot ?: return
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.widthIn(max = 340.dp)) {
            Choice(input.title, input.imageUrl, null, Modifier.weight(1f)) { answer(ComparisonAnswer.MORE) }
            AnimatedContent(
                targetState = pivot,
                transitionSpec = {
                    (fadeIn(tween(150)) + slideInHorizontally(tween(150)) { it / 20 }) togetherWith
                        (fadeOut(tween(150)) + slideOutHorizontally(tween(150)) { -it / 20 })
                },
                contentKey = { it.workId },
                modifier = Modifier.weight(1f),
                label = "assist-pivot",
            ) { p ->
                // The pool spans every club the user belongs to, so a pivot scored
                // elsewhere says where it came from.
                val from = p.clubName.takeIf { currentClubSlug != null && p.clubSlug != currentClubSlug && it.isNotBlank() }
                Choice(p.title, p.imageUrl, from?.let { "from $it" }, Modifier.fillMaxWidth()) { answer(ComparisonAnswer.LESS) }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = { answer(ComparisonAnswer.SAME) }) { Text("Too close to call") }
            TextButton(onClick = { answer(ComparisonAnswer.SKIP) }) {
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

@Composable
private fun Choice(title: String, imageUrl: String?, caption: String?, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(MaterialTheme.shapes.medium).clickable(onClickLabel = "I liked $title more", onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Poster(imageUrl, title, Modifier.fillMaxWidth().aspectRatio(2f / 3f), shape = MaterialTheme.shapes.medium)
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 2.dp),
        )
        caption?.let {
            Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        }
    }
}
