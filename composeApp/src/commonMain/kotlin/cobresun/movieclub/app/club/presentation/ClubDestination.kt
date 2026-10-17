package cobresun.movieclub.app.club.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.runtime.derivedStateOf
import cobresun.movieclub.app.core.presentation.components.LocalNativeChromeVisible
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import cobresun.movieclub.app.core.domain.defaultClubSlug
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import cobresun.movieclub.app.app.Route
import cobresun.movieclub.app.auth.presentation.AuthAction
import cobresun.movieclub.app.auth.presentation.AuthViewModel
import cobresun.movieclub.app.core.domain.AsyncResult
import cobresun.movieclub.app.core.domain.Club
import cobresun.movieclub.app.member.domain.Member
import cobresun.movieclub.app.member.presentation.CreateClubScreenRoot
import cobresun.movieclub.app.member.presentation.JoinClubScreenRoot
import cobresun.movieclub.app.app.DeepLink
import cobresun.movieclub.app.app.DeepLinks
import cobresun.movieclub.app.member.presentation.MemberViewModel
import cobresun.movieclub.app.member.presentation.ProfileScreenRoot
import cobresun.movieclub.app.member.presentation.components.ErrorMessage
import cobresun.movieclub.app.member.presentation.components.MemberAvatar
import cobresun.movieclub.app.member.presentation.components.StatusMessage
import cobresun.movieclub.app.member.presentation.components.icon
import cobresun.movieclub.app.member.presentation.components.label
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * Opens the club switcher drawer. Screens inside the club shell (e.g. a top-bar
 * menu button in ClubScreen) can call `LocalClubSwitcher.current()`.
 */
val LocalClubSwitcher = staticCompositionLocalOf<() -> Unit> { {} }

@Composable
fun ClubsScreenRoot(
    viewModel: MemberViewModel = koinViewModel(),
    authViewModel: AuthViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val authState by authViewModel.state.collectAsStateWithLifecycle()

    ClubsScreen(
        clubs = state.clubs,
        me = state.me,
        lastClubSlug = state.lastClubSlug,
        onLogout = { authViewModel.onAction(AuthAction.Logout) },
        memberViewModel = viewModel,
        isLoggingOut = authState.isLoggingOut
    )
}

private fun NavHostController.navigateClearingStack(route: Route) {
    navigate(route) {
        popUpTo(graph.id) { inclusive = true }
        launchSingleTop = true
    }
}

