package cobresun.movieclub.app.core.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIColor
import platform.UIKit.UIImage
import platform.UIKit.UITabBar
import platform.UIKit.UITabBarDelegateProtocol
import platform.UIKit.UITabBarItem
import platform.UIKit.UIUserInterfaceStyle
import platform.darwin.NSObject

/**
 * A native `UITabBar` placed as an overlay above the Compose layer. Being a real
 * UIKit view, iOS 26 renders it with Liquid Glass; older iOS versions get the
 * standard translucent material bar.
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalComposeUiApi::class)
@Composable
actual fun PlatformTabBar(
    tabs: List<PlatformTab>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    modifier: Modifier,
) {
    val currentOnSelect = rememberUpdatedState(onSelect)
    val delegate = remember { TabBarDelegate { key -> currentOnSelect.value(key) } }
    val tint = MaterialTheme.colorScheme.secondary.toUIColor()
    val barHeight = remember {
        UITabBar().sizeThatFits(CGSizeMake(390.0, 0.0)).useContents { height }.dp
    }
    val bottomInset = with(LocalDensity.current) { WindowInsets.navigationBars.getBottom(this).toDp() }

    Box(modifier.fillMaxWidth().height(barHeight + bottomInset)) {
        if (LocalNativeChromeVisible.current) {
            UIKitView(
                factory = {
                    UITabBar().apply {
                        this.delegate = delegate
                        overrideUserInterfaceStyle = UIUserInterfaceStyle.UIUserInterfaceStyleDark
                    }
                },
                update = { bar ->
                    bar.tintColor = tint
                    val current = bar.items?.map { (it as UITabBarItem).title }
                    if (current != tabs.map { it.title }) {
                        bar.setItems(
                            tabs.mapIndexed { index, tab ->
                                UITabBarItem(
                                    title = tab.title,
                                    image = UIImage.systemImageNamed(tab.iosSymbol),
                                    selectedImage = UIImage.systemImageNamed(tab.iosSelectedSymbol),
                                ).apply { tag = index.toLong() }
                            },
                            animated = false,
                        )
                    }
                    delegate.keys = tabs.map { it.key }
                    val index = tabs.indexOfFirst { it.key == selectedKey }
                    bar.selectedItem = bar.items?.getOrNull(index) as? UITabBarItem
                },
                modifier = Modifier.fillMaxWidth().height(barHeight + bottomInset),
                properties = UIKitInteropProperties(
                    interactionMode = UIKitInteropInteractionMode.NonCooperative,
                    isNativeAccessibilityEnabled = true,
                    placedAsOverlay = true,
                ),
            )
        }
    }
}

actual val tabBarOverlaysContent: Boolean = true

private class TabBarDelegate(
    private val onSelect: (String) -> Unit,
) : NSObject(), UITabBarDelegateProtocol {
    var keys: List<String> = emptyList()

    override fun tabBar(tabBar: UITabBar, didSelectItem: UITabBarItem) {
        keys.getOrNull(didSelectItem.tag.toInt())?.let(onSelect)
    }
}

private fun Color.toUIColor(): UIColor = UIColor.colorWithRed(
    red = red.toDouble(),
    green = green.toDouble(),
    blue = blue.toDouble(),
    alpha = alpha.toDouble(),
)
