package cobresun.movieclub.app.core.presentation

import coil3.PlatformContext
import okio.Path
import okio.Path.Companion.toOkioPath

actual fun imageDiskCacheDirectory(context: PlatformContext): Path =
    context.cacheDir.resolve("image_cache").toOkioPath()
