package cobresun.movieclub.app.member.presentation.components

import cobresun.movieclub.app.core.presentation.initialsOf
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.presentation.fadeUpOnAppear
import androidx.compose.ui.unit.sp
import cobresun.movieclub.app.core.domain.ClubType
import coil3.compose.AsyncImage

/** Uppercase caption above a [SettingsGroup], like iOS grouped lists. */
@Composable
fun SettingsSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        letterSpacing = 1.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingsFooter(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp)
    )
}

/** Rounded card holding rows separated by inset dividers. */
@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsDivider(inset: Dp = 56.dp) {
    HorizontalDivider(
        modifier = Modifier.padding(start = inset),
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
        thickness = 0.5.dp,
    )
}

@Composable
fun SettingsIcon(icon: ImageVector, tint: Color = MaterialTheme.colorScheme.primary) {
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.18f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
fun SettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = MaterialTheme.colorScheme.primary,
    subtitle: String? = null,
    value: String? = null,
    destructive: Boolean = false,
    showChevron: Boolean = false,
    enabled: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val titleColor = when {
        destructive -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .then(if (onClick != null && enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        when {
            leading != null -> leading()
            icon != null -> SettingsIcon(icon, if (destructive) MaterialTheme.colorScheme.error else iconTint)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (enabled) titleColor else titleColor.copy(alpha = 0.5f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (value != null) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 180.dp)
            )
        }
        trailing?.invoke()
        if (showChevron) {
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SettingsSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    SettingsRow(
        title = title,
        icon = icon,
        subtitle = subtitle,
        enabled = enabled,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                )
            )
        }
    )
}

/** Circular avatar: remote image when available, otherwise initials on a tinted disc. */
@Composable
fun MemberAvatar(
    name: String,
    imageUrl: String?,
    size: Dp = 40.dp,
    modifier: Modifier = Modifier,
) {
    val initials = initialsOf(name)
    val fallback: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initials,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                fontSize = (size.value * 0.38f).sp,
            )
        }
    }
    Box(modifier = modifier.size(size).clip(CircleShape)) {
        // Initials underneath; the photo covers them once it loads.
        fallback()
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

fun ClubType.icon(): ImageVector = when (this) {
    ClubType.MOVIE -> Icons.Default.Movie
    ClubType.BOOK -> Icons.AutoMirrored.Filled.MenuBook
}

fun ClubType.label(): String = when (this) {
    ClubType.MOVIE -> "Movie club"
    ClubType.BOOK -> "Book club"
}

/** Small pill label, e.g. "Admin" or "You". */
@Composable
fun Badge(text: String, color: Color = MaterialTheme.colorScheme.primary) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

/** Centered illustration + message + optional actions for empty and error states. */
@Composable
fun StatusMessage(
    icon: ImageVector,
    title: String,
    message: String? = null,
    modifier: Modifier = Modifier,
    primaryAction: Pair<String, () -> Unit>? = null,
    secondaryAction: Pair<String, () -> Unit>? = null,
) {
    Column(
        // Empty and error states fade up as they appear (web EmptyState `animate-fade-up`).
        modifier = modifier.fadeUpOnAppear().fillMaxWidth().padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(42.dp))
        }
        Spacer(Modifier.size(20.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        if (message != null) {
            Spacer(Modifier.size(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
            )
        }
        if (primaryAction != null) {
            Spacer(Modifier.size(24.dp))
            Button(
                onClick = primaryAction.second,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
            ) { Text(primaryAction.first, fontWeight = FontWeight.SemiBold) }
        }
        if (secondaryAction != null) {
            Spacer(Modifier.size(10.dp))
            OutlinedButton(
                onClick = secondaryAction.second,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                shape = RoundedCornerShape(12.dp),
            ) { Text(secondaryAction.first, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable
fun ErrorMessage(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    StatusMessage(
        icon = Icons.Default.ErrorOutline,
        title = "Something went wrong",
        message = message,
        modifier = modifier,
        primaryAction = "Try again" to onRetry,
    )
}

/** Horizontal spacer helper. */
@Composable
fun HSpace(width: Dp) = Spacer(Modifier.width(width))
