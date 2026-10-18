package cobresun.movieclub.app.core.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.currentCompositionLocalContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.uikit.LocalUIViewController
import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.UIKit.UIColor
import platform.UIKit.UIModalPresentationPageSheet
import platform.UIKit.UIPresentationController
import platform.UIKit.UISheetPresentationControllerDelegateProtocol
import platform.UIKit.UISheetPresentationControllerDetent
import platform.UIKit.UIViewController
import platform.UIKit.sheetPresentationController
import platform.UIKit.transitionCoordinator
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

/** Room left above the content for the system grabber (5pt down, 5pt tall). */
private val grabberAreaHeight = 22.dp

@Stable
private class NativeSheetState(private val confirmHide: () -> Boolean) : AppSheetState {
    override var isVisible by mutableStateOf(true)
    var host: SheetHost? = null

    fun canHide(): Boolean = confirmHide()

    override suspend fun hide() {
        host?.let { host -> suspendCancellableCoroutine { cont -> host.dismiss { cont.resume(Unit) } } }
        isVisible = false
    }
}

@Composable
actual fun rememberAppSheetState(confirmHide: () -> Boolean): AppSheetState {
    val currentConfirmHide by rememberUpdatedState(confirmHide)
    return remember { NativeSheetState { currentConfirmHide() } }
}

@Composable
actual fun AppBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    sheetState: AppSheetState,
    containerColor: Color,
    sheetGesturesEnabled: Boolean,
    transparentHandle: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val state = sheetState as NativeSheetState
    val presenter = LocalUIViewController.current
    val locals = currentCompositionLocalContext
    val host = remember { SheetHost(state, locals, content) }
    SideEffect {
        host.locals = locals
        host.content = content
        host.modifier = modifier
        host.containerColor = containerColor
        host.transparentHandle = transparentHandle
        host.gesturesEnabled = sheetGesturesEnabled
        host.onDismissRequest = onDismissRequest
    }
    DisposableEffect(host) {
        state.host = host
        state.isVisible = true
        host.present(presenter)
        onDispose {
            // Leaving composition without hide() (the owner just dropped the sheet) still slides it away.
            host.dismiss {}
            if (state.host === host) state.host = null
        }
    }
}

/**
 * Presents a page sheet hosting the Compose content in its own `ComposeUIViewController`.
 * The sheet's one detent is the content's height, capped at the system's largest.
 */
private class SheetHost(
    private val state: NativeSheetState,
    locals: CompositionLocalContext,
    content: @Composable ColumnScope.() -> Unit,
) {
    var locals by mutableStateOf(locals)
    var content by mutableStateOf(content)
    var modifier by mutableStateOf<Modifier>(Modifier)
    var containerColor by mutableStateOf(Color.Unspecified)
    var transparentHandle by mutableStateOf(false)
    var gesturesEnabled = true
    var onDismissRequest: () -> Unit = {}

    /** Content height and the tallest the sheet may be, in points (= dp on iOS). */
    private var contentHeight = 0.0
    private var maxHeight = 0.0
    private var dismissed = false

    private val delegate = SheetDelegate(this)
    private val controller: UIViewController = ComposeUIViewController(
        configure = { onFocusBehavior = OnFocusBehavior.DoNothing },
    ) { SheetRoot() }.apply {
        // Light or dark from the window, which AppTheme sets to match Compose.
        modalPresentationStyle = UIModalPresentationPageSheet
        sheetPresentationController?.let { sheet ->
            sheet.detents = listOf(
                UISheetPresentationControllerDetent.customDetentWithIdentifier("content") { context ->
                    val max = context?.maximumDetentValue ?: 0.0
                    maxHeight = max
                    if (contentHeight > 0) minOf(contentHeight, max) else max
                },
            )
            sheet.prefersGrabberVisible = true
            sheet.prefersEdgeAttachedInCompactHeight = true
            sheet.delegate = delegate
        }
    }

    fun present(from: UIViewController) {
        // A sheet opened from a sheet stacks on top of it, as UIKit sheets do.
        var top = from
        while (true) {
            val next = top.presentedViewController ?: break
            if (next.isBeingDismissed()) break
            top = next
        }
        afterTransition(top) {
            if (!dismissed) top.presentViewController(controller, animated = true, completion = null)
        }
    }

    fun dismiss(completion: () -> Unit) {
        dismissed = true
        afterTransition(controller) {
            val presenting = controller.presentingViewController
            if (presenting == null || controller.isBeingDismissed()) {
                afterTransition(controller, completion)
            } else {
                presenting.dismissViewControllerAnimated(true) { completion() }
            }
        }
    }

    fun canDismiss(): Boolean = gesturesEnabled && state.canHide()

    fun onUserDismissed() {
        dismissed = true
        state.isVisible = false
        onDismissRequest()
    }

    fun onContentHeight(height: Double) {
        if (abs(height - contentHeight) < 0.5) return
        contentHeight = height
        dispatch_async(dispatch_get_main_queue()) {
            controller.sheetPresentationController?.let { sheet ->
                sheet.animateChanges { sheet.invalidateDetents() }
            }
        }
    }

    @Composable
    private fun SheetRoot() {
        // The sheet's own platform locals (focus, keyboard, interop container, view controller)
        // must win, but the app's (theme, Koin, view models) come from where the sheet was opened.
        val ownLocals = currentCompositionLocalContext
        CompositionLocalProvider(locals) {
            val viewModelOwner = LocalViewModelStoreOwner.current
            CompositionLocalProvider(ownLocals) {
                if (viewModelOwner != null) {
                    CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelOwner) { SheetBody() }
                } else {
                    SheetBody()
                }
            }
        }
    }

    @Composable
    private fun SheetBody() {
        Surface(Modifier.fillMaxSize(), color = containerColor) {
            Box(
                Modifier
                    .windowInsetsPadding(
                        WindowInsets.navigationBars.union(WindowInsets.ime).only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal),
                    )
                    .measureNaturalHeight(),
            ) {
                Column(modifier) {
                    if (!transparentHandle) Spacer(Modifier.height(grabberAreaHeight))
                    content()
                }
            }
        }
    }

    /**
     * Lays the content out as tall as it wants (up to the sheet's largest size) rather than
     * as tall as the sheet currently is, and reports that height for the detent.
     */
    private fun Modifier.measureNaturalHeight() = layout { measurable, constraints ->
        val cap = if (maxHeight > 0) (maxHeight * density).toInt() else constraints.maxHeight
        val placeable = measurable.measure(constraints.copy(minHeight = 0, maxHeight = maxOf(cap, constraints.minHeight)))
        onContentHeight(placeable.height / density.toDouble())
        layout(placeable.width, placeable.height.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            placeable.place(0, 0)
        }
    }
}

private class SheetDelegate(private val host: SheetHost) : NSObject(), UISheetPresentationControllerDelegateProtocol {
    override fun presentationControllerShouldDismiss(presentationController: UIPresentationController): Boolean =
        host.canDismiss()

    override fun presentationControllerDidDismiss(presentationController: UIPresentationController) {
        host.onUserDismissed()
    }
}

/** Runs [block] once [controller]'s running presentation transition (if any) has finished. */
private fun afterTransition(controller: UIViewController, block: () -> Unit) {
    val coordinator = controller.transitionCoordinator
    if (coordinator == null) block() else coordinator.animateAlongsideTransition(null) { block() }
}
