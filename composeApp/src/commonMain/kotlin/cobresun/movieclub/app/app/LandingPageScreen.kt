package cobresun.movieclub.app.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.presentation.fadeUpOnAppear
import cobresun.movieclub.app.theme.AppTheme
import movieclub.composeapp.generated.resources.Res
import movieclub.composeapp.generated.resources.home_cinema
import movieclub.composeapp.generated.resources.landing_review
import movieclub.composeapp.generated.resources.landing_statistics
import movieclub.composeapp.generated.resources.landing_watchlist
import movieclub.composeapp.generated.resources.logo
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private data class FeatureHighlight(val image: DrawableResource, val title: String, val description: String)

/** Web `HomeView` feature highlights. */
private val features = listOf(
    FeatureHighlight(
        Res.drawable.landing_review,
        "Reviews & Scores",
        "Score every movie as a club and see everyone's ratings side by side.",
    ),
    FeatureHighlight(
        Res.drawable.landing_watchlist,
        "Shared Lists",
        "Keep a shared watchlist and backlog so the next pick is never a debate.",
    ),
    FeatureHighlight(
        Res.drawable.landing_statistics,
        "Statistics",
        "Charts reveal your club's patterns — top genres, toughest critics, and taste twins.",
    ),
)

@Composable
fun LandingPageScreen(
    onAuthClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
    ) {
        Row(
            Modifier.padding(top = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Image(painterResource(Res.drawable.logo), contentDescription = null, modifier = Modifier.size(36.dp))
            Text("Movie Club", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }

        // Hero
        Image(
            painter = painterResource(Res.drawable.home_cinema),
            contentDescription = "Friends watching a movie together",
            modifier = Modifier.fillMaxWidth().padding(top = 32.dp).heightIn(max = 280.dp).fadeUpOnAppear(),
            contentScale = ContentScale.Fit,
        )
        Text(
            "Get your 🍿 ready for MovieClub: The Book Club for Movies",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 40.dp).semantics { heading() }.fadeUpOnAppear(delayMillis = 80),
        )
        Text(
            "Rate movies, compare favorites, and find patterns.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 16.dp).fadeUpOnAppear(delayMillis = 140),
        )
        CtaButton("Get started — it's free", onAuthClick, Modifier.padding(top = 24.dp).fadeUpOnAppear(delayMillis = 200))

        // Feature highlights
        Text(
            "Everything your club needs",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 56.dp, bottom = 24.dp).semantics { heading() },
        )
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            features.forEach { FeatureCard(it) }
        }

        // Closing CTA
        Text(
            "Movies or books — start a club, invite your friends with a single link, and settle the scores together.",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Light,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 56.dp),
        )
        CtaButton(
            "Start your club",
            onAuthClick,
            Modifier.align(Alignment.CenterHorizontally).padding(top = 24.dp),
        )
    }
}

@Composable
private fun CtaButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier.heightIn(min = 52.dp),
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun FeatureCard(feature: FeatureHighlight) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painterResource(feature.image),
                contentDescription = feature.title,
                modifier = Modifier.size(width = 144.dp, height = 80.dp),
            )
            Text(
                feature.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            )
            Text(
                feature.description,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Light,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview
@Composable
fun LandingPageScreenPreview() {
    AppTheme {
        LandingPageScreen(onAuthClick = {})
    }
}
