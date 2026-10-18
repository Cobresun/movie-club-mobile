package cobresun.movieclub.app.core.platform

/**
 * The app's one logging entry point (instead of `println`): Logcat on Android, NSLog on iOS.
 *
 * Debug lines (like the HTTP request log) are dropped unless [debugEnabled], which each
 * platform's entry point sets from its build type. Warnings and errors also go to every
 * [Sink], the hook for error monitoring (a crash reporter adds one at startup).
 */
object AppLog {
    enum class Level { DEBUG, INFO, WARN, ERROR }

    fun interface Sink {
        fun log(level: Level, tag: String, message: String, throwable: Throwable?)
    }

    var debugEnabled: Boolean = false

    private var sinks: List<Sink> = emptyList()

    fun addSink(sink: Sink) {
        sinks = sinks + sink
    }

    fun d(tag: String, message: () -> String) {
        if (debugEnabled) platformLog(Level.DEBUG, tag, message(), null)
    }

    fun i(tag: String, message: String) = platformLog(Level.INFO, tag, message, null)

    fun w(tag: String, message: String, throwable: Throwable? = null) = report(Level.WARN, tag, message, throwable)

    fun e(tag: String, message: String, throwable: Throwable? = null) = report(Level.ERROR, tag, message, throwable)

    private fun report(level: Level, tag: String, message: String, throwable: Throwable?) {
        platformLog(level, tag, message, throwable)
        sinks.forEach { it.log(level, tag, message, throwable) }
    }
}

internal expect fun platformLog(level: AppLog.Level, tag: String, message: String, throwable: Throwable?)
