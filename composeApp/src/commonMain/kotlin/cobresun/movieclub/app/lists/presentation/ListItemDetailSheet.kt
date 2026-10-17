package cobresun.movieclub.app.lists.presentation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.domain.WorkDetails
import cobresun.movieclub.app.core.domain.WorkType
import cobresun.movieclub.app.core.platform.HapticFeedback
import cobresun.movieclub.app.core.presentation.components.Avatar
import cobresun.movieclub.app.core.presentation.components.ConfirmDialog
import cobresun.movieclub.app.core.presentation.components.SheetSnackbarHost
import cobresun.movieclub.app.core.presentation.components.ShimmerBox
import cobresun.movieclub.app.core.presentation.components.WorkHero
import cobresun.movieclub.app.core.presentation.finishedVerb
import cobresun.movieclub.app.core.presentation.formatShort
import cobresun.movieclub.app.lists.domain.WorkList
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.providers.presentation.WatchProvidersSection
import cobresun.movieclub.app.reviews.presentation.CastSection
import cobresun.movieclub.app.reviews.presentation.CommentsSection
import cobresun.movieclub.app.reviews.presentation.DetailsSection
import cobresun.movieclub.app.reviews.presentation.SynopsisSection
import cobresun.movieclub.app.reviews.presentation.overview
import cobresun.movieclub.app.reviews.presentation.reviewMetaLine
import kotlinx.coroutines.launch

/**
 * A list item's details (web `ListItemDetailsDrawer`): hero with who added it,
 * move/remove pills, synopsis, cast, details, where to watch and the comment
 * thread, with "Mark as watched" and the Up Next pin in a sticky footer.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListItemDetailSheet(
    work: Work,
    detail: ListItemDetailState,
    isUpNext: Boolean,
    addedBy: Member?,
    lists: List<WorkList>,
    currentUserId: String?,
    hapticFeedback: HapticFeedback,
    onAction: (ListsAction) -> Unit,
    /** Moves the work to Reviews and goes there (web "Mark as watched"). */
    onMarkFinished: (Work) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var confirmRemove by remember { mutableStateOf(false) }
    val otherLists = lists.filter { it.id != detail.listId }
    val fullDetails = (detail.details as? AsyncResult.Success)?.data
    val shown = if (fullDetails != null) work.copy(details = fullDetails) else work

    fun closeThen(block: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion {
            onDismiss()
            block()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = null,
    ) {
        Box(Modifier.fillMaxHeight(0.96f).imePadding()) {
            Column(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 24.dp),
                ) {
                    WorkHero(shown, facts = shown.reviewMetaLine()) {
                        AddedLine(work, addedBy)
                    }

                    ActionPills(
                        otherLists = otherLists,
                        onMove = { listId -> closeThen { onAction(ListsAction.MoveWork(work, detail.listId, listId)) } },
                        onRemove = { confirmRemove = true },
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp),
                    )

                    shown.overview()?.let {
                        SynopsisSection(it, Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp))
                    }

                    val cast = (shown.details as? WorkDetails.Movie)?.cast.orEmpty()
                    if (cast.isNotEmpty()) {
                        CastSection(cast, Modifier.padding(top = 24.dp))
                    } else if (detail.details is AsyncResult.Loading && work.type == WorkType.MOVIE) {
                        Row(Modifier.padding(start = 20.dp, top = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            repeat(4) { ShimmerBox(Modifier.size(64.dp), MaterialTheme.shapes.extraLarge) }
                        }
                    }

                    DetailsSection(
                        work = shown,
                        tmdbHidden = false,
                        onRevealTmdb = {},
                        showLinks = false,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp),
                    )

                    // Availability comes straight from TMDB off the external id, so it shows
                    // even before the work's cached metadata fills in (like web).
                    if (work.type == WorkType.MOVIE) {
                        work.externalId?.let {
                            Box(Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp)) { WatchProvidersSection(it) }
                        }
                    }

                    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        CommentsSection(
                            comments = detail.comments,
                            currentUserId = currentUserId,
                            isPosting = detail.isPostingComment,
                            onAdd = { text, spoiler -> onAction(ListsAction.AddComment(text, spoiler)) },
                            onEdit = { id, text, spoiler -> onAction(ListsAction.EditComment(id, text, spoiler)) },
                            onDelete = { onAction(ListsAction.DeleteComment(it)) },
                            onRetry = { onAction(ListsAction.RetryComments) },
                        )
                    }
                }
                Footer(
                    finishedVerb = work.type.finishedVerb,
                    isUpNext = isUpNext,
                    onMarkFinished = { closeThen { onMarkFinished(work) } },
                    onToggleUpNext = {
                        if (isUpNext) {
                            onAction(ListsAction.ClearUpNext)
                        } else {
                            hapticFeedback.performConfirm()
                            onAction(ListsAction.SetUpNext(work))
                        }
                    },
                )
            }
            SheetSnackbarHost()
        }
    }

    if (confirmRemove) {
        ConfirmDialog(
            title = "Remove from list",
            message = "Remove ${work.title} from this list?",
            confirmLabel = "Remove",
            onConfirm = { closeThen { onAction(ListsAction.RemoveWork(work, detail.listId)) } },
            onDismiss = { confirmRemove = false },
        )
    }
}

