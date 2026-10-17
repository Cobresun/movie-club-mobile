package cobresun.movieclub.app.reviews.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.presentation.components.PlatformNumericTextField
import cobresun.movieclub.app.core.presentation.components.SectionLabel
import cobresun.movieclub.app.reviews.domain.ScoreScale
import cobresun.movieclub.app.reviews.domain.ScoreAssist
import cobresun.movieclub.app.reviews.domain.ScoredWork
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material3.Icon

/** What Score Assist needs to compare the work being scored against the user's other scores. */
data class ScoreAssistInput(
    val target: ScoreAssist.Target,
    val title: String,
    val imageUrl: String?,
    val pool: List<ScoredWork>,
)

/**
 * Your score for a work: a 0–10 slider in half steps for quick entry, plus a
 * text field for exact values (up to two decimals), matching the web panel.
 */
@Composable
fun ScoreEntryPanel(
    workId: String,
    existing: Double?,
    isSaving: Boolean,
    enabled: Boolean,
    onSave: (Double) -> Unit,
    onDelete: () -> Unit,
    assist: ScoreAssistInput? = null,
) {
    var text by remember(workId, existing) { mutableStateOf(existing?.let(ScoreScale::format) ?: "") }
    var showAssist by remember(workId) { mutableStateOf(false) }
    var suggested by remember(workId, existing) { mutableStateOf<Double?>(null) }
    val parsed = text.toDoubleOrNull()
    val valid = ScoreScale.isValid(parsed)
    val dirty = valid && parsed != existing
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    Column(
        Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                SectionLabel("Your score")
                Text(
                    when {
                        suggested != null && parsed == suggested -> "Suggested by Score Assist. Save it or adjust first."
                        existing == null -> "Slide or type a score from 0 to 10"
                        dirty -> "Tap Update to save the change"
                        else -> "Slide or type to change it"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PlatformNumericTextField(
                value = text,
                onValueChange = { text = ScoreScale.sanitizeInput(it) },
                modifier = Modifier
                    .width(84.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                textStyle = MaterialTheme.typography.headlineMedium.copy(
                    color = if (existing == null && text.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.secondary,
                    textAlign = TextAlign.Center,
                ),
                singleLine = true,
                onDone = {
                    focusManager.clearFocus()
                    if (dirty && parsed != null) onSave(parsed)
                },
                focusRequester = focusRequester,
            )
        }
        Slider(
            value = (parsed ?: existing ?: 5.0).toFloat().coerceIn(0f, 10f),
            onValueChange = { text = ScoreScale.format(ScoreScale.snap(it.toDouble())) },
            valueRange = ScoreScale.MIN.toFloat()..ScoreScale.MAX.toFloat(),
            steps = ((ScoreScale.MAX - ScoreScale.MIN) / ScoreScale.STEP).toInt() - 1,
            enabled = enabled && !isSaving,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.secondary,
                activeTrackColor = MaterialTheme.colorScheme.secondary,
                activeTickColor = MaterialTheme.colorScheme.onSecondary.copy(alpha = 0.4f),
                inactiveTickColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
            ),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (assist != null && ScoreAssist.isEligible(assist.pool) && enabled) {
                TextButton(onClick = { showAssist = true }, enabled = !isSaving) {
                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Help me score")
                }
            }
            if (existing != null) {
                TextButton(onClick = onDelete, enabled = !isSaving) {
                    Text("Clear my score", color = MaterialTheme.colorScheme.error)
                }
            }
            Spacer(Modifier.weight(1f))
            AnimatedVisibility(visible = dirty || isSaving) {
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        parsed?.let(onSave)
                    },
                    enabled = enabled && dirty && !isSaving,
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (existing == null) "Save" else "Update", maxLines = 1)
                }
            }
        }
    }
    if (showAssist && assist != null) {
        ScoreAssistDialog(
            target = assist.target,
            targetTitle = assist.title,
            targetImageUrl = assist.imageUrl,
            pool = assist.pool,
            onSuggest = { score ->
                showAssist = false
                suggested = score
                text = ScoreScale.format(score)
            },
            onDismiss = { showAssist = false },
        )
    }
}
