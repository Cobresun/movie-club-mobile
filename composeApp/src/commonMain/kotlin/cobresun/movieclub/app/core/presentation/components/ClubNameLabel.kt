package cobresun.movieclub.app.core.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.club.presentation.LocalAccountMenu
import cobresun.movieclub.app.club.presentation.LocalClubSwitcher
import cobresun.movieclub.app.club.presentation.LocalCurrentClubName
import cobresun.movieclub.app.club.presentation.LocalCurrentMember
import cobresun.movieclub.app.member.presentation.components.MemberAvatar

/** Tappable eyebrow above a tab title: names the current club and opens the club switcher sheet. */
@Composable
fun ClubNameLabel(name: String) {
    val openSwitcher = LocalClubSwitcher.current
    // While a tab's own club context loads, use the name the shell already knows.
    val shown = name.ifBlank { LocalCurrentClubName.current }.ifBlank { "Clubs" }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            // Keeps long names clear of the account button the club shell floats top-right.
            .padding(end = 36.dp)
            .clip(MaterialTheme.shapes.small)
            .clickable(onClickLabel = "Switch club", role = Role.Button) { openSwitcher() }
            .semantics { contentDescription = "Club menu. Current: $shown" }
            .padding(vertical = 4.dp, horizontal = 2.dp),
    ) {
        Text(
            shown.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Spacer(Modifier.width(2.dp))
        Icon(
            Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(18.dp),
        )
    }
}

/** The signed-in member's avatar; opens the account sheet (web `AccountMenu`). */
@Composable
fun AccountButton(modifier: Modifier = Modifier, size: Dp = 32.dp) {
    val me = LocalCurrentMember.current
    val open = LocalAccountMenu.current
    MemberAvatar(
        name = me?.name.orEmpty(),
        imageUrl = me?.imageUrl,
        size = size,
        modifier = modifier
            .minimumInteractiveComponentSize()
            .clip(CircleShape)
            .clickable(onClickLabel = "Account", role = Role.Button) { open() }
            .semantics { contentDescription = "Account" },
    )
}