/** "Added by {avatar} Name on Oct 3, 2026", or just the date when the adder is unknown. */
@Composable
private fun AddedLine(work: Work, addedBy: Member?) {
    val date = work.addedDate?.formatShort()
    if (addedBy == null && date == null) return
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (addedBy != null) Avatar(name = addedBy.name, imageUrl = addedBy.imageUrl, size = 20.dp)
        Text(
            when {
                addedBy != null && date != null -> "Added by ${addedBy.name} on $date"
                addedBy != null -> "Added by ${addedBy.name}"
                else -> "Added $date"
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ActionPills(
    otherLists: List<WorkList>,
    onMove: (String) -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (otherLists.isNotEmpty()) {
            Box {
                Pill(
                    onClick = { menuOpen = true },
                    background = MaterialTheme.colorScheme.surfaceContainerHighest,
                    content = MaterialTheme.colorScheme.onSurface,
                ) {
                    Icon(Icons.Outlined.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                    Text("Move to list", style = MaterialTheme.typography.labelLarge)
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(16.dp))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    otherLists.forEach { list ->
                        DropdownMenuItem(
                            text = { Text(list.title) },
                            onClick = {
                                menuOpen = false
                                onMove(list.id)
                            },
                        )
                    }
                }
            }
        }
        Pill(onClick = onRemove, background = Color.Transparent, content = MaterialTheme.colorScheme.error) {
            Icon(Icons.Outlined.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
            Text("Remove from list", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun Pill(onClick: () -> Unit, background: Color, content: Color, body: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        androidx.compose.runtime.CompositionLocalProvider(androidx.compose.material3.LocalContentColor provides content) {
            body()
        }
    }
}

/** Web's sticky footer: "Mark as watched" with its hint, and the Up next pin toggle. */
@Composable
private fun Footer(finishedVerb: String, isUpNext: Boolean, onMarkFinished: () -> Unit, onToggleUpNext: () -> Unit) {
    val pinBackground by animateColorAsState(
        if (isUpNext) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.surfaceContainerHighest,
        label = "pin",
    )
    val pinContent by animateColorAsState(
        if (isUpNext) MaterialTheme.colorScheme.onTertiary else MaterialTheme.colorScheme.onSurface,
        label = "pin-content",
    )
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 6.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onMarkFinished,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.weight(1f).height(52.dp),
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Mark as $finishedVerb", fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = onToggleUpNext,
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = pinBackground, contentColor = pinContent),
                    modifier = Modifier.height(52.dp).semantics { selected = isUpNext },
                ) {
                    Icon(
                        if (isUpNext) Icons.Filled.PushPin else Icons.Outlined.PushPin,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Up next", fontWeight = FontWeight.Bold)
                }
            }
            Text(
                "Moves it to Reviews so the club can score it.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
        }
    }
}
