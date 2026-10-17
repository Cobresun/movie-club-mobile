package cobresun.movieclub.app.member.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap

/** Returns a launcher that opens the native share sheet with the given text. */
@Composable
expect fun rememberShareSheet(): (text: String, subject: String?) -> Unit

/**
 * Returns a launcher that opens the native share sheet with a PNG image, offered
 * to other apps as a file called `fileName` (e.g. to save or post it).
 */
@Composable
expect fun rememberImageShareSheet(): (png: ByteArray, fileName: String, title: String?) -> Unit

/** Encodes a rendered image (e.g. a captured graphics layer) as PNG bytes. */
expect fun ImageBitmap.encodeToPng(): ByteArray

/** A picked image, re-encoded as JPEG and downscaled so uploads stay small. */
class PickedImage(val bytes: ByteArray, val mimeType: String = "image/jpeg")

/**
 * Returns a launcher that opens the system photo picker. [onPicked] receives
 * null when the user cancels or the image can't be read.
 */
@Composable
expect fun rememberImagePicker(onPicked: (PickedImage?) -> Unit): () -> Unit

internal const val MAX_AVATAR_DIMENSION = 1024.0
