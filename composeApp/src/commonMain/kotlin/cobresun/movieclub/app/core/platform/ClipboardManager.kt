package cobresun.movieclub.app.core.platform

expect class ClipboardManager {
    suspend fun copyToClipboard(text: String)
}
