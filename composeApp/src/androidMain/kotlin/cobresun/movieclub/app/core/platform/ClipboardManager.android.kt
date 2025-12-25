package cobresun.movieclub.app.core.platform

import android.content.ClipData
import android.content.Context

actual class ClipboardManager(private val context: Context) {
    actual suspend fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val clip = ClipData.newPlainText("invite_link", text)
        clipboard.setPrimaryClip(clip)
    }
}
