package cobresun.movieclub.app.reviews.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cobresun.movieclub.app.app.AppTheme
import cobresun.movieclub.app.core.domain.User
import cobresun.movieclub.app.reviews.domain.Score
import coil3.compose.AsyncImage
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun ReviewDetailsBottomSheetContent(
    title: String,
    createdDate: String,
    posterImageUrl: String,
    scores: Map<User, Score>,
    currentUserId: String?,
    onScoreSubmit: (scoreId: String?, scoreValue: Double) -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Movie Poster
        AsyncImage(
            model = posterImageUrl,
            contentDescription = "Poster for $title",
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Fit
        )

        // Title
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // Created Date
        Text(
            text = createdDate,
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        // User Scores Section
        if (scores.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Scores",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth()
                )

                // Score chips in 2-column grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(scores.toList()) { (user, score) ->
                        val isCurrentUser = currentUserId != null && currentUserId == user.id
                        val imageUrl = user.imageUrl

                        if (isCurrentUser) {
                            EditableScoreChip(
                                imageUrl = imageUrl,
                                firstName = user.name.split(" ").firstOrNull(),
                                lastName = user.name.split(" ").getOrNull(1),
                                userName = user.name,
                                currentScore = score.value,
                                isEditable = true,
                                onScoreSubmit = { newScore ->
                                    onScoreSubmit(score.id, newScore)
                                },
                                size = ScoreChipSize.Large
                            )
                        } else {
                            if (imageUrl != null) {
                                ScoreChip(
                                    imageUrl = imageUrl,
                                    contentDescription = user.name,
                                    score = score.value,
                                    size = ScoreChipSize.Large
                                )
                            } else {
                                val initials = user.name.split(" ")

                                if (initials.isNotEmpty()) {
                                    ScoreChip(
                                        firstName = initials.first(),
                                        lastName = if (initials.size > 1) initials.last() else null,
                                        contentDescription = user.name,
                                        score = score.value,
                                        size = ScoreChipSize.Large
                                    )
                                }
                            }
                        }
                    }

                    // Add new score if current user hasn't scored yet
                    if (currentUserId != null && !scores.keys.any { it.id == currentUserId }) {
                        item {
                            EditableScoreChip(
                                imageUrl = null,
                                firstName = null,
                                lastName = null,
                                userName = "You",
                                currentScore = null,
                                isEditable = true,
                                onScoreSubmit = { newScore ->
                                    onScoreSubmit(null, newScore)
                                },
                                size = ScoreChipSize.Large
                            )
                        }
                    }

                    // Average score chip
                    if (scores.isNotEmpty()) {
                        item {
                            ScoreChip(
                                image = AverageIconVector,
                                contentDescription = "Average",
                                score = scores.values.map { it.value }.average(),
                                size = ScoreChipSize.Large
                            )
                        }
                    }
                }
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Delete Button (Red with transparency)
            Button(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Red.copy(alpha = 0.2f),
                    contentColor = Color.Red
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Delete")
            }

            // Share Button (Primary with transparency)
            Button(
                onClick = onShare,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = "Share",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Share")
            }
        }

        // Bottom padding for comfortable dismissal
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview
@Composable
fun ReviewDetailsBottomSheetContentPreview() {
    AppTheme {
        Surface {
            ReviewDetailsBottomSheetContent(
                title = "The Lord of the Rings: The Fellowship of the Ring",
                createdDate = "2021-01-01",
                posterImageUrl = "https://www.themoviedb.org/t/p/w600_and_h900_bestv2/6oom5QYQ2yQzgp4bUS4eE2MvNte.jpg",
                scores = mapOf(
                    User(id = "1", name = "Cole", imageUrl = null, email = "cole@example.com") to Score(id = "1", value = 9.5),
                    User(id = "2", name = "Brian", imageUrl = null, email = "brian@example.com") to Score(id = "2", value = 8.0),
                    User(id = "3", name = "Wesley", imageUrl = null, email = "wesley@example.com") to Score(id = "3", value = 9.0),
                ),
                currentUserId = "2",
                onScoreSubmit = { _, _ -> },
                onDelete = {},
                onShare = {}
            )
        }
    }
}

@Preview
@Composable
fun ReviewDetailsBottomSheetContentNoScoresPreview() {
    AppTheme {
        Surface {
            ReviewDetailsBottomSheetContent(
                title = "Movie Without Scores",
                createdDate = "2024-01-15",
                posterImageUrl = "https://image.tmdb.org/t/p/w500/poster.jpg",
                scores = emptyMap(),
                currentUserId = "1",
                onScoreSubmit = { _, _ -> },
                onDelete = {},
                onShare = {}
            )
        }
    }
}
