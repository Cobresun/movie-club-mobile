package cobresun.movieclub.app.awards.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.awards.domain.AwardCategory
import cobresun.movieclub.app.awards.domain.AwardsSeason
import cobresun.movieclub.app.awards.presentation.components.DragHandle
import cobresun.movieclub.app.awards.presentation.components.ReorderableColumn
import cobresun.movieclub.app.core.presentation.components.ConfirmDialog
import cobresun.movieclub.app.core.presentation.components.EmptyState
import cobresun.movieclub.app.core.presentation.components.TextInputDialog

private val SUGGESTIONS = listOf(
    "Best Picture", "Best Director", "Best Actor", "Best Actress", "Best Cinematography",
    "Best Score", "Best Screenplay", "Best Villain", "Most Rewatchable", "Biggest Surprise",
)

fun LazyListScope.categoriesStep(
    season: AwardsSeason,
    onAction: (AwardsAction) -> Unit,
) {
    item(key = "categories-list") {
        if (season.categories.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.EmojiEvents,
                title = "No categories yet",
                message = "Add a few below, or start from a suggestion.",
                modifier = Modifier.padding(vertical = 0.dp),
            )
        } else {
            CategoryList(season.categories, onAction)
        }
    }
    item(key = "categories-add") {
        AddCategory(season, onAction)
    }
}

@Composable
private fun CategoryList(categories: List<AwardCategory>, onAction: (AwardsAction) -> Unit) {
    var renaming by rememberSaveable { mutableStateOf<String?>(null) }
    var deleting by rememberSaveable { mutableStateOf<String?>(null) }

    ReorderableColumn(
        items = categories,
        key = { it.title },
        onMove = { from, to -> onAction(AwardsAction.MoveCategory(from, to)) },
        modifier = Modifier.padding(horizontal = 16.dp),
    ) { category ->
        Row(
            Modifier
                .fillMaxWidth()
                .background(
                    if (isDragging) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceContainerLow,
                    MaterialTheme.shapes.medium,
                )
                .padding(start = 8.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DragHandle(handleModifier.padding(4.dp), contentDescription = "Reorder ${category.title}")
            Text(
                "${index + 1}",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(28.dp).padding(start = 6.dp),
            )
            Text(
                category.title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = { renaming = category.title }) {
                Icon(Icons.Outlined.Edit, "Rename ${category.title}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { deleting = category.title }) {
                Icon(Icons.Outlined.DeleteOutline, "Delete ${category.title}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    renaming?.let { title ->
        val nominated = categories.firstOrNull { it.title == title }?.nominations?.isNotEmpty() == true
        TextInputDialog(
            title = if (nominated) "Rename (clears nominations)" else "Rename category",
            initialValue = title,
            confirmLabel = "Save",
            placeholder = "Category name",
            onConfirm = { onAction(AwardsAction.RenameCategory(title, it)) },
            onDismiss = { renaming = null },
        )
    }
    deleting?.let { title ->
        ConfirmDialog(
            title = "Delete $title?",
            message = "This removes the category and any nominations in it for everyone.",
            confirmLabel = "Delete",
            onConfirm = { onAction(AwardsAction.DeleteCategory(title)) },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun AddCategory(season: AwardsSeason, onAction: (AwardsAction) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    fun submit() {
        if (text.isNotBlank()) {
            onAction(AwardsAction.AddCategory(text))
            text = ""
        }
    }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(
                value = text,
                onValueChange = { if (it.length <= 60) text = it },
                placeholder = { Text("Add a category") },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier.weight(1f),
            )
            FilledIconButton(onClick = ::submit, enabled = text.isNotBlank(), modifier = Modifier.size(48.dp)) {
                Icon(Icons.Default.Add, contentDescription = "Add category")
            }
        }
        val taken = season.categories.map { it.title.lowercase() }.toSet()
        val suggestions = SUGGESTIONS.filter { it.lowercase() !in taken }
        if (suggestions.isNotEmpty()) {
            Text("Suggestions", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.forEach { suggestion ->
                    AssistChip(
                        onClick = { onAction(AwardsAction.AddCategory(suggestion)) },
                        label = { Text(suggestion) },
                        leadingIcon = { Icon(Icons.Default.Add, null, Modifier.size(16.dp)) },
                        colors = AssistChipDefaults.assistChipColors(leadingIconContentColor = MaterialTheme.colorScheme.secondary),
                    )
                }
            }
        }
    }
}

/** The phase's description, under the progress bar. */
@Composable
fun StepIntro(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 20.dp, vertical = 4.dp),
    )
}
