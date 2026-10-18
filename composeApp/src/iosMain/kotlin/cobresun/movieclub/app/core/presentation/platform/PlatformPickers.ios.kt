package cobresun.movieclub.app.core.presentation.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.create
import platform.Foundation.writeToURL
import platform.PhotosUI.PHPickerConfiguration
import platform.PhotosUI.PHPickerFilter
import platform.PhotosUI.PHPickerResult
import platform.PhotosUI.PHPickerViewController
import platform.PhotosUI.PHPickerViewControllerDelegateProtocol
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIGraphicsBeginImageContextWithOptions
import platform.UIKit.UIGraphicsEndImageContext
import platform.UIKit.UIGraphicsGetImageFromCurrentImageContext
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.posix.memcpy

private fun topViewController(): UIViewController? {
    var top = UIApplication.sharedApplication.keyWindow?.rootViewController
    while (top?.presentedViewController != null) top = top.presentedViewController
    return top
}

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberShareSheet(): (text: String, subject: String?) -> Unit = remember {
    { text, _ -> presentShareSheet(listOf(text)) }
}

/**
 * Shares the image as a named PNG file in the temporary directory, so the
 * share sheet previews it, offers "Save Image" and keeps the file name.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberImageShareSheet(): (png: ByteArray, fileName: String, title: String?) -> Unit = remember {
    { png, fileName, _ ->
        val url = NSURL.fileURLWithPath(NSTemporaryDirectory() + fileName)
        val written = png.toNSData().writeToURL(url, atomically = true)
        val item: Any = if (written) url else (UIImage(data = png.toNSData()) as Any)
        presentShareSheet(listOf(item))
    }
}

actual fun ImageBitmap.encodeToPng(): ByteArray =
    Image.makeFromBitmap(asSkiaBitmap()).encodeToData(EncodedImageFormat.PNG)?.bytes
        ?: error("The image could not be encoded as PNG")

actual fun ImageBitmap.encodeToJpeg(quality: Int): ByteArray =
    Image.makeFromBitmap(asSkiaBitmap()).encodeToData(EncodedImageFormat.JPEG, quality)?.bytes
        ?: error("The image could not be encoded as JPEG")

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private fun ByteArray.toNSData(): NSData = usePinned {
    NSData.create(bytes = it.addressOf(0), length = size.toULong())
}

@OptIn(ExperimentalForeignApi::class)
private fun presentShareSheet(items: List<Any>) {
    val controller = UIActivityViewController(activityItems = items, applicationActivities = null)
    val presenter = topViewController()
    // iPad presents share sheets as popovers, which need an anchor.
    presenter?.view?.let { view ->
        controller.popoverPresentationController?.sourceView = view
        view.bounds.useContents {
            controller.popoverPresentationController?.sourceRect =
                CGRectMake(size.width / 2, size.height / 2, 0.0, 0.0)
        }
    }
    presenter?.presentViewController(controller, animated = true, completion = null)
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    val result = ByteArray(size)
    if (size > 0) result.usePinned { memcpy(it.addressOf(0), bytes, length) }
    return result
}

@OptIn(ExperimentalForeignApi::class)
private fun downscaledJpeg(data: NSData): ByteArray? {
    val image = UIImage(data = data)
    val (w, h) = image.size.useContents { width to height }
    if (w <= 0.0 || h <= 0.0) return null
    val scale = minOf(1.0, MAX_AVATAR_DIMENSION / maxOf(w, h))
    UIGraphicsBeginImageContextWithOptions(CGSizeMake(w * scale, h * scale), true, 1.0)
    image.drawInRect(CGRectMake(0.0, 0.0, w * scale, h * scale))
    val resized = UIGraphicsGetImageFromCurrentImageContext()
    UIGraphicsEndImageContext()
    return UIImageJPEGRepresentation(resized ?: image, 0.85)?.toByteArray()
}

private class PickerDelegate(
    private val onPicked: (PickedImage?) -> Unit
) : NSObject(), PHPickerViewControllerDelegateProtocol {
    override fun picker(picker: PHPickerViewController, didFinishPicking: List<*>) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val result = didFinishPicking.firstOrNull() as? PHPickerResult
        if (result == null) {
            onPicked(null)
            return
        }
        result.itemProvider.loadDataRepresentationForTypeIdentifier("public.image") { data, _ ->
            val bytes = data?.let { downscaledJpeg(it) }
            dispatch_async(dispatch_get_main_queue()) {
                onPicked(bytes?.let { PickedImage(it) })
            }
        }
    }
}

@Composable
actual fun rememberImagePicker(onPicked: (PickedImage?) -> Unit): () -> Unit {
    val callback = rememberUpdatedState(onPicked)
    // Held for the composable's lifetime: the picker keeps only a weak delegate reference.
    val delegate = remember { PickerDelegate { callback.value(it) } }
    return remember {
        {
            val config = PHPickerConfiguration().apply {
                filter = PHPickerFilter.imagesFilter
                selectionLimit = 1
            }
            val picker = PHPickerViewController(configuration = config)
            picker.delegate = delegate
            topViewController()?.presentViewController(picker, animated = true, completion = null)
        }
    }
}
