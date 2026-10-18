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
import cobresun.movieclub.app.core.presentation.components.AccountButton
import cobresun.movieclub.app.core.platform.HapticFeedback
import cobresun.movieclub.app.core.domain.ClubType
import org.koin.compose.koinInject
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import cobresun.movieclub.app.core.domain.defaultClubSlug
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.compositionLocalOf
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
import cobresun.movieclub.app.app.ClubSection
import cobresun.movieclub.app.reviews.presentation.SharedReviewScreenRoot
import androidx.navigation.NavDestination.Companion.hasRoute
import cobresun.movieclub.app.member.presentation.MemberViewModel
import cobresun.movieclub.app.member.presentation.ProfileScreenRoot
import cobresun.movieclub.app.stats.presentation.WrappedScreenRoot
import cobresun.movieclub.app.member.presentation.components.ErrorMessage
import cobresun.movieclub.app.member.presentation.components.MemberAvatar
import cobresun.movieclub.app.member.presentation.components.StatusMessage
import cobresun.movieclub.app.member.presentation.components.icon
import cobresun.movieclub.app.member.presentation.components.label
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

/**
 * Opens the club switcher sheet. Screens inside the club shell (e.g. the club
 * name above each tab's title) can call `LocalClubSwitcher.current()`.
 */
val LocalClubSwitcher = staticCompositionLocalOf<() -> Unit> { {} }

/** Opens the account sheet (avatar, profile, log out). */
val LocalAccountMenu = staticCompositionLocalOf<() -> Unit> { {} }

/** The signed-in member, for the account button. */
val LocalCurrentMember = compositionLocalOf<Member?> { null }

/** Name of the current club, known before each tab loads its own context. */
val LocalCurrentClubName = compositionLocalOf { "" }

/** "Movie club · 6 members" for the current club (web `clubMetaLine`). */
val LocalCurrentClubMeta = compositionLocalOf { "" }

/** Members of the current club, for the header's avatar stack. */
val LocalCurrentClubMembers = compositionLocalOf<List<Member>> { emptyList() }

