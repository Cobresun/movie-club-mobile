package cobresun.movieclub.app.core.platform

import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UIKit.UINotificationFeedbackGenerator
import platform.UIKit.UINotificationFeedbackType
import platform.UIKit.UISelectionFeedbackGenerator

actual open class HapticFeedback() {
    private val light = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleLight)
    private val medium = UIImpactFeedbackGenerator(UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
    private val selection = UISelectionFeedbackGenerator()
    private val notification = UINotificationFeedbackGenerator()

    init {
        light.prepare()
        selection.prepare()
    }

    actual open fun performLightPulse() {
        light.impactOccurred()
        light.prepare()
    }

    actual open fun performTick() {
        selection.selectionChanged()
        selection.prepare()
    }

    actual open fun performConfirm() {
        medium.impactOccurred()
        medium.prepare()
    }

    actual open fun performSuccess() {
        notification.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeSuccess)
        notification.prepare()
    }

    actual open fun performWarning() {
        notification.notificationOccurred(UINotificationFeedbackType.UINotificationFeedbackTypeWarning)
        notification.prepare()
    }
}

// No-op implementation for previews
@androidx.compose.runtime.Composable
actual fun createNoOpHapticFeedback(): HapticFeedback {
    return object : HapticFeedback() {
        override fun performLightPulse() = Unit
        override fun performTick() = Unit
        override fun performConfirm() = Unit
        override fun performSuccess() = Unit
        override fun performWarning() = Unit
    }
}
