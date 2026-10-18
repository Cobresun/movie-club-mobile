package cobresun.movieclub.app.core.platform

import android.util.Log

internal actual fun platformLog(level: AppLog.Level, tag: String, message: String, throwable: Throwable?) {
    when (level) {
        AppLog.Level.DEBUG -> Log.d(tag, message, throwable)
        AppLog.Level.INFO -> Log.i(tag, message, throwable)
        AppLog.Level.WARN -> Log.w(tag, message, throwable)
        AppLog.Level.ERROR -> Log.e(tag, message, throwable)
    }
}
