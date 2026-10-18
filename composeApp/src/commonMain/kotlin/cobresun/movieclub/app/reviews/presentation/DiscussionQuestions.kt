package cobresun.movieclub.app.reviews.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.material3.Surface
import androidx.compose.runtime.key
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

/**
 * Web `DiscussionQuestions`: a generate button until there are questions, then a
 * card whose numbered questions deal in one after another (70ms apart). A
 * regenerate dims the current set; if it fails, they stay with a note.
 */
@Composable
internal fun DiscussionQuestions(state: DiscussionQuestionsState, noun: String, onGenerate: () -> Unit) {
    val questions = state.questions
    val phase = when {
        !questions.isNullOrEmpty() -> "loaded"
        questions != null -> "unrecognized"
        state.failed -> "error"
        else -> "idle"
    }
    AnimatedContent(
        targetState = phase,
        transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
        modifier = Modifier.fillMaxWidth(),
        label = "discussion-questions",
    ) { p ->
        when (p) {
            "loaded" -> Column(
                Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(16.dp)
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(18.dp))
                    Text("Discussion questions", style = MaterialTheme.typography.titleSmall)
                }
                Column(
                    Modifier.graphicsLayer { alpha = if (state.isLoading) 0.5f else 1f },
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    questions.orEmpty().forEachIndexed { i, q ->
                        // Keyed by text so a regenerated set remounts and deals in again.
                        key("$i-$q") {
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .fadeUpOnEnter(delayMillis = i * 70)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.6f))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text("${i + 1}.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                Text(q, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
                if (state.failed) {
                    Text(
                        "Couldn't regenerate. Showing the previous questions.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onGenerate, enabled = !state.isLoading) {
                        if (state.isLoading) {
                            CircularProgressIndicator(Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(if (state.isLoading) "Regenerating…" else "Regenerate", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            "unrecognized" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "We couldn't recognize this $noun, so we couldn't generate discussion questions for it.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                GenerateQuestionsButton("Generate discussion questions", loading = false, enabled = false, onClick = onGenerate)
            }

            "error" -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Couldn't generate questions. Please try again.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                GenerateQuestionsButton("Try again", loading = state.isLoading, enabled = !state.isLoading, onClick = onGenerate)
            }

            else -> GenerateQuestionsButton(
                if (state.isLoading) "Generating questions…" else "Generate discussion questions",
                loading = state.isLoading,
                enabled = !state.isLoading,
                onClick = onGenerate,
            )
        }
    }
}

/** The AI button: a slowly turning gradient ring (web `ai-shimmer-button`), faster while loading. */
@Composable
private fun GenerateQuestionsButton(label: String, loading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "ai-shimmer")
    val angle by transition.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(if (loading) 1_200 else 4_000, easing = LinearEasing)),
        label = "ai-angle",
    )
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primary,
    )
    val shape = MaterialTheme.shapes.medium
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = if (enabled || loading) 1f else 0.5f }
            .clip(shape)
            .drawBehind {
                rotate(angle) {
                    drawCircle(Brush.sweepGradient(colors), radius = size.maxDimension)
                }
            }
            .padding(1.5.dp),
    ) {
        Row(
            Modifier.padding(vertical = 14.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
        }
    }
}
