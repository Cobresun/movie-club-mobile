package cobresun.movieclub.app.reviews.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.presentation.components.Avatar
import cobresun.movieclub.app.core.presentation.components.ConfirmDialog
import cobresun.movieclub.app.core.presentation.components.SectionLabel
import cobresun.movieclub.app.core.presentation.components.ShimmerBox
import cobresun.movieclub.app.core.presentation.formatShort
import cobresun.movieclub.app.reviews.domain.ReviewComment
import cobresun.movieclub.app.reviews.domain.relativeTime

private const val MAX_COMMENT = 2000

/**
 * The comment thread (web `CommentThread`). It loads in place; only comments
 * added afterwards rise in, and deleted ones fade out.
 */
@Composable
fun CommentsSection(
    comments: AsyncResult<List<ReviewComment>>,
    currentUserId: String?,
    isPosting: Boolean,
    onAction: (ReviewsAction) -> Unit,
) {
    // Comments already seen (by author + text, so an optimistic copy swapping for
    // the server's doesn't animate twice). Null until the first load lands.
    val seen = remember { mutableSetOf<String>() }
    var loaded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionLabel("Comments")
        when (comments) {
            is AsyncResult.Loading -> repeat(2) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ShimmerBox(Modifier.size(28.dp), MaterialTheme.shapes.extraLarge)
                    ShimmerBox(Modifier.fillMaxWidth().padding(end = 40.dp).height(40.dp))
                }
            }

            is AsyncResult.Error -> Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Couldn't load comments.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onAction(ReviewsAction.RetryComments) }) { Text("Retry") }
            }

            is AsyncResult.Success -> {
                val firstLoad = !loaded
                SideEffect { loaded = true }
                if (comments.data.isEmpty()) {
                    Text(
                        "No comments yet. Be the first to share your thoughts!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                comments.data.forEach { comment ->
                    key(comment.id) {
                        val signature = "${comment.userId}\u0000${comment.content}"
                        val entering = remember { !firstLoad && signature !in seen }
                        SideEffect { seen += signature }
                        CommentRow(
                            comment,
                            isMine = comment.userId == currentUserId,
                            onAction = onAction,
                            modifier = if (entering) Modifier.fadeUpOnEnter() else Modifier,
                        )
                    }
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
private fun CommentRow(comment: ReviewComment, isMine: Boolean, onAction: (ReviewsAction) -> Unit, modifier: Modifier = Modifier) {
    var revealed by remember(comment.id) { mutableStateOf(!comment.spoiler || isMine) }
    var editing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var leaving by remember { mutableStateOf(false) }
    val fade = remember { Animatable(1f) }
    LaunchedEffect(leaving) {
        if (leaving) {
            fade.animateTo(0f, tween(200))
            onAction(ReviewsAction.DeleteComment(comment.id))
        }
    }

    Column(
        modifier
            .graphicsLayer { alpha = fade.value }
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Avatar(comment.userName, comment.userImageUrl, size = 28.dp)
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                Text(comment.userName, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Medium)
                val time = comment.createdAt?.let(::relativeTime) ?: comment.createdDate?.formatShort()
                time?.let {
                    Text(" · $it", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (comment.spoiler) {
                    Text(" · Spoiler", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                }
            }
            if (isMine && !editing) {
                IconButton(onClick = { editing = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Outlined.Edit, contentDescription = "Edit comment", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { confirmDelete = true }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Outlined.DeleteOutline, contentDescription = "Delete comment", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
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
            // One element for both states so the blur can ease away; hidden text is
            // kept from screen readers too, since they can't see the blur.
            val blur by animateFloatAsState(if (revealed) 0f else 10f, tween(300), label = "spoiler-blur")
            Box(
                Modifier
                    .clip(MaterialTheme.shapes.small)
                    .clickable(enabled = !revealed, onClickLabel = "Reveal spoiler") { revealed = true }
            ) {
                Text(
                    comment.content,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier
                        .blur(blur.dp)
                        .then(if (revealed) Modifier else Modifier.clearAndSetSemantics { contentDescription = "Spoiler hidden" }),
                )
                if (!revealed) {
                    // Blur isn't available on older Android, so a scrim always covers the text.
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

    if (confirmDelete) {
        ConfirmDialog(
            title = "Delete comment?",
            message = "Are you sure you want to delete this comment? This action cannot be undone.",
            confirmLabel = "Delete",
            onConfirm = { leaving = true },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun CommentComposer(isPosting: Boolean, onSubmit: (String, Boolean) -> Unit) {
    var resetKey by remember { mutableIntStateOf(0) }
    key(resetKey) {
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
    val canSend = text.isNotBlank() && text.length <= MAX_COMMENT && !isPosting
    Column {
        OutlinedTextField(
            value = text,
            onValueChange = { if (it.length <= MAX_COMMENT) text = it },
            placeholder = { Text("Write a comment…") },
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            minLines = if (confirmLabel == null) 1 else 3,
            maxLines = 6,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = spoiler, onCheckedChange = { spoiler = it })
            Text("Spoiler", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.width(12.dp))
            Text(
                "${text.length}/$MAX_COMMENT",
                style = MaterialTheme.typography.labelSmall,
                color = if (text.length > MAX_COMMENT * 0.9) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (onCancel != null) TextButton(onClick = onCancel) { Text("Cancel") }
            if (confirmLabel != null) {
                FilledTonalButton(onClick = { onSubmit(text.trim(), spoiler) }, enabled = canSend) { Text(confirmLabel) }
            } else if (isPosting) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            } else {
                IconButton(onClick = { onSubmit(text.trim(), spoiler) }, enabled = canSend) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send comment",
                        tint = if (canSend) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
