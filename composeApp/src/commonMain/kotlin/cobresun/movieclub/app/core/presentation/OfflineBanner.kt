package cobresun.movieclub.app.core.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cobresun.movieclub.app.core.domain.Connectivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.koin.compose.viewmodel.koinViewModel

class ConnectivityViewModel(private val connectivity: Connectivity) : ViewModel() {
    val isOnline: StateFlow<Boolean> = connectivity.isOnline
    val showingSavedData: StateFlow<Boolean> = connectivity.showingSavedData
    val reconnected: Flow<Unit> = connectivity.reconnected

    fun retry() = connectivity.retry()
}

/** Runs [onReconnect] each time the connection comes back (screens reload quietly). */
@Composable
fun OnReconnect(viewModel: ConnectivityViewModel = koinViewModel(), onReconnect: () -> Unit) {
    val action by rememberUpdatedState(onReconnect)
    LaunchedEffect(viewModel) { viewModel.reconnected.collect { action() } }
}

/**
 * A small pill saying the app is showing saved data: offline, or online while the server
 * can't be reached (then tapping it retries). Hidden while answers are fresh.
 */
@Composable
fun OfflineBanner(modifier: Modifier = Modifier, viewModel: ConnectivityViewModel = koinViewModel()) {
    val online by viewModel.isOnline.collectAsStateWithLifecycle()
    val showingSaved by viewModel.showingSavedData.collectAsStateWithLifecycle()
    val serverUnreachable = online && showingSaved
    AnimatedVisibility(
        visible = !online || serverUnreachable,
        modifier = modifier,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
    ) {
        Surface(
            onClick = viewModel::retry,
            enabled = serverUnreachable,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            shadowElevation = 4.dp,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.CloudOff, contentDescription = null, modifier = Modifier.size(16.dp))
                Text(
                    if (serverUnreachable) "Can't reach the server · showing saved data" else "Offline · showing saved data",
                    style = MaterialTheme.typography.labelLarge,
                )
                if (serverUnreachable) {
                    Text("Retry", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}