@Composable
fun ClubsScreen(
    clubs: AsyncResult<List<Club>>,
    me: Member?,
    lastClubSlug: String?,
    onLogout: () -> Unit = {},
    memberViewModel: MemberViewModel,
    isLoggingOut: Boolean = false,
) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    var currentClubSlug by rememberSaveable { mutableStateOf<String?>(null) }

    fun closeDrawer() = coroutineScope.launch { drawerState.close() }

    fun selectClub(slug: String?) {
        currentClubSlug = slug
        if (slug != null) memberViewModel.rememberClub(slug)
    }

    fun openClub(slug: String) {
        selectClub(slug)
        navController.navigateClearingStack(Route.Club(slug))
    }

    // Keep the selection valid when the club list changes (left, removed, renamed slug).
    LaunchedEffect(clubs) {
        if (clubs !is AsyncResult.Success) return@LaunchedEffect
        val list = clubs.data
        val selected = currentClubSlug
        when {
            list.isEmpty() -> if (selected != null) {
                currentClubSlug = null
                navController.navigateClearingStack(Route.EmptyClubs)
            }
            // Matches ClubNavHost's start destination.
            selected == null -> selectClub(defaultClubSlug(list, lastClubSlug))
            list.none { it.slug == selected } -> openClub(list.first().slug)
        }
    }

    ModalNavigationDrawer(
        drawerContent = {
            ClubSwitcherDrawer(
                clubs = clubs,
                me = me,
                currentClubSlug = currentClubSlug,
                onClubClick = { clubSlug ->
                    if (clubSlug != currentClubSlug) openClub(clubSlug)
                    closeDrawer()
                },
                onSettingsClick = { clubSlug ->
                    navController.navigate(Route.ClubSettings(clubSlug))
                    closeDrawer()
                },
                onCreateClubClick = {
                    navController.navigate(Route.CreateClub)
                    closeDrawer()
                },
                onJoinClubClick = {
                    navController.navigate(Route.JoinClub())
                    closeDrawer()
                },
                onProfileClick = {
                    navController.navigate(Route.Profile)
                    closeDrawer()
                },
                onLogout = onLogout,
                isLoggingOut = isLoggingOut,
            )
        },
        modifier = Modifier.background(color = MaterialTheme.colorScheme.surface),
        drawerState = drawerState,
    ) {
        val drawerClosed by remember {
            derivedStateOf {
                drawerState.currentValue == DrawerValue.Closed &&
                    drawerState.targetValue == DrawerValue.Closed &&
                    !drawerState.isAnimationRunning
            }
        }
        CompositionLocalProvider(
            LocalClubSwitcher provides { coroutineScope.launch { drawerState.open() } },
            LocalNativeChromeVisible provides drawerClosed,
        ) {
            when (clubs) {
                is AsyncResult.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is AsyncResult.Error -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ErrorMessage(
                        message = "Couldn't load your clubs. Check your connection and try again.",
                        onRetry = memberViewModel::refresh,
                    )
                }
                is AsyncResult.Success -> ClubNavHost(
                    initialClubs = clubs.data,
                    lastClubSlug = lastClubSlug,
                    navController = navController,
                    memberViewModel = memberViewModel,
                    onOpenClub = ::openClub,
                    onSlugChanged = { selectClub(it) },
                    onLogout = onLogout,
                    isLoggingOut = isLoggingOut,
                )
            }
        }
    }
}

@Composable
private fun ClubNavHost(
    initialClubs: List<Club>,
    lastClubSlug: String?,
    navController: NavHostController,
    memberViewModel: MemberViewModel,
    onOpenClub: (String) -> Unit,
    onSlugChanged: (String) -> Unit,
    onLogout: () -> Unit,
    isLoggingOut: Boolean,
) {
    // Start destination is fixed at first composition; later changes navigate explicitly.
    val startDestination = remember {
        defaultClubSlug(initialClubs, lastClubSlug)?.let { Route.Club(it) } ?: Route.EmptyClubs
    }
    // Invite links opened from outside the app (held while signed out, consumed once signed in).
    val pendingLink by DeepLinks.pending.collectAsStateWithLifecycle()
    LaunchedEffect(pendingLink) {
        when (val link = pendingLink) {
            is DeepLink.JoinClub -> {
                DeepLinks.consume(link)
                navController.navigate(Route.JoinClub(link.token))
            }
            null -> Unit
        }
    }
    NavHost(navController = navController, startDestination = startDestination) {
        composable<Route.Club> {
            ClubScreenRoot()
        }

        composable<Route.EmptyClubs> {
            EmptyClubsScreen(
                onCreateClick = { navController.navigate(Route.CreateClub) },
                onJoinClick = { navController.navigate(Route.JoinClub()) },
                onProfileClick = { navController.navigate(Route.Profile) },
            )
        }

        composable<Route.CreateClub> {
            CreateClubScreenRoot(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToClub = { clubSlug ->
                    memberViewModel.refresh()
                    onOpenClub(clubSlug)
                }
            )
        }

        composable<Route.JoinClub> {
            JoinClubScreenRoot(
                onNavigateBack = { navController.popBackStack() },
                onJoined = { clubSlug ->
                    memberViewModel.refresh()
                    onOpenClub(clubSlug)
                }
            )
        }

        composable<Route.Profile> {
            ProfileScreenRoot(
                onNavigateBack = { navController.popBackStack() },
                onSignOut = onLogout,
                isSigningOut = isLoggingOut,
            )
        }

        composable<Route.ClubSettings> { entry ->
            val routeSlug = entry.toRoute<Route.ClubSettings>().clubSlug
            ClubSettingsScreenRoot(
                onNavigateBack = { navController.popBackStack() },
                // The shell's club-list effect moves to another club (or the empty state).
                onNavigateAfterLeave = { memberViewModel.refresh() },
                onClubUpdated = { newSlug ->
                    memberViewModel.refresh()
                    if (newSlug != routeSlug) {
                        // Routes are keyed by slug: rebuild the stack on the new one.
                        onSlugChanged(newSlug)
                        navController.navigateClearingStack(Route.Club(newSlug))
                        navController.navigate(Route.ClubSettings(newSlug))
                    }
                }
            )
        }
    }
}

