package cobresun.movieclub.app.reviews.presentation.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.domain.User
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.reviews.domain.ReviewSortOption
import cobresun.movieclub.app.reviews.domain.ReviewSortState
import coil3.compose.AsyncImage

@Composable
fun SortChipsRow(
    sortState: ReviewSortState,
    clubMembers: List<Member>,
    onSortChange: (ReviewSortOption) -> Unit,
    onToggleDirection: () -> Unit,
    onClearSort: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // "Sort by" label
        Text(
            text = "Sort by:",
            style = MaterialTheme.typography.labelMedium
        )

        // Date reviewed chip
        FilterChip(
            selected = sortState.option == ReviewSortOption.DateReviewed,
            onClick = { onSortChange(ReviewSortOption.DateReviewed) },
            label = { Text("Date") },
            leadingIcon = if (sortState.option == ReviewSortOption.DateReviewed) {
                {
                    Icon(
                        Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else null
        )

        // Average score chip
        FilterChip(
            selected = sortState.option == ReviewSortOption.AverageScore,
            onClick = { onSortChange(ReviewSortOption.AverageScore) },
            label = { Text("Average") },
            leadingIcon = if (sortState.option == ReviewSortOption.AverageScore) {
                {
                    Icon(
                        AverageIconVector,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else null
        )

        // Member score chips (dynamic based on club members)
        clubMembers.forEach { member ->
            val memberUser = User(
                id = member.id,
                name = member.name,
                imageUrl = member.imageUrl,
                email = member.email
            )
            val sortOption = ReviewSortOption.MemberScore(memberUser)
            val isSelected = sortState.option == sortOption

            FilterChip(
                selected = isSelected,
                onClick = { onSortChange(sortOption) },
                label = { Text(member.name) },
                leadingIcon = if (member.imageUrl != null && isSelected) {
                    {
                        AsyncImage(
                            model = member.imageUrl,
                            contentDescription = member.name,
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                        )
                    }
                } else null
            )
        }

        // Direction toggle (only visible when sort is active)
        if (sortState.isActive) {
            IconButton(onClick = onToggleDirection) {
                Icon(
                    imageVector = if (sortState.descending) {
                        Icons.Default.ArrowDownward
                    } else {
                        Icons.Default.ArrowUpward
                    },
                    contentDescription = if (sortState.descending) {
                        "Descending"
                    } else {
                        "Ascending"
                    }
                )
            }
        }

        // Clear sort (only visible when sort is active)
        if (sortState.isActive) {
            FilterChip(
                selected = false,
                onClick = onClearSort,
                label = { Text("Clear") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            )
        }
    }
}
