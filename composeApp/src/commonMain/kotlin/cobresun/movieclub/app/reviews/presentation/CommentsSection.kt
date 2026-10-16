package cobresun.movieclub.app.reviews.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.presentation.components.Avatar
import cobresun.movieclub.app.core.presentation.components.ConfirmDialog
import cobresun.movieclub.app.core.presentation.components.SectionLabel
import cobresun.movieclub.app.core.presentation.components.ShimmerBox
import cobresun.movieclub.app.core.presentation.formatShort
import cobresun.movieclub.app.reviews.domain.ReviewComment

private const val MAX_COMMENT = 2000

@Composable
fun CommentsSection(
    comments: AsyncResult<List<ReviewComment>>,
    currentUserId: String?,
    isPosting: Boolean,
    onAction: (ReviewsAction) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        val count = (comments as? AsyncResult.Success)?.data?.size
        SectionLabel(if (count != null && count > 0) "Comments · $count" else "Comments")
        when (comments) {
            is AsyncResult.Loading -> repeat(2) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ShimmerBox(Modifier.size(32.dp), MaterialTheme.shapes.extraLarge)
                    ShimmerBox(Modifier.fillMaxWidth().padding(end = 40.dp).height(40.dp))
                }
            }

            is AsyncResult.Error -> Text(
                "Couldn't load comments. Pull down on the reviews to refresh.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            is AsyncResult.Success -> {
                if (comments.data.isEmpty()) {
                    Text(
                        "No comments yet. Share what you thought!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                comments.data.forEach { comment ->
                    CommentRow(comment, isMine = comment.userId == currentUserId, onAction = onAction)
                }
            }
        }
        CommentComposer(
            isPosting = isPosting,
            onSubmit = { text, spoiler -> onAction(ReviewsAction.AddComment(text, spoiler)) },
        )
    }
}

@Composable
private fun CommentRow(comment: ReviewComment, isMine: Boolean, onAction: (ReviewsAction) -> Unit) {
    var revealed by remember(comment.id) { mutableStateOf(!comment.spoiler || isMine) }
    var menuOpen by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Avatar(comment.userName, comment.userImageUrl, size = 32.dp)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(comment.userName, style = MaterialTheme.typography.labelLarge)
                comment.createdDate?.let {
                    Text(
                        "  ·  ${it.formatShort()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (comment.spoiler) {
                    Text(
                        "  SPOILER",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
            if (editing) {
                CommentEditor(
                    initial = comment.content,
                    initialSpoiler = comment.spoiler,
                    confirmLabel = "Save",
                    onCancel = { editing = false },
                    onSubmit = { text, spoiler ->
                        onAction(ReviewsAction.EditComment(comment.id, text, spoiler))
                        editing = false
                    },
                )
            } else {
                Box(
                    Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable(enabled = !revealed) { revealed = true }
                ) {
                    Text(
                        comment.content,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = if (revealed) Modifier else Modifier.blur(10.dp),
                    )
                    if (!revealed) {
                        Row(
                            Modifier
                                .matchParentSize()
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.6f)),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(Icons.Outlined.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Spoiler, tap to reveal", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
        if (isMine && !editing) {
            Box {
                IconButton(onClick = { menuOpen = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Comment options")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text("Edit") }, onClick = { menuOpen = false; editing = true })
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        onClick = { menuOpen = false; confirmDelete = true },
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete comment?",
            message = "This can't be undone.",
            confirmLabel = "Delete",
            onConfirm = { onAction(ReviewsAction.DeleteComment(comment.id)) },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun CommentComposer(isPosting: Boolean, onSubmit: (String, Boolean) -> Unit) {
    var resetKey by remember { mutableStateOf(0) }
    androidx.compose.runtime.key(resetKey) {
        CommentEditor(
            initial = "",
            initialSpoiler = false,
            confirmLabel = null,
            isPosting = isPosting,
            onCancel = null,
            onSubmit = { text, spoiler ->
                onSubmit(text, spoiler)
                resetKey++
            },
        )
    }
}

@Composable
private fun CommentEditor(
    initial: String,
    initialSpoiler: Boolean,
    confirmLabel: String?,
    onCancel: (() -> Unit)?,
    onSubmit: (String, Boolean) -> Unit,
    isPosting: Boolean = false,
) {
    var text by remember { mutableStateOf(initial) }
    var spoiler by remember { mutableStateOf(initialSpoiler) }
    val canSend = text.isNotBlank() && !isPosting
    Column {
        OutlinedTextField(
            value = text,
            onValueChange = { if (it.length <= MAX_COMMENT) text = it },
            placeholder = { Text("Add a comment…") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            maxLines = 6,
            trailingIcon = if (confirmLabel == null) {
                {
                    if (isPosting) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = { onSubmit(text.trim(), spoiler) }, enabled = canSend) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Post comment",
                                tint = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            } else null,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = spoiler, onCheckedChange = { spoiler = it })
            Text("Contains spoilers", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            if (onCancel != null) TextButton(onClick = onCancel) { Text("Cancel") }
            if (confirmLabel != null) {
                TextButton(onClick = { onSubmit(text.trim(), spoiler) }, enabled = canSend) { Text(confirmLabel) }
            }
        }
    }
}
