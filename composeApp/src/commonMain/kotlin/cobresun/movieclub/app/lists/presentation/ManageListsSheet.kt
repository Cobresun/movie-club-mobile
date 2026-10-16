package cobresun.movieclub.app.lists.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.presentation.components.ConfirmDialog
import cobresun.movieclub.app.core.presentation.components.TextInputDialog
import cobresun.movieclub.app.lists.domain.WorkList

/** Create, rename, delete and reorder the club's lists. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageListsSheet(
    lists: List<WorkList>,
    onAction: (ListsAction) -> Unit,
    onDismiss: () -> Unit,
) {
    var renaming by remember { mutableStateOf<WorkList?>(null) }
    var deleting by remember { mutableStateOf<WorkList?>(null) }
    var creating by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Manage lists", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                FilledTonalButton(onClick = { creating = true }) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("New")
                }
            }
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(lists, key = { _, l -> l.id }) { index, list ->
                    Row(
                        modifier = Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(list.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${list.itemCount} item${if (list.itemCount == 1) "" else "s"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        IconButton(onClick = { onAction(ListsAction.MoveList(list.id, -1)) }, enabled = index > 0) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move ${list.title} up")
                        }
                        IconButton(onClick = { onAction(ListsAction.MoveList(list.id, 1)) }, enabled = index < lists.lastIndex) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move ${list.title} down")
                        }
                        IconButton(onClick = { renaming = list }) {
                            Icon(Icons.Outlined.Edit, contentDescription = "Rename ${list.title}")
                        }
                        IconButton(onClick = { deleting = list }) {
                            Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete ${list.title}", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }

    renaming?.let { list ->
        TextInputDialog(
            title = "Rename list",
            initialValue = list.title,
            confirmLabel = "Save",
            placeholder = "List name",
            onConfirm = { onAction(ListsAction.RenameList(list.id, it)) },
            onDismiss = { renaming = null },
        )
    }
    deleting?.let { list ->
        ConfirmDialog(
            title = "Delete \"${list.title}\"?",
            message = if (list.itemCount > 0) {
                "Its ${list.itemCount} item${if (list.itemCount == 1) "" else "s"} will be removed too. This can't be undone."
            } else {
                "This can't be undone."
            },
            confirmLabel = "Delete",
            onConfirm = { onAction(ListsAction.DeleteList(list.id)) },
            onDismiss = { deleting = null },
        )
    }
    if (creating) {
        TextInputDialog(
            title = "New list",
            initialValue = "",
            confirmLabel = "Create",
            placeholder = "e.g. Halloween picks",
            onConfirm = { onAction(ListsAction.CreateList(it)) },
            onDismiss = { creating = false },
        )
    }
}
