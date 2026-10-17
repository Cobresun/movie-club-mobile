package cobresun.movieclub.app.club.presentation

import cobresun.movieclub.app.core.presentation.components.AppBottomSheet
import cobresun.movieclub.app.core.presentation.components.rememberAppSheetState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cobresun.movieclub.app.core.domain.Club
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.member.presentation.components.MemberAvatar
import cobresun.movieclub.app.member.presentation.components.label
import kotlinx.coroutines.launch

/**
 * The club switcher, a bottom sheet like the mobile web's (`ClubSwitcher` +
 * `ClubPanelRows`): the current club with its members & settings, the other
 * clubs, and a way to start one (that screen also joins by invite link).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ClubSwitcherSheet(
    clubs: List<Club>,
    current: Club?,
    meta: String,
    onDismiss: () -> Unit,
    onSelectClub: (Club) -> Unit,
    onSettings: (Club) -> Unit,
    /** The new-club screen, which also takes a pasted invite link. */
    onCreateClub: () -> Unit,
) {
    val sheetState = rememberAppSheetState()
    val scope = rememberCoroutineScope()
    // Slide the sheet away first, then act, so navigation doesn't cut the animation off.
    fun closeThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss(); action() }
    }
    AppBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(bottom = 16.dp)) {
            if (current != null) {
                Row(
                    Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    MemberAvatar(current.name, null, size = 48.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            current.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (meta.isNotEmpty()) {
                            Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                SheetDivider()
                SheetRow(Icons.Outlined.Groups, "Members & settings", chevron = true) { closeThen { onSettings(current) } }
            }

            if (clubs.size > 1) {
                SheetDivider()
                Text(
                    "YOUR CLUBS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                    modifier = Modifier.padding(start = 20.dp, top = 14.dp, bottom = 6.dp),
                )
                clubs.forEach { club ->
                    val selected = club.slug == current?.slug
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp)
                            .clickable(role = Role.Button) { if (selected) closeThen {} else closeThen { onSelectClub(club) } }
                            .padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        MemberAvatar(club.name, null, size = 34.dp)
                        Column(Modifier.weight(1f)) {
                            Text(
                                club.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                color = if (selected) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(club.type.label(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (selected) {
                            Icon(Icons.Default.Check, contentDescription = "Current club", tint = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }

            SheetDivider(Modifier.padding(top = 8.dp))
            SheetRow(Icons.Default.Add, "Create new club") { closeThen(onCreateClub) }
        }
    }
}

/**
 * The account menu as a bottom sheet (web `AccountPanel`): who's signed in,
 * a way to the profile, and logging out. Nothing is edited here.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountSheet(
    me: Member?,
    isLoggingOut: Boolean,
    onDismiss: () -> Unit,
    onProfile: () -> Unit,
    onLogout: () -> Unit,
) {
    val sheetState = rememberAppSheetState()
    val scope = rememberCoroutineScope()
    fun closeThen(action: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { onDismiss(); action() }
    }
    AppBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
            Row(
                Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                MemberAvatar(me?.name.orEmpty(), me?.imageUrl, size = 60.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        me?.name.orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        me?.email.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            SheetDivider()
            SheetRow(Icons.Outlined.Person, "Profile", chevron = true) { closeThen(onProfile) }
            SheetDivider(Modifier.padding(top = 4.dp))
            // Logging out tears the club shell down, so it runs straight away.
            SheetRow(
                Icons.AutoMirrored.Filled.Logout,
                if (isLoggingOut) "Logging out…" else "Log out",
                tint = MaterialTheme.colorScheme.error,
                enabled = !isLoggingOut,
                busy = isLoggingOut,
                onClick = onLogout,
            )
        }
    }
}

@Composable
private fun SheetDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier, color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
}

@Composable
private fun SheetRow(
    icon: ImageVector,
    label: String,
    chevron: Boolean = false,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true,
    busy: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        } else {
            Icon(
                icon,
                contentDescription = null,
                tint = if (tint == MaterialTheme.colorScheme.onSurface) MaterialTheme.colorScheme.onSurfaceVariant else tint,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(label, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = tint, modifier = Modifier.weight(1f))
        if (chevron) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }
    }
}
