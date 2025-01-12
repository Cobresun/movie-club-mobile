package cobresun.movieclub.app.reviews.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlin.math.roundToLong

@Composable
fun ScoreChip(
    imageUrl: String,
    contentDescription: String,
    score: Double,
    modifier: Modifier = Modifier
) {
    ScoreChipPill(
        modifier = modifier,
        imageContent = {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                modifier = Modifier
                    .clip(CircleShape)
                    .size(32.dp)
            )
        },
        scoreContent = { ScoreText(score) }
    )
}

@Composable
fun ScoreChip(
    image: ImageVector,
    contentDescription: String,
    score: Double,
    modifier: Modifier = Modifier
) {
    ScoreChipPill(
        modifier = modifier,
        imageContent = {
            Image(
                imageVector = image,
                contentDescription = contentDescription,
                modifier = Modifier
                    .clip(CircleShape)
                    .size(32.dp)
            )
        },
        scoreContent = { ScoreText(score) }
    )
}

@Composable
fun ScoreChip(
    firstName: String?,
    lastName: String?,
    contentDescription: String,
    score: Double,
    modifier: Modifier = Modifier
) {
    ScoreChipPill(
        modifier = modifier,
        imageContent = {
            TODO("Add placeholder image using user's initials")
        },
        scoreContent = { ScoreText(score) }
    )
}

@Composable
fun ScoreChipPill(
    modifier: Modifier = Modifier,
    imageContent: @Composable () -> Unit,
    scoreContent: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .background(
                color = Color(0xFF393E46),
                shape = RoundedCornerShape(100)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            imageContent()
            scoreContent()
        }
    }
}

@Composable
fun ScoreText(score: Double) {
    Text(
        text = score.toRoundedString(),
        modifier = Modifier.padding(horizontal = 8.dp),
        color = Color.White,
        style = TextStyle.Default.copy(fontSize = 12.sp),
        textAlign = TextAlign.Center,
        maxLines = 1,
    )
}

/**
 * Rounds the double to two decimal places.
 */
fun Double.toRoundedString(): String {
    return ((this * 100.0).roundToLong() / 100.0).toString()
}