/** Club screens report the tab they show, so switching clubs can keep the section. */
internal val LocalClubTabShown = staticCompositionLocalOf<(ClubTab) -> Unit> { {} }

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
    val haptics = koinInject<HapticFeedback>()

    var currentClubSlug by rememberSaveable { mutableStateOf<String?>(null) }
    var currentTab by rememberSaveable { mutableStateOf<ClubTab?>(null) }
    var showSwitcher by rememberSaveable { mutableStateOf(false) }
    var showAccount by rememberSaveable { mutableStateOf(false) }

    fun selectClub(slug: String?) {
        currentClubSlug = slug
        if (slug != null) memberViewModel.rememberClub(slug)
    }

    /** [tab] null reopens the tab last used in that club. */
    fun openClub(slug: String, tab: ClubTab? = null) {
        selectClub(slug)
        navController.navigateClearingStack(Route.Club(slug, tab?.name))
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

    val clubList = (clubs as? AsyncResult.Success)?.data.orEmpty()
    val currentClub = clubList.firstOrNull { it.slug == currentClubSlug }
    val memberState by memberViewModel.state.collectAsStateWithLifecycle()
    val currentMembers = currentClubSlug?.let { memberState.members[it] }
    val currentMeta = clubMetaLine(currentClub?.type, currentMembers?.size)
    LaunchedEffect(currentClubSlug) { currentClubSlug?.let(memberViewModel::loadMembers) }

    CompositionLocalProvider(
        LocalClubSwitcher provides { showSwitcher = true },
        LocalAccountMenu provides { showAccount = true },
        LocalCurrentMember provides me,
        LocalCurrentClubName provides currentClub?.name.orEmpty(),
        LocalCurrentClubMeta provides currentMeta,
        LocalCurrentClubMembers provides currentMembers.orEmpty(),
        LocalClubTabShown provides { currentTab = it },
    ) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface)) {
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
                    onOpenClub = { openClub(it) },
                    onSlugChanged = { selectClub(it) },
                    onLogout = onLogout,
                    isLoggingOut = isLoggingOut,
                )
            }
        }
    }

    if (showSwitcher) {
        ClubSwitcherSheet(
            clubs = clubList,
            current = currentClub,
            meta = currentMeta,
            onDismiss = { showSwitcher = false },
            onSelectClub = { club ->
                haptics.performTick()
                // Switching keeps the section you were reading (web ClubSwitcher); Awards
                // has no equivalent in a book club, so that lands on Reviews.
                val tab = currentTab?.takeUnless { it == ClubTab.AWARDS && club.type != ClubType.MOVIE }
                    ?: ClubTab.REVIEWS
                openClub(club.slug, tab)
            },
            onSettings = { club -> navController.navigate(Route.ClubSettings(club.slug)) },
            onCreateClub = { navController.navigate(Route.CreateClub) },
        )
    }
    if (showAccount) {
        AccountSheet(
            me = me,
            isLoggingOut = isLoggingOut,
            onDismiss = { showAccount = false },
            onProfile = { navController.navigate(Route.Profile) },
            onLogout = onLogout,
        )
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
            // Club and shared-review links: bring that club's screen up; ClubScreenRoot
            // consumes the link once it shows (tab + review details).
            is DeepLink.Club -> {
                val memberSlugs = (memberViewModel.state.value.clubs as? AsyncResult.Success)?.data?.map { it.slug }
                    ?: initialClubs.map { it.slug }
                val entry = navController.currentBackStackEntry
                val onClub = entry?.destination?.hasRoute<Route.Club>() == true &&
                    entry.toRoute<Route.Club>().clubSlug == link.slug
                when {
                    link.slug !in memberSlugs -> {
                        DeepLinks.consume(link)
                        // Not in that club: a shared review still opens as its public page.
                        link.reviewWorkId?.let { navController.navigate(Route.SharedReview(link.slug, it)) }
                    }
                    link.section == ClubSection.SETTINGS -> {
                        DeepLinks.consume(link)
                        if (!onClub) onOpenClub(link.slug)
                        navController.navigate(Route.ClubSettings(link.slug))
                    }
                    link.section == ClubSection.WRAPPED -> {
                        DeepLinks.consume(link)
                        if (!onClub) onOpenClub(link.slug)
                        navController.navigate(Route.Wrapped(link.slug, link.wrappedYear))
                    }
                    !onClub -> onOpenClub(link.slug)
                }
            }
            null -> Unit
        }
    }
    NavHost(navController = navController, startDestination = startDestination) {
        composable<Route.Club> { entry ->
            val slug = entry.toRoute<Route.Club>().clubSlug
            ClubScreenRoot(onOpenWrapped = { year -> navController.navigate(Route.Wrapped(slug, year)) })
        }

        composable<Route.Wrapped> {
            WrappedScreenRoot(
                onClose = { navController.popBackStack() },
                onGoToReviews = {
                    navController.previousBackStackEntry?.savedStateHandle?.set(ClubViewModel.KEY_TAB, ClubTab.REVIEWS)
                    navController.popBackStack()
                },
            )
        }

        // Web routes club-less members to the new-club view, framed as onboarding.
        composable<Route.EmptyClubs> {
            CreateClubScreenRoot(
                isFirstClub = true,
                onNavigateBack = null,
                onNavigateToClub = { clubSlug, name, type ->
                    memberViewModel.refresh()
                    onOpenClub(clubSlug)
                    navController.navigate(Route.ClubInvite(clubSlug, name, type.value))
                },
                onJoin = { token -> navController.navigate(Route.JoinClub(token)) },
                topEnd = { AccountButton() },
            )
        }

        composable<Route.CreateClub> {
            CreateClubScreenRoot(
                onNavigateBack = { navController.popBackStack() },
                onJoin = { token -> navController.navigate(Route.JoinClub(token)) },
                onNavigateToClub = { clubSlug, name, type ->
                    memberViewModel.refresh()
                    onOpenClub(clubSlug)
                    // The club sits underneath, so "Go to the club" (or back) lands there.
                    navController.navigate(Route.ClubInvite(clubSlug, name, type.value))
                }
            )
        }

        composable<Route.ClubInvite> {
            ClubInviteScreenRoot(onGoToClub = { navController.popBackStack() })
        }

        composable<Route.JoinClub> {
            val memberState by memberViewModel.state.collectAsStateWithLifecycle()
            JoinClubScreenRoot(
                memberClubSlugs = (memberState.clubs as? AsyncResult.Success)?.data.orEmpty().map { it.slug }.toSet(),
                onNavigateBack = { navController.popBackStack() },
                onJoined = { clubSlug ->
                    memberViewModel.refresh()
                    onOpenClub(clubSlug)
                }
            )
        }

        composable<Route.SharedReview> {
            SharedReviewScreenRoot(onBack = { navController.popBackStack() })
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
            // Members may have been removed: refresh the header's count on the way out.
            DisposableEffect(routeSlug) { onDispose { memberViewModel.loadMembers(routeSlug) } }
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
