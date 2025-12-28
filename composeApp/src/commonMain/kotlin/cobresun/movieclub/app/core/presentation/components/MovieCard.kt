package cobresun.movieclub.app.core.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.theme.AppTheme
import coil3.compose.AsyncImage
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun MovieCard(
    title: String,
    posterImageUrl: String?,
    highlight: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    content: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Card(
        modifier = modifier.padding(8.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (highlight) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier.clickable { onClick() }
        ) {
            if (posterImageUrl != null && !posterImageUrl.endsWith("null")) {
                AsyncImage(
                    model = posterImageUrl,
                    contentDescription = null,
                    modifier = Modifier.height(256.dp),
                    contentScale = ContentScale.FillBounds
                )
            } else {
                Column(
                    modifier = Modifier
                        .height(256.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Missing poster",
                        modifier = Modifier.padding(32.dp),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(
                modifier = Modifier.padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = title,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                )

                content?.let { it() }
            }
        }
    }
}

@Preview
@Composable
fun MovieCardWithPosterPreview() {
    AppTheme {
        MovieCard(
            title = "Movie Title",
            posterImageUrl = "https://image.tmdb.org/t/p/w500/poster.jpg",
            highlight = false,
        )
    }
}

@Preview
@Composable
fun MovieCardWithoutPosterPreview() {
    AppTheme {
        MovieCard(
            title = "Movie Title",
            posterImageUrl = null,
            highlight = false,
        )
    }
}
