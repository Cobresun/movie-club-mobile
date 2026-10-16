package cobresun.movieclub.app.core.presentation.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage

const val POSTER_ASPECT = 2f / 3f

/**
 * A 2:3 poster/cover with rounded corners. Shows a shimmer while loading and a
 * titled placeholder when there is no image or it fails to load.
 */
@Composable
fun Poster(
    imageUrl: String?,
    title: String,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small,
    highlighted: Boolean = false,
) {
    val highlightColor = MaterialTheme.colorScheme.secondary
    Box(
        modifier = modifier
            .aspectRatio(POSTER_ASPECT)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        if (imageUrl.isNullOrBlank()) {
            PosterPlaceholder(title)
        } else {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = { ShimmerBox(Modifier.fillMaxSize()) },
                error = { PosterPlaceholder(title) },
            )
        }
        if (highlighted) {
            Box(
                Modifier
                    .fillMaxSize()
                    .border(2.dp, highlightColor, shape)
            )
        }
    }
}

@Composable
private fun PosterPlaceholder(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        MaterialTheme.colorScheme.surfaceContainerLow,
                    )
                )
            )
            .padding(10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Movie,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
            modifier = Modifier.size(40.dp).align(Alignment.TopCenter).padding(top = 8.dp),
        )
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** A softly pulsing placeholder block used for loading skeletons. */
@Composable
fun ShimmerBox(modifier: Modifier = Modifier, shape: Shape = MaterialTheme.shapes.small) {
    val transition = rememberInfiniteTransition()
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
    )
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha))
    )
}

/** Circular member avatar with an initial fallback. */
@Composable
fun Avatar(
    name: String,
    imageUrl: String?,
    size: Dp = 32.dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.trim().firstOrNull()?.uppercase() ?: "?",
            style = if (size < 28.dp) MaterialTheme.typography.labelSmall else MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
        if (!imageUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = {},
                loading = {},
            )
        }
    }
}

/** Vertical scrim for text over imagery. */
fun bottomScrim(color: Color, startFraction: Float = 0.35f): Brush =
    Brush.verticalGradient(
        0f to Color.Transparent,
        startFraction to Color.Transparent,
        1f to color,
    )
