package cobresun.movieclub.app.core.platform

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

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

    actual open fun performLightPulse() = oneShot(10, 50)

    actual open fun performTick() = predefined(VibrationEffect.EFFECT_TICK) { oneShot(8, 40) }

    actual open fun performConfirm() = predefined(VibrationEffect.EFFECT_CLICK) { oneShot(20, 120) }

    actual open fun performSuccess() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 40, 60, 90), intArrayOf(0, 140, 0, 255), -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(longArrayOf(0, 40, 60, 90), -1)
        }
    }

    actual open fun performWarning() = predefined(VibrationEffect.EFFECT_DOUBLE_CLICK) { oneShot(30, 180) }

    private inline fun predefined(effectId: Int, fallback: () -> Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            vibrator.vibrate(VibrationEffect.createPredefined(effectId))
        } else {
            fallback()
        }
    }

    private fun oneShot(millis: Long, amplitude: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(millis, amplitude))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(millis)
        }
    }
}

// No-op implementation for previews
@androidx.compose.runtime.Composable
actual fun createNoOpHapticFeedback(): HapticFeedback {
    val context = androidx.compose.ui.platform.LocalContext.current
    return object : HapticFeedback(context) {
        override fun performLightPulse() = Unit
        override fun performTick() = Unit
        override fun performConfirm() = Unit
        override fun performSuccess() = Unit
        override fun performWarning() = Unit
    }
}
