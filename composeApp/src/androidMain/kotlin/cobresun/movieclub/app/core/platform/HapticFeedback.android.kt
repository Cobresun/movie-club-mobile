package cobresun.movieclub.app.core.platform

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.ui.platform.LocalContext

actual open class HapticFeedback(private val context: Context) {
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    actual open fun performLightPulse() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Modern API: Use VibrationEffect for precise control
            val effect = VibrationEffect.createOneShot(10, 50) // 10ms duration, medium-light intensity (50/255)
            vibrator.vibrate(effect)
        } else {
            // Legacy API: Simple vibration
            @Suppress("DEPRECATION")
            vibrator.vibrate(10)
        }
    }
}

// No-op implementation for previews
@androidx.compose.runtime.Composable
actual fun createNoOpHapticFeedback(): HapticFeedback {
    val context = androidx.compose.ui.platform.LocalContext.current
    return object : HapticFeedback(context) {
        override fun performLightPulse() {
            // No-op for preview
        }
    }
}
