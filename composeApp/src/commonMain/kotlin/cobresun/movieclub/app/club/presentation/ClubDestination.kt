package cobresun.movieclub.app.club.presentation

import cobresun.movieclub.app.stats.presentation.SharedStatisticsScreenRoot
import cobresun.movieclub.app.lists.presentation.SharedListScreenRoot
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
import cobresun.movieclub.app.core.presentation.OnReconnect
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
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import cobresun.movieclub.app.app.Route
import cobresun.movieclub.app.app.RouteDisplay
import cobresun.movieclub.app.app.pop
import cobresun.movieclub.app.app.push
import cobresun.movieclub.app.app.rememberRouteBackStack
import cobresun.movieclub.app.app.resetTo
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
import cobresun.movieclub.app.member.presentation.MemberViewModel
import cobresun.movieclub.app.member.presentation.ProfileScreenRoot
import cobresun.movieclub.app.stats.presentation.WrappedScreenRoot
import cobresun.movieclub.app.core.presentation.components.ErrorMessage
import cobresun.movieclub.app.core.presentation.components.MemberAvatar
import cobresun.movieclub.app.core.presentation.components.StatusMessage
import cobresun.movieclub.app.core.presentation.components.icon
import cobresun.movieclub.app.core.presentation.components.label
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

@Composable
fun ClubsScreen(
    clubs: AsyncResult<List<Club>>,
    me: Member?,
    lastClubSlug: String?,
    onLogout: () -> Unit = {},
    memberViewModel: MemberViewModel,
    isLoggingOut: Boolean = false,
) {
    // Back online: clubs, profile and members shown meanwhile came from storage.
    OnReconnect { memberViewModel.refresh() }

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
            // Clubs stay loaded once they are (refreshes keep them on screen), so the shell isn't torn down.
            is AsyncResult.Success -> ClubShell(
                clubs = clubs.data,
                me = me,
                lastClubSlug = lastClubSlug,
                onLogout = onLogout,
                memberViewModel = memberViewModel,
                isLoggingOut = isLoggingOut,
            )
        }
    }
}

/** The signed-in shell: the club back stack, its header locals, and the switcher and account sheets. */
@Composable
private fun ClubShell(
    clubs: List<Club>,
    me: Member?,
    lastClubSlug: String?,
    onLogout: () -> Unit,
    memberViewModel: MemberViewModel,
    isLoggingOut: Boolean,
) {
    // Starts on the default club; later changes navigate explicitly.
    val backStack = rememberRouteBackStack(
        defaultClubSlug(clubs, lastClubSlug)?.let { Route.Club(it) } ?: Route.EmptyClubs
    )
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
        backStack.resetTo(Route.Club(slug, tab?.name))
    }

    // Keep the selection valid when the club list changes (left, removed, renamed slug).
    LaunchedEffect(clubs) {
        val selected = currentClubSlug
        when {
            clubs.isEmpty() -> if (selected != null) {
                currentClubSlug = null
                backStack.resetTo(Route.EmptyClubs)
            }
            // Matches the back stack's start route.
            selected == null -> selectClub(defaultClubSlug(clubs, lastClubSlug))
            clubs.none { it.slug == selected } -> openClub(clubs.first().slug)
        }
    }

    val currentClub = clubs.firstOrNull { it.slug == currentClubSlug }
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
        ClubNavHost(
            initialClubs = clubs,
            backStack = backStack,
            memberViewModel = memberViewModel,
            onOpenClub = { openClub(it) },
            onSlugChanged = { selectClub(it) },
            onLogout = onLogout,
            isLoggingOut = isLoggingOut,
        )
    }

    if (showSwitcher) {
        ClubSwitcherSheet(
            clubs = clubs,
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
            onSettings = { club -> backStack.push(Route.ClubSettings(club.slug)) },
            onCreateClub = { backStack.push(Route.CreateClub) },
        )
    }
    if (showAccount) {
        AccountSheet(
            me = me,
            isLoggingOut = isLoggingOut,
            onDismiss = { showAccount = false },
            onProfile = { backStack.push(Route.Profile) },
            onLogout = onLogout,
        )
    }
}