@Composable
private fun EmptyClubsScreen(
    onCreateClick: () -> Unit,
    onJoinClick: () -> Unit,
    onProfileClick: () -> Unit,
) {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        IconButton(onClick = onProfileClick, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp)) {
            Icon(Icons.Default.Settings, contentDescription = "Account")
        }
        StatusMessage(
            icon = Icons.Default.Groups,
            title = "No clubs yet",
            message = "Start your own club and invite friends, or join one with an invite link.",
            primaryAction = "Create a club" to onCreateClick,
            secondaryAction = "Join with invite link" to onJoinClick,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun ClubSwitcherDrawer(
    clubs: AsyncResult<List<Club>>,
    me: Member?,
    currentClubSlug: String?,
    onClubClick: (String) -> Unit,
    onSettingsClick: (String) -> Unit,
    onCreateClubClick: () -> Unit,
    onJoinClubClick: () -> Unit,
    onProfileClick: () -> Unit,
    onLogout: () -> Unit,
    isLoggingOut: Boolean,
) {
    ModalDrawerSheet(
        drawerContainerColor = MaterialTheme.colorScheme.surface,
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
    ) {
        Column(Modifier.fillMaxHeight()) {
            // Account header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onProfileClick)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                MemberAvatar(me?.name.orEmpty(), me?.imageUrl, size = 44.dp)
                Column(Modifier.weight(1f)) {
                    Text(
                        me?.name ?: "Account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        me?.email ?: "Profile & settings",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Account settings",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(20.dp),
                )
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))

            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Text(
                    "YOUR CLUBS",
                    style = MaterialTheme.typography.labelMedium,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 8.dp),
                )
                when (clubs) {
                    is AsyncResult.Loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                    is AsyncResult.Error -> Text(
                        "Couldn't load clubs",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                    is AsyncResult.Success -> {
                        if (clubs.data.isEmpty()) {
                            Text(
                                "You're not in any clubs yet.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                            )
                        }
                        clubs.data.forEach { club ->
                            ClubRow(
                                club = club,
                                selected = club.slug == currentClubSlug,
                                onClick = { onClubClick(club.slug) },
                                onSettingsClick = { onSettingsClick(club.slug) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                DrawerAction(Icons.Default.Add, "Create a club", onCreateClubClick)
                DrawerAction(Icons.Default.Link, "Join with invite link", onJoinClubClick)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = !isLoggingOut, onClick = onLogout)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .navigationBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (isLoggingOut) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                }
                Text(
                    if (isLoggingOut) "Signing out…" else "Sign out",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun ClubRow(
    club: Club,
    selected: Boolean,
    onClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) accent.copy(alpha = 0.14f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(start = 10.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (selected) accent else MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                club.type.icon(),
                contentDescription = club.type.label(),
                tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Text(
                club.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                club.type.label(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
        }
        if (selected) {
            Icon(Icons.Default.Check, contentDescription = "Current club", tint = accent, modifier = Modifier.size(20.dp))
        }
        IconButton(onClick = onSettingsClick) {
            Icon(
                Icons.Default.Settings,
                contentDescription = "${club.name} settings",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun DrawerAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
        Text(label, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
    }
}
