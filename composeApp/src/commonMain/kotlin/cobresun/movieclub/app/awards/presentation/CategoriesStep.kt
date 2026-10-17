package cobresun.movieclub.app.awards.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Button
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
import cobresun.movieclub.app.awards.domain.AWARD_TITLE_MAX_LENGTH
import cobresun.movieclub.app.awards.domain.AwardsSeason
import cobresun.movieclub.app.awards.domain.categoryTitleError
import cobresun.movieclub.app.awards.domain.isSameCategoryTitle
import cobresun.movieclub.app.awards.presentation.components.DragHandle
import cobresun.movieclub.app.awards.presentation.components.ReorderableColumn
import cobresun.movieclub.app.core.presentation.components.EmptyState
import cobresun.movieclub.app.core.presentation.components.TextInputDialog

fun LazyListScope.categoriesStep(
    season: AwardsSeason,
    onAction: (AwardsAction) -> Unit,
) {
    item(key = "categories-list") {
        if (season.categories.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.EmojiEvents,
                title = "No categories yet",
                message = "Add your own below or pick from the suggestions.",
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
            IconButton(onClick = { onAction(AwardsAction.DeleteCategory(category.title)) }) {
                Icon(Icons.Outlined.DeleteOutline, "Remove ${category.title}", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }

    renaming?.let { title ->
        TextInputDialog(
            title = "Rename category",
            initialValue = title,
            confirmLabel = "Save",
            placeholder = "Category name",
            onConfirm = { onAction(AwardsAction.RenameCategory(title, it)) },
            onDismiss = { renaming = null },
            maxLength = AWARD_TITLE_MAX_LENGTH,
        )
    }
}

@Composable
private fun AddCategory(season: AwardsSeason, onAction: (AwardsAction) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    fun add(title: String): Boolean {
        error = season.categoryTitleError(title)
        if (error != null) return false
        onAction(AwardsAction.AddCategory(title))
        return true
    }
    fun submit() {
        if (add(text)) text = ""
    }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextField(
                value = text,
                onValueChange = {
                    if (it.length <= AWARD_TITLE_MAX_LENGTH) text = it
                    error = null
                },
                label = { Text("New category") },
                placeholder = { Text("e.g. Best Soundtrack") },
                isError = error != null,
                supportingText = error?.let { { Text(it) } },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { submit() }),
                colors = TextFieldDefaults.colors(
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier.weight(1f),
            )
            Button(onClick = ::submit, modifier = Modifier.height(56.dp)) { Text("Add") }
        }
        val suggestions = SUGGESTED_CATEGORIES.filter { s -> season.categories.none { isSameCategoryTitle(it.title, s) } }
        if (suggestions.isNotEmpty()) {
            Text("Suggestions", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                suggestions.forEach { suggestion ->
                    AssistChip(
                        onClick = { add(suggestion) },
                        label = { Text(suggestion) },
                        leadingIcon = { Icon(Icons.Default.Add, "Add $suggestion", Modifier.size(16.dp)) },
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
