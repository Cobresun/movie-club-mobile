package cobresun.movieclub.app.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import cobresun.movieclub.app.core.presentation.NavMotion
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclassesOfSealed

/** Back stacks are saved polymorphically; iOS has no reflection, so every [Route] is registered. */
@OptIn(ExperimentalSerializationApi::class)
private val RouteSavedState = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) { subclassesOfSealed<Route>() }
    }
}

/** A back stack of [Route]s that survives configuration changes and process death. */
@Composable
fun rememberRouteBackStack(start: Route): NavBackStack<NavKey> = rememberNavBackStack(RouteSavedState, start)

/** Pushes [route] unless it's already on top (Nav2's `launchSingleTop`). */
fun NavBackStack<NavKey>.push(route: Route) {
    if (lastOrNull() != route) add(route)
}

/** Pops the top route; the last one stays (a display can't be empty). */
fun NavBackStack<NavKey>.pop() {
    if (size > 1) removeAt(lastIndex)
}

/** Replaces the whole stack with [route]. */
fun NavBackStack<NavKey>.resetTo(route: Route) {
    add(route)
    while (size > 1) removeAt(0)
}

/**
 * A [NavDisplay] with the app's motion, a saveable-state holder and a ViewModelStore
 * per entry (so `koinViewModel()` inside an entry lives as long as that entry).
 */
@Composable
fun RouteDisplay(
    backStack: NavBackStack<NavKey>,
    modifier: Modifier = Modifier,
    entryProvider: (NavKey) -> NavEntry<NavKey>,
) {
    NavDisplay(
        backStack = backStack,
        modifier = modifier,
        onBack = { backStack.pop() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        transitionSpec = { NavMotion.transition() },
        popTransitionSpec = { NavMotion.transition() },
        predictivePopTransitionSpec = { NavMotion.transition() },
        entryProvider = entryProvider,
    )
}
