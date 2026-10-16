package cobresun.movieclub.app.member.presentation.platform

import androidx.compose.runtime.Composable

/** Returns a launcher that opens the native share sheet with the given text. */
@Composable
expect fun rememberShareSheet(): (text: String, subject: String?) -> Unit

/** A picked image, re-encoded as JPEG and downscaled so uploads stay small. */
class PickedImage(val bytes: ByteArray, val mimeType: String = "image/jpeg")

/**
 * Returns a launcher that opens the system photo picker. [onPicked] receives
 * null when the user cancels or the image can't be read.
 */
@Composable
expect fun rememberImagePicker(onPicked: (PickedImage?) -> Unit): () -> Unit

internal const val MAX_AVATAR_DIMENSION = 1024.0
