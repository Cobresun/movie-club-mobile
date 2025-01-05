package cobresun.movieclub.app.reviews.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cobresun.movieclub.app.core.domain.User
import coil3.compose.AsyncImage

@Composable
fun ScoreChip(
    user: User,
    score: Double,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = Color(0xFF393E46),
                shape = RoundedCornerShape(100)
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (user.imageUrl != null) {
                AsyncImage(
                    model = user.imageUrl,
                    contentDescription = user.name,
                    modifier = Modifier
                        .clip(CircleShape)
                        .size(32.dp)
                )
            } else {
                TODO("Add placeholder")
            }

            Text(
                text = score.toString(),
                modifier = Modifier.weight(1f),
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
            )
        }
    }
}
