package cobresun.movieclub.app.core.platform

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle

actual open class HapticFeedback() {
    private val generator = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)

    init {
        generator.prepare()
    }

    actual open fun performLightPulse() {
        generator.impactOccurred()
        generator.prepare() // Prepare for next pulse
    }
}

// No-op implementation for previews
@androidx.compose.runtime.Composable
actual fun createNoOpHapticFeedback(): HapticFeedback {
    return object : HapticFeedback() {
        override fun performLightPulse() {
            // No-op for preview
        }
    }
}
