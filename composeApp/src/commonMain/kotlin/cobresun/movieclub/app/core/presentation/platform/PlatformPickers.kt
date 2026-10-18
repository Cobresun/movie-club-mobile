package cobresun.movieclub.app.core.presentation.platform

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

/** Encodes an image as JPEG bytes ([quality] 0–100), e.g. a cropped avatar. */
expect fun ImageBitmap.encodeToJpeg(quality: Int): ByteArray

/** A picked image, upright, re-encoded as JPEG and downscaled to [MAX_AVATAR_DIMENSION]. */
class PickedImage(val bytes: ByteArray, val mimeType: String = "image/jpeg")

/**
 * Returns a launcher that opens the system photo picker. [onPicked] receives
 * null when the user cancels or the image can't be read.
 */
@Composable
expect fun rememberImagePicker(onPicked: (PickedImage?) -> Unit): () -> Unit

/**
 * Longest side a picked photo is scaled to (web `WORKING_MAX_SIDE`): cheap to
 * redraw while cropping, still full resolution for the avatar at maximum zoom.
 */
internal const val MAX_AVATAR_DIMENSION = 2048.0
