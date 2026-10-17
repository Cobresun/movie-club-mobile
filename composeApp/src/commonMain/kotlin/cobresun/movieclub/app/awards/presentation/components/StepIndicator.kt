package cobresun.movieclub.app.awards.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.awards.domain.AwardsStep
import cobresun.movieclub.app.awards.presentation.phase

/** Categories → Nominations → Voting → Ceremony, with the current phase highlighted. */
@Composable
fun StepIndicator(step: AwardsStep, modifier: Modifier = Modifier) {
    val steps = AwardsStep.visible
    val current = if (step == AwardsStep.COMPLETED) steps.size else steps.indexOf(step)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clearAndSetSemantics {
                contentDescription = if (step == AwardsStep.COMPLETED) "Awards complete"
                else "Step ${current + 1} of ${steps.size}: ${step.phase.label}"
            },
        verticalAlignment = Alignment.Top,
    ) {
        steps.forEachIndexed { index, s ->
            val done = index < current
            val active = index == current
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Connector(visible = index > 0, lit = index <= current, modifier = Modifier.weight(1f))
                    StepDot(number = index + 1, done = done, active = active)
                    Connector(visible = index < steps.lastIndex, lit = index < current, modifier = Modifier.weight(1f))
                }
                Text(
                    s.phase.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = when {
                        active -> MaterialTheme.colorScheme.secondary
                        done -> MaterialTheme.colorScheme.onSurface
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
                    modifier = Modifier.padding(top = 6.dp),
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun StepDot(number: Int, done: Boolean, active: Boolean) {
    val fill by animateColorAsState(
        when {
            done -> MaterialTheme.colorScheme.secondary
            active -> MaterialTheme.colorScheme.secondaryContainer
            else -> MaterialTheme.colorScheme.surfaceContainerHigh
        }
    )
    val ring = if (active) MaterialTheme.colorScheme.secondary else fill
    Box(
        Modifier.size(26.dp).background(fill, CircleShape).border(2.dp, ring, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (done) {
            Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.onSecondary, modifier = Modifier.size(16.dp))
        } else {
            Text(
                "$number",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = if (active) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Connector(visible: Boolean, lit: Boolean, modifier: Modifier) {
    Box(
        modifier
            .height(2.dp)
            .background(
                when {
                    !visible -> MaterialTheme.colorScheme.background.copy(alpha = 0f)
                    lit -> MaterialTheme.colorScheme.secondary
                    else -> MaterialTheme.colorScheme.surfaceContainerHigh
                }
            )
    )
}

/** Small rounded label, e.g. "1/2" or "Submitted". */
@Composable
fun Pill(text: String, modifier: Modifier = Modifier, emphasized: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = if (emphasized) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .background(
                if (emphasized) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.surfaceContainerHighest,
                CircleShape,
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Composable
fun SectionCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLow, MaterialTheme.shapes.large)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) { content() }
}
