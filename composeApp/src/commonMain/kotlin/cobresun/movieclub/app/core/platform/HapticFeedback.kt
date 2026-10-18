package cobresun.movieclub.app.core.platform

import androidx.compose.runtime.Composable
/**
 * Platform-agnostic haptic feedback.
 *
 * Each method maps to the closest native idiom: UIKit feedback generators on iOS and
 * predefined vibration effects on Android.
 */
expect open class HapticFeedback {
    /** A light pulse, used for each shuffle iteration. */
    open fun performLightPulse()

    /** A crisp selection tick, used for each detent crossed on a dial or picker. */
    open fun performTick()

    /** A firm tap confirming a committed action, like saving a score or dropping a dragged item. */
    open fun performConfirm()

    /** A celebratory success pattern, like a revealed winner or a landed pick. */
    open fun performSuccess()

    /** A warning buzz for rejected input. */
    open fun performWarning()
}

/**
 * Creates a no-op HapticFeedback instance for previews and testing.
 */
@Composable
expect fun createNoOpHapticFeedback(): HapticFeedback
