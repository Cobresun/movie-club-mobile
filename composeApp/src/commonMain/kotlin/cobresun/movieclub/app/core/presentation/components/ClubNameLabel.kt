package cobresun.movieclub.app.core.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.style.TextOverflow

/** Small eyebrow above a tab title naming the current club. */
@Composable
fun ClubNameLabel(name: String) {
    AnimatedVisibility(visible = name.isNotBlank(), enter = fadeIn()) {
        Text(
            name.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
