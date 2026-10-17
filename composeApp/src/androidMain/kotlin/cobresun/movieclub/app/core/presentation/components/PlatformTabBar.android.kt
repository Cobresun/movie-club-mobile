package cobresun.movieclub.app.core.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import cobresun.movieclub.app.core.presentation.AppMotion
import cobresun.movieclub.app.core.presentation.rememberReducedMotion

@Composable
actual fun PlatformTabBar(
    tabs: List<PlatformTab>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier,
) {
    // Set only on a change, so the icon bounces when a tab is picked but not on first show (web tab-pop).
    val firstKey = remember { mutableStateOf(selectedKey) }
    val pop = remember { Animatable(0f) }
    val reduced = rememberReducedMotion()
    LaunchedEffect(selectedKey) {
        if (selectedKey == firstKey.value || reduced) return@LaunchedEffect
        firstKey.value = ""
        pop.snapTo(0f)
        pop.animateTo(0f, keyframes {
            durationMillis = AppMotion.SLOW
            1f at (AppMotion.SLOW * 0.4f).toInt() using AppMotion.Emphasized
        })
    }
    NavigationBar(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        val colors = NavigationBarItemDefaults.colors(
            selectedIconColor = MaterialTheme.colorScheme.onSecondary,
            indicatorColor = MaterialTheme.colorScheme.secondary,
            selectedTextColor = MaterialTheme.colorScheme.secondary,
        )
        tabs.forEach { tab ->
            val selected = tab.key == selectedKey
            NavigationBarItem(
                selected = selected,
                onClick = { onSelect(tab.key) },
                icon = {
                    Icon(
                        if (selected) tab.selectedIcon else tab.icon,
                        contentDescription = null,
                        modifier = if (!selected) Modifier else Modifier.graphicsLayer {
                            val scale = 1f + 0.18f * pop.value
                            scaleX = scale
                            scaleY = scale
                            translationY = -2.dp.toPx() * pop.value
                        },
                    )
                },
                label = { Text(tab.title) },
                colors = colors,
            )
        }
    }
}

actual val tabBarOverlaysContent: Boolean = false
