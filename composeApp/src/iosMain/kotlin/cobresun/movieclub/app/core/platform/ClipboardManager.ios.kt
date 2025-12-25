package cobresun.movieclub.app.core.platform

import platform.UIKit.UIPasteboard

actual class ClipboardManager {
    actual suspend fun copyToClipboard(text: String) {
        UIPasteboard.generalPasteboard.string = text
    }
}
