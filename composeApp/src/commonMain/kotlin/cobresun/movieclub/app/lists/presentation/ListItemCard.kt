package cobresun.movieclub.app.lists.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.presentation.components.Avatar
import cobresun.movieclub.app.core.presentation.components.Poster
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.reviews.presentation.EmphasizedEasing

/**
 * A list's poster card (web `WorkPosterCard` in `ListItems`): the poster, its
 * title and who added it, with an "Up next" pin on the club's next work.
 * [pulse] changes to make the card glow once (web `card-pulse`), so the eye
 * finds a card that was just pinned or dropped.
 */
@Composable
fun ListItemCard(
    work: Work,
    addedBy: Member?,
    isUpNext: Boolean,
    pulse: Int?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    /** Still being added: dimmed under a spinner and not tappable (web `loading`). */
    pending: Boolean = false,
    lifted: Boolean = false,
) {
    val progress = remember { Animatable(1f) }
    LaunchedEffect(pulse) {
        if (pulse == null) return@LaunchedEffect
        progress.snapTo(0f)
        progress.animateTo(1f, tween(PULSE_MILLIS, easing = EmphasizedEasing))
    }
    // Web's gold highlightBackground, which the app keeps in tertiary.
    val ring = MaterialTheme.colorScheme.tertiary
    val shape = MaterialTheme.shapes.small
    Column(
        modifier = modifier
            .graphicsLayer {
                val glow = pulseCurve(progress.value)
                val scale = (1f + 0.05f * glow) * (if (lifted) 1.06f else 1f)
                scaleX = scale
                scaleY = scale
            }
            .then(if (lifted) Modifier else Modifier.clip(shape))
            .clickable(enabled = !pending, onClick = onClick),
    ) {
        Box(
            // Only the poster casts the lifted shadow: on the whole card it showed as a
            // dark box behind the transparent title area. (The card drops its clip while
            // lifted so the shadow isn't cut off.)
            Modifier.then(if (lifted) Modifier.shadow(16.dp, shape, clip = false) else Modifier).drawBehind {
                val glow = pulseCurve(progress.value)
                if (glow <= 0f) return@drawBehind
                // A gold ring spreading out from the poster's edge, like web's box-shadow.
                val spread = 6.dp.toPx() * glow
                drawRoundRect(
                    color = ring.copy(alpha = 0.45f * glow),
                    topLeft = Offset(-spread / 2, -spread / 2),
                    size = Size(size.width + spread, size.height + spread),
                    cornerRadius = CornerRadius(8.dp.toPx() + spread / 2),
                    style = Stroke(width = spread),
                )
            }
        ) {
            Poster(
                imageUrl = work.imageUrl,
                title = work.title,
                modifier = Modifier.fillMaxWidth().then(if (isUpNext) Modifier.border(3.dp, ring, shape) else Modifier),
            )
            if (isUpNext) UpNextBadge(Modifier.align(Alignment.TopStart).padding(6.dp))
            if (pending) {
                Box(
                    Modifier.matchParentSize().clip(shape).background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            work.title,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (addedBy != null) {
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Avatar(name = addedBy.name, imageUrl = addedBy.imageUrl, size = 18.dp)
                Text(
                    addedBy.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Web's gold "Up next" pill pinned to the poster's corner. */
@Composable
fun UpNextBadge(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.tertiary)
            .padding(start = 6.dp, end = 8.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            Icons.Default.PushPin,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onTertiary,
            modifier = Modifier.size(12.dp),
        )
        Text(
            "Up next",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiary,
        )
    }
}

private const val PULSE_MILLIS = 700

/** 0 → 1 over the first 30% of the pulse, then back to 0 (web keyframes). */
private fun pulseCurve(t: Float): Float = when {
    t >= 1f -> 0f
    t < 0.3f -> t / 0.3f
    else -> (1f - t) / 0.7f
}
