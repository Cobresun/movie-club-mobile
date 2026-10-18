package cobresun.movieclub.app.core.platform

import platform.Foundation.NSLog

internal actual fun platformLog(level: AppLog.Level, tag: String, message: String, throwable: Throwable?) {
    val suffix = throwable?.let { "\n${it.stackTraceToString()}" }.orEmpty()
    // NSLog's format is varargs, which Kotlin/Native can't pass a String through safely:
    // escape '%' and send the whole line as the format.
    NSLog("${level.name.first()}/$tag: $message$suffix".replace("%", "%%"))
}