@Composable
private fun ClubNavHost(
    initialClubs: List<Club>,
    backStack: NavBackStack<NavKey>,
    memberViewModel: MemberViewModel,
    onOpenClub: (String) -> Unit,
    onSlugChanged: (String) -> Unit,
    onLogout: () -> Unit,
    isLoggingOut: Boolean,
) {
    // A tab for the club screen to switch to when it shows again (Wrapped's "go to reviews").
    var tabRequest by remember { mutableStateOf<ClubTab?>(null) }
    // Invite links opened from outside the app (held while signed out, consumed once signed in).
    val pendingLink by DeepLinks.pending.collectAsStateWithLifecycle()
    LaunchedEffect(pendingLink) {
        when (val link = pendingLink) {
            is DeepLink.JoinClub -> {
                DeepLinks.consume(link)
                backStack.push(Route.JoinClub(link.token))
            }
            // Club and shared-review links: bring that club's screen up; ClubScreenRoot
            // consumes the link once it shows (tab + review details).
            is DeepLink.Club -> {
                val memberSlugs = (memberViewModel.state.value.clubs as? AsyncResult.Success)?.data?.map { it.slug }
                    ?: initialClubs.map { it.slug }
                val onClub = (backStack.lastOrNull() as? Route.Club)?.clubSlug == link.slug
                when {
                    link.slug !in memberSlugs -> {
                        DeepLinks.consume(link)
                        // Not in that club: shared reviews, lists and statistics open as their public pages.
                        when {
                            link.reviewWorkId != null -> backStack.push(Route.SharedReview(link.slug, link.reviewWorkId))
                            link.listId != null -> backStack.push(Route.SharedList(link.slug, link.listId))
                            link.section == ClubSection.STATISTICS -> backStack.push(Route.SharedStatistics(link.slug))
                            else -> backStack.push(Route.ClubNotFound)
                        }
                    }
                    link.section == ClubSection.SETTINGS -> {
                        DeepLinks.consume(link)
                        if (!onClub) onOpenClub(link.slug)
                        backStack.push(Route.ClubSettings(link.slug))
                    }
                    link.section == ClubSection.WRAPPED -> {
                        DeepLinks.consume(link)
                        if (!onClub) onOpenClub(link.slug)
                        backStack.push(Route.Wrapped(link.slug, link.wrappedYear))
                    }
                    !onClub -> onOpenClub(link.slug)
                }
            }
            null -> Unit
        }
    }
    RouteDisplay(
        backStack = backStack,
        entryProvider = entryProvider {
            entry<Route.Club> { route ->
                ClubScreenRoot(
                    clubSlug = route.clubSlug,
                    initialTab = ClubTab.entries.firstOrNull { it.name == route.tab },
                    tabRequest = tabRequest,
                    onTabRequestHandled = { tabRequest = null },
                    onOpenWrapped = { year -> backStack.push(Route.Wrapped(route.clubSlug, year)) },
                )
            }

            entry<Route.Wrapped> { route ->
                WrappedScreenRoot(
                    clubSlug = route.clubSlug,
                    year = route.year,
                    onClose = { backStack.pop() },
                    onGoToReviews = {
                        tabRequest = ClubTab.REVIEWS
                        backStack.pop()
                    },
                )
            }

            // Web routes club-less members to the new-club view, framed as onboarding.
            entry<Route.EmptyClubs> {
                CreateClubScreenRoot(
                    isFirstClub = true,
                    onNavigateBack = null,
                    onNavigateToClub = { clubSlug, name, type ->
                        memberViewModel.refresh()
                        onOpenClub(clubSlug)
                        backStack.push(Route.ClubInvite(clubSlug, name, type.value))
                    },
                    onJoin = { token -> backStack.push(Route.JoinClub(token)) },
                    topEnd = { AccountButton() },
                )
            }

            entry<Route.CreateClub> {
                CreateClubScreenRoot(
                    onNavigateBack = { backStack.pop() },
                    onJoin = { token -> backStack.push(Route.JoinClub(token)) },
                    onNavigateToClub = { clubSlug, name, type ->
                        memberViewModel.refresh()
                        onOpenClub(clubSlug)
                        // The club sits underneath, so "Go to the club" (or back) lands there.
                        backStack.push(Route.ClubInvite(clubSlug, name, type.value))
                    }
                )
            }

            entry<Route.ClubInvite> { route ->
                ClubInviteScreenRoot(
                    clubSlug = route.clubSlug,
                    clubName = route.clubName,
                    clubType = route.clubType,
                    onGoToClub = { backStack.pop() },
                )
            }

            entry<Route.JoinClub> { route ->
                val memberState by memberViewModel.state.collectAsStateWithLifecycle()
                JoinClubScreenRoot(
                    token = route.token,
                    memberClubSlugs = (memberState.clubs as? AsyncResult.Success)?.data.orEmpty().map { it.slug }.toSet(),
                    onNavigateBack = { backStack.pop() },
                    onJoined = { clubSlug ->
                        memberViewModel.refresh()
                        onOpenClub(clubSlug)
                    }
                )
            }

            entry<Route.ClubNotFound> {
                ClubNotFoundScreen(onBrowseClubs = { backStack.pop() })
            }
            entry<Route.SharedList> { route ->
                SharedListScreenRoot(clubSlug = route.clubSlug, listId = route.listId, onBack = { backStack.pop() })
            }
            entry<Route.SharedStatistics> { route ->
                SharedStatisticsScreenRoot(clubSlug = route.clubSlug, onBack = { backStack.pop() })
            }
            entry<Route.SharedReview> { route ->
                SharedReviewScreenRoot(clubSlug = route.clubSlug, workId = route.workId, onBack = { backStack.pop() })
            }

            entry<Route.Profile> {
                ProfileScreenRoot(
                    onNavigateBack = { backStack.pop() },
                    onSignOut = onLogout,
                    isSigningOut = isLoggingOut,
                )
            }

            entry<Route.ClubSettings> { route ->
                val routeSlug = route.clubSlug
                // Members may have been removed: refresh the header's count on the way out.
                DisposableEffect(routeSlug) { onDispose { memberViewModel.loadMembers(routeSlug) } }
                ClubSettingsScreenRoot(
                    clubSlug = routeSlug,
                    onNavigateBack = { backStack.pop() },
                    // The shell's club-list effect moves to another club (or the empty state).
                    onNavigateAfterLeave = { memberViewModel.refresh() },
                    onClubUpdated = { newSlug ->
                        memberViewModel.refresh()
                        if (newSlug != routeSlug) {
                            // Routes are keyed by slug: rebuild the stack on the new one.
                            onSlugChanged(newSlug)
                            backStack.resetTo(Route.Club(newSlug))
                            backStack.push(Route.ClubSettings(newSlug))
                        }
                    }
                )
            }
        },
    )
}
