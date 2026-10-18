package cobresun.movieclub.app.core.platform

import platform.Foundation.NSLog

internal actual fun platformLog(level: AppLog.Level, tag: String, message: String, throwable: Throwable?) {
    val suffix = throwable?.let { "\n${it.stackTraceToString()}" }.orEmpty()
    // %@ keeps any '%' in the message from being read as a format specifier.
    NSLog("%@", "${level.name.first()}/$tag: $message$suffix")
}
