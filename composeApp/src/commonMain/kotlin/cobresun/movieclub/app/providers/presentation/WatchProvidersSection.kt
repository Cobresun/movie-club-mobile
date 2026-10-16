package cobresun.movieclub.app.providers.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import cobresun.movieclub.app.core.domain.onSuccess
import cobresun.movieclub.app.core.presentation.components.SectionLabel
import cobresun.movieclub.app.providers.domain.WatchAvailability
import cobresun.movieclub.app.providers.domain.WatchProvidersRepository
import coil3.compose.AsyncImage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/** Caches streaming availability per movie for the detail sheets on a screen. */
class WatchProvidersViewModel(
    private val repository: WatchProvidersRepository,
) : ViewModel() {
    private val _availability = MutableStateFlow<Map<String, WatchAvailability?>>(emptyMap())
    val availability = _availability.asStateFlow()
    private val requested = mutableSetOf<String>()

    fun load(tmdbId: String, region: String) {
        val key = "$tmdbId/$region"
        if (!requested.add(key)) return
        viewModelScope.launch {
            // Failures just hide the section; it's supplementary info.
            repository.getProviders(tmdbId, region)
                .onSuccess { result -> _availability.update { it + (key to result) } }
        }
    }
}

/**
 * "Where to watch" logos for a movie in the device's region. Renders nothing until
 * there is something to show, and nothing at all when the region has no streaming options.
 */
@Composable
fun WatchProvidersSection(
    tmdbId: String,
    modifier: Modifier = Modifier,
    viewModel: WatchProvidersViewModel = koinViewModel(),
) {
    val region = Locale.current.region.takeIf { it.length == 2 } ?: return
    LaunchedEffect(tmdbId, region) { viewModel.load(tmdbId, region) }
    val all by viewModel.availability.collectAsStateWithLifecycle()
    val availability = all["$tmdbId/$region"] ?: return
    val uriHandler = LocalUriHandler.current

    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("Where to watch")
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            availability.providers.forEach { provider ->
                AsyncImage(
                    model = provider.logoUrl,
                    contentDescription = provider.name,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = availability.link != null) {
                            availability.link?.let { runCatching { uriHandler.openUri(it) } }
                        },
                )
            }
        }
        Text(
            "Streaming data powered by JustWatch",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        )
    }
}
