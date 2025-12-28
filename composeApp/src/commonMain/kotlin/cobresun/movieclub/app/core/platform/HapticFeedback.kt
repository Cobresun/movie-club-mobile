package cobresun.movieclub.app.core.platform

/**
 * Platform-agnostic haptic feedback interface.
 * Provides light pulse feedback for shuffle animation iterations.
 */
expect open class HapticFeedback {
    /**
     * Performs a light haptic pulse.
     * Called on each shuffle iteration for slot-machine feel.
     */
    open fun performLightPulse()
}

/**
 * Creates a no-op HapticFeedback instance for previews and testing.
 */
@androidx.compose.runtime.Composable
expect fun createNoOpHapticFeedback(): HapticFeedback
