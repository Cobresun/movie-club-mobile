package cobresun.movieclub.app.stats.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.HourglassEmpty
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.ClubType
import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.platform.HapticFeedback
import cobresun.movieclub.app.core.presentation.components.EmptyState
import cobresun.movieclub.app.core.presentation.components.ErrorState
import cobresun.movieclub.app.core.presentation.components.ShimmerBox
import cobresun.movieclub.app.core.presentation.rememberReducedMotion
import cobresun.movieclub.app.core.presentation.toUserMessage
import cobresun.movieclub.app.theme.WrappedColors
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun WrappedScreenRoot(
    onClose: () -> Unit,
    onGoToReviews: () -> Unit,
    viewModel: WrappedViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WrappedScreen(
        state = state,
        onAction = viewModel::onAction,
        onClose = onClose,
        onGoToReviews = onGoToReviews,
        hapticFeedback = koinInject(),
    )
}

/**
 * Club Wrapped, full screen over the app's chrome so the story card can use
 * the whole display (web WrappedView.vue).
 */
@Composable
fun WrappedScreen(
    state: WrappedState,
    onAction: (WrappedAction) -> Unit,
    onClose: () -> Unit,
    onGoToReviews: () -> Unit,
    hapticFeedback: HapticFeedback,
) {
    val reducedMotion = rememberReducedMotion()
    Box(Modifier.fillMaxSize().background(WrappedColors.Backdrop)) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding(),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(start = 4.dp, end = 12.dp, top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = onClose,
                    colors = IconButtonDefaults.iconButtonColors(contentColor = WrappedColors.Ink),
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Close Wrapped", modifier = Modifier.size(28.dp))
                }
                if (state.years.isNotEmpty()) {
                    YearChips(state.years, state.selectedYear, reducedMotion, { onAction(WrappedAction.SelectYear(it)) }, Modifier.weight(1f))
                }
            }

            Box(Modifier.weight(1f).fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp)) {
                val noun = if (state.clubType == ClubType.BOOK) "book" else "movie"
                when (val content = state.content) {
                    is AsyncResult.Loading -> ShimmerBox(
                        Modifier.align(Alignment.Center).padding(top = 32.dp).widthIn(max = 360.dp).fillMaxWidth().aspectRatio(9f / 16f),
                        RoundedCornerShape(16.dp),
                    )
                    is AsyncResult.Error -> ErrorState(
                        message = (content.dataError as? DataError.Remote)?.toUserMessage("load Wrapped") ?: "Couldn't load Wrapped.",
                        onRetry = { onAction(WrappedAction.Retry) },
                        modifier = Modifier.align(Alignment.Center),
                    )
                    is AsyncResult.Success -> when (val c = content.data) {
                        WrappedContent.NothingYet -> EmptyState(
                            icon = Icons.Outlined.AutoAwesome,
                            title = "Nothing to wrap yet",
                            message = "Wrapped appears once your club has scored some ${noun}s.",
                            actionLabel = "Go to Reviews",
                            onAction = onGoToReviews,
                            modifier = Modifier.align(Alignment.Center),
                        )
                        is WrappedContent.NotOutYet -> EmptyState(
                            icon = Icons.Outlined.HourglassEmpty,
                            title = "${c.year} Wrapped isn't out yet",
                            message = "Your club's ${c.year} Wrapped arrives on January 1, ${c.year + 1}.",
                            modifier = Modifier.align(Alignment.Center),
                        )
                        is WrappedContent.EmptyYear -> EmptyState(
                            icon = Icons.Outlined.EventBusy,
                            title = "Nothing to wrap for ${c.year}",
                            message = "Your club didn't score any ${noun}s in ${c.year}.",
                            modifier = Modifier.align(Alignment.Center),
                        )
                        // A new year is a new story: start it from its first card.
                        is WrappedContent.Deck -> key(c.year) {
                            WrappedStory(
                            cards = c.cards,
                            clubName = state.clubName,
                            year = c.year,
                            hapticFeedback = hapticFeedback,
                            reducedMotion = reducedMotion,
                        )
                        }
                    }
                }
            }
        }
    }
}

/** Scrollable year chips; the open year is brought to the middle of the row. */
@Composable
private fun YearChips(
    years: List<Int>,
    selected: Int?,
    reducedMotion: Boolean,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(selected, years) {
        val index = years.indexOf(selected)
        if (index < 0) return@LaunchedEffect
        if (listState.layoutInfo.visibleItemsInfo.none { it.index == index }) listState.scrollToItem(index)
        val info = listState.layoutInfo
        val item = info.visibleItemsInfo.firstOrNull { it.index == index } ?: return@LaunchedEffect
        val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2f
        val delta = item.offset + item.size / 2f - viewportCenter
        if (reducedMotion) listState.scrollBy(delta) else listState.animateScrollBy(delta, tween(300))
    }
    LazyRow(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        items(years, key = { it }) { year ->
            val isSelected = year == selected
            val background by animateColorAsState(
                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerLow,
                tween(150),
            )
            Text(
                "$year",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = WrappedColors.Ink.copy(alpha = if (isSelected) 1f else 0.7f),
                modifier = Modifier
                    .clip(CircleShape)
                    .background(background)
                    .semantics { this.selected = isSelected }
                    .clickable { onSelect(year) }
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}
