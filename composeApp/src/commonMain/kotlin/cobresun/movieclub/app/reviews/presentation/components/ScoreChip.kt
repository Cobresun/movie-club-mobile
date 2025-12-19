package cobresun.movieclub.app.reviews.presentation.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cobresun.movieclub.app.core.presentation.LIGHT_GRAY
import coil3.compose.AsyncImage
import kotlin.math.roundToLong

enum class ScoreChipSize(
    val avatarSize: Dp,
    val fontSize: TextUnit,
    val horizontalPadding: Dp
) {
    Small(avatarSize = 32.dp, fontSize = 12.sp, horizontalPadding = 8.dp),
    Large(avatarSize = 48.dp, fontSize = 18.sp, horizontalPadding = 12.dp)
}

@Composable
fun ScoreChip(
    imageUrl: String,
    contentDescription: String,
    score: Double,
    modifier: Modifier = Modifier,
    size: ScoreChipSize = ScoreChipSize.Small
) {
    ScoreChipPill(
        size = size,
        modifier = modifier,
        imageContent = {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                modifier = Modifier
                    .clip(CircleShape)
                    .size(size.avatarSize)
            )
        },
        scoreContent = { ScoreText(score, size) }
    )
}

@Composable
fun ScoreChip(
    image: ImageVector,
    contentDescription: String,
    score: Double,
    modifier: Modifier = Modifier,
    size: ScoreChipSize = ScoreChipSize.Small
) {
    ScoreChipPill(
        size = size,
        modifier = modifier,
        imageContent = {
            Image(
                imageVector = image,
                contentDescription = contentDescription,
                modifier = Modifier
                    .clip(CircleShape)
                    .size(size.avatarSize)
            )
        },
        scoreContent = { ScoreText(score, size) }
    )
}

@Composable
fun ScoreChip(
    firstName: String?,
    lastName: String?,
    contentDescription: String,
    score: Double,
    modifier: Modifier = Modifier,
    size: ScoreChipSize = ScoreChipSize.Small
) {
    ScoreChipPill(
        size = size,
        modifier = modifier,
        imageContent = {
            // TODO("Add placeholder image using user's initials")
        },
        scoreContent = { ScoreText(score, size) }
    )
}

@Composable
fun ScoreChipPill(
    size: ScoreChipSize,
    modifier: Modifier = Modifier,
    imageContent: @Composable () -> Unit,
    scoreContent: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .wrapContentWidth()
            .background(
                color = Color(LIGHT_GRAY),
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
fun RowScope.ScoreText(score: Double, size: ScoreChipSize) {
    Text(
        text = score.toRoundedString(),
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = size.horizontalPadding)
            .wrapContentHeight(Alignment.CenterVertically),
        style = TextStyle.Default.copy(fontSize = size.fontSize, textAlign = TextAlign.Center),
        maxLines = 1,
    )
}

/**
 * Rounds the double to two decimal places.
 */
fun Double.toRoundedString(): String {
    return ((this * 100.0).roundToLong() / 100.0).toString()
}
