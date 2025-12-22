package cobresun.movieclub.app.core.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.app.AppTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
fun MovieActionBottomSheetContent(
    title: String,
    onDelete: () -> Unit,
    primaryButtonText: String?,
    onPrimaryButtonClick: () -> Unit,
    secondaryButtonText: String? = null,
    onSecondaryButtonClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = title,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            OutlinedButton(
                onClick = onDelete,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(4.dp),
                content = {
                    Text(text = "Delete")
                }
            )

            primaryButtonText?.let {
                Button(
                    onClick = onPrimaryButtonClick,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(4.dp),
                    content = {
                        Text(text = it)
                    }
                )
            }
        }

        secondaryButtonText?.let {
            Button(
                onClick = onSecondaryButtonClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                content = {
                    Text(text = it)
                }
            )
        }
    }
}

@Composable
@Preview
private fun MovieActionBottomSheetContentPreview() {
    AppTheme {
        MovieActionBottomSheetContent(
            title = "The Lord of the Rings: The Fellowship of the Ring",
            onDelete = {},
            primaryButtonText = "Review",
            onPrimaryButtonClick = {},
        )
    }
}
