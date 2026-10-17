package cobresun.movieclub.app.lists.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import cobresun.movieclub.app.core.presentation.components.AppBottomSheet
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.awards.presentation.components.DragHandle
import cobresun.movieclub.app.awards.presentation.components.ReorderableColumn
import cobresun.movieclub.app.core.presentation.components.ConfirmSheet
import cobresun.movieclub.app.lists.domain.WorkList

/**
 * Web `ManageListsModal`: create a list inline, drag lists into a new order
 * by their handle, rename one in place, or delete it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageListsSheet(
    lists: List<WorkList>,
    /** "movie" / "book", for the delete confirmation. */
    noun: String,
    onAction: (ListsAction) -> Unit,
    onDismiss: () -> Unit,
) {
    var newTitle by remember { mutableStateOf("") }
    var renamingId by remember { mutableStateOf<String?>(null) }
    var renameTitle by remember { mutableStateOf("") }
    var deleting by remember { mutableStateOf<WorkList?>(null) }

    fun create() {
        val title = newTitle.trim()
        if (title.isEmpty()) return
        onAction(ListsAction.CreateList(title))
        newTitle = ""
    }

    fun confirmRename(listId: String) {
        val title = renameTitle.trim()
        if (title.isEmpty()) return
        onAction(ListsAction.RenameList(listId, title))
        renamingId = null
    }

    AppBottomSheet(
        onDismissRequest = onDismiss,
    ) {
        Column(
            Modifier
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Manage Lists", style = MaterialTheme.typography.titleLarge)

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = newTitle,
                    onValueChange = { newTitle = it.take(100) },
                    placeholder = { Text("New list name…") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { create() }),
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = ::create, enabled = newTitle.isNotBlank()) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Create")
                }
            }

            ReorderableColumn(
                items = lists,
                key = { it.id },
                onMove = { from, to -> lists.getOrNull(from)?.let { onAction(ListsAction.MoveList(it.id, to - from)) } },
                enabled = renamingId == null,
            ) { list ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.medium)
                        .padding(start = 8.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DragHandle(handleModifier, contentDescription = "Drag to reorder ${list.title}")
                    if (renamingId == list.id) {
                        OutlinedTextField(
                            value = renameTitle,
                            onValueChange = { renameTitle = it.take(100) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { confirmRename(list.id) }),
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { confirmRename(list.id) }, enabled = renameTitle.isNotBlank()) { Text("Save") }
                        TextButton(onClick = { renamingId = null }) { Text("Cancel") }
                    } else {
                        Text(
                            list.title,
                            style = MaterialTheme.typography.titleSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "(${list.itemCount})",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        IconButton(onClick = {
                            renamingId = list.id
                            renameTitle = list.title
                        }) {
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

    deleting?.let { list ->
        ConfirmSheet(
            destructive = true,
            title = "Delete list",
            message = "Are you sure? ${noun.replaceFirstChar { it.uppercase() }}s on this list will not be deleted from the club.",
            confirmLabel = "Delete",
            onConfirm = { onAction(ListsAction.DeleteList(list.id)) },
            onDismiss = { deleting = null },
        )
    }
}
