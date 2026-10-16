package cobresun.movieclub.app.lists.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DriveFileMove
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PlayCircleOutline
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.presentation.components.ConfirmDialog
import cobresun.movieclub.app.core.presentation.components.WorkAbout
import cobresun.movieclub.app.core.presentation.components.WorkHero
import cobresun.movieclub.app.core.presentation.finishedVerb
import cobresun.movieclub.app.core.presentation.formatShort
import cobresun.movieclub.app.lists.domain.WorkList
import kotlinx.coroutines.launch

/** Bottom sheet with a list item's metadata and every action you can take on it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListItemDetailSheet(
    work: Work,
    isUpNext: Boolean,
    addedByName: String?,
    currentListId: String?,
    lists: List<WorkList>,
    onAction: (ListsAction) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var confirmRemove by remember { mutableStateOf(false) }
    var confirmReview by remember { mutableStateOf(false) }
    var moveMenuOpen by remember { mutableStateOf(false) }
    val otherLists = lists.filter { it.id != currentListId }
    val listName = lists.firstOrNull { it.id == currentListId }?.title ?: "this list"

    fun act(action: ListsAction) {
        onAction(action)
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss() }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        dragHandle = null,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
        ) {
            WorkHero(work)
            Column(
                Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                val added = listOfNotNull(
                    work.addedDate?.let { "Added ${it.formatShort()}" },
                    addedByName?.let { "by $it" },
                ).joinToString(" ")
                if (added.isNotBlank()) {
                    Text(added, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                // Primary actions
                if (isUpNext) {
                    OutlinedButton(onClick = { act(ListsAction.ClearUpNext) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.PlayCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(Modifier.size(8.dp))
                        Text("Remove from Up Next")
                    }
                } else {
                    Button(onClick = { act(ListsAction.SetUpNext(work)) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Outlined.PlayCircleOutline, contentDescription = null)
                        Spacer(Modifier.size(8.dp))
                        Text("Set as Up Next")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FilledTonalButton(onClick = { confirmReview = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.RateReview, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text("Move to Reviews")
                    }
                    Box(Modifier.weight(1f)) {
                        FilledTonalButton(
                            onClick = { moveMenuOpen = true },
                            enabled = otherLists.isNotEmpty(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.AutoMirrored.Outlined.DriveFileMove, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.size(6.dp))
                            Text("Move to…")
                        }
                        DropdownMenu(expanded = moveMenuOpen, onDismissRequest = { moveMenuOpen = false }) {
                            otherLists.forEach { list ->
                                DropdownMenuItem(
                                    text = { Text(list.title) },
                                    onClick = {
                                        moveMenuOpen = false
                                        act(ListsAction.MoveWork(work, list.id))
                                    },
                                )
                            }
                        }
                    }
                }

                WorkAbout(work)

                Spacer(Modifier.height(4.dp))
                TextButton(
                    onClick = { confirmRemove = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                ) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text("Remove from $listName")
                }
            }
        }
    }

    if (confirmRemove) {
        ConfirmDialog(
            title = "Remove ${work.title}?",
            message = "It will be removed from $listName for everyone in the club.",
            confirmLabel = "Remove",
            onConfirm = { act(ListsAction.RemoveWork(work)) },
            onDismiss = { confirmRemove = false },
        )
    }
    if (confirmReview) {
        ConfirmDialog(
            title = "Move to Reviews?",
            message = "Mark ${work.title} as ${work.type.finishedVerb} so members can score it.",
            confirmLabel = "Move",
            destructive = false,
            onConfirm = { act(ListsAction.MoveToReviews(work)) },
            onDismiss = { confirmReview = false },
        )
    }
}
