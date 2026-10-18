package cobresun.movieclub.app.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.domain.Constants.BASE_URL

/**
 * Shown when the server is broken at launch (5xx or an unreadable response), so the app
 * can't sign anyone in. The web app is still served, so offer to open it there instead.
 */
@Composable
fun ServiceUnavailableScreen(
    onRetry: () -> Unit,
) {
    val uriHandler = LocalUriHandler.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            "Movie Club is having trouble right now",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() },
        )
        Text(
            "The service isn't responding correctly. You can keep using Movie Club on the web while we fix this.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )
        Button(
            onClick = { uriHandler.openUri(BASE_URL) },
            modifier = Modifier.fillMaxWidth().padding(top = 32.dp).heightIn(min = 52.dp),
        ) {
            Text("Open in browser", fontWeight = FontWeight.SemiBold)
        }
        OutlinedButton(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp).heightIn(min = 52.dp),
        ) {
            Text("Try again")
        }
    }
}
