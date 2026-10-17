package cobresun.movieclub.app.member.presentation.platform

import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

@Composable
actual fun rememberShareSheet(): (text: String, subject: String?) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { text, subject ->
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, text)
                if (subject != null) putExtra(Intent.EXTRA_SUBJECT, subject)
            }
            context.startActivity(
                Intent.createChooser(send, subject).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }
}

@Composable
actual fun rememberImageShareSheet(): (png: ByteArray, fileName: String, title: String?) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(context) {
        { png, fileName, title ->
            scope.launch {
                val uri = withContext(Dispatchers.IO) {
                    val dir = File(context.cacheDir, "shared").apply { mkdirs() }
                    // Only the latest shared image is kept around.
                    dir.listFiles()?.forEach { it.delete() }
                    val file = File(dir, fileName).apply { writeBytes(png) }
                    FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                }
                val send = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    if (title != null) putExtra(Intent.EXTRA_TITLE, title)
                    clipData = ClipData.newRawUri(fileName, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(send, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
    }
}

actual fun ImageBitmap.encodeToPng(): ByteArray =
    ByteArrayOutputStream().also { asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()

@Composable
actual fun rememberImagePicker(onPicked: (PickedImage?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) {
            onPicked(null)
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            val image = withContext(Dispatchers.IO) {
                runCatching {
                    val resolver = context.contentResolver
                    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
                    var sample = 1
                    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_AVATAR_DIMENSION) {
                        sample *= 2
                    }
                    val bitmap = resolver.openInputStream(uri)?.use {
                        BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
                    } ?: return@runCatching null
                    val out = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                    PickedImage(out.toByteArray())
                }.getOrNull()
            }
            onPicked(image)
        }
    }
    return remember(launcher) {
        { launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    }
}
