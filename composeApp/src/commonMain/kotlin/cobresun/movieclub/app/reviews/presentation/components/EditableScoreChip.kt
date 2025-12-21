package cobresun.movieclub.app.reviews.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

@Composable
fun EditableScoreChip(
    imageUrl: String?,
    firstName: String?,
    lastName: String?,
    userName: String,
    currentScore: Double?,
    isEditable: Boolean,
    onScoreSubmit: (Double) -> Unit,
    size: ScoreChipSize,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }
    var inputValue by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isEditing) {
        if (isEditing) {
            inputValue = currentScore?.toRoundedString() ?: ""
            focusRequester.requestFocus()
        }
    }

    val handleSubmit = {
        val parsedScore = inputValue.toDoubleOrNull()
        if (parsedScore != null && parsedScore in 0.0..10.0) {
            onScoreSubmit(parsedScore)
            isEditing = false
        }
    }

    ScoreChipPill(
        size = size,
        modifier = modifier.clickable(enabled = isEditable) {
            if (!isEditing) isEditing = true
        },
        imageContent = {
            when {
                imageUrl != null -> {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = userName,
                        modifier = Modifier
                            .clip(CircleShape)
                            .size(size.avatarSize)
                    )
                }
                else -> {
                    // TODO: Implement initials placeholder
                    Box(
                        modifier = Modifier
                            .size(size.avatarSize)
                            .background(Color.Gray, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        // Placeholder for now
                    }
                }
            }
        },
        scoreContent = {
            when {
                isEditing -> {
                    BasicTextField(
                        value = inputValue,
                        onValueChange = { inputValue = it },
                        modifier = Modifier
                            .width(60.dp)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .focusRequester(focusRequester),
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = size.fontSize,
                            textAlign = TextAlign.Center,
                            color = Color.White
                        ),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { handleSubmit() }
                        ),
                        singleLine = true
                    )
                }
                currentScore != null -> {
                    ScoreText(currentScore, size)
                }
                isEditable -> {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add score",
                        modifier = Modifier
                            .width(60.dp)
                            .padding(horizontal = size.horizontalPadding)
                    )
                }
            }
        }
    )
}
