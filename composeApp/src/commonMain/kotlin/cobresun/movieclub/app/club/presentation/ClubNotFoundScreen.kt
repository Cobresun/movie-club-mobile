package cobresun.movieclub.app.club.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ConfirmationNumber
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/** Web `ClubNotFoundView`: a club link for a club that doesn't exist or isn't yours. */
@Composable
fun ClubNotFoundScreen(onBrowseClubs: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).safeDrawingPadding().padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerLow)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.ConfirmationNumber, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(32.dp))
            }
            Text("Club not found", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Members only", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.tertiary)
            Text(
                "This club doesn't exist or you don't have access. You'll need an invitation to join a private club.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onBrowseClubs, modifier = Modifier.padding(top = 8.dp)) { Text("Browse your clubs") }
        }
    }
}
