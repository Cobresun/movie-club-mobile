package cobresun.movieclub.app.core.presentation

import coil3.PlatformContext
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

// Caches survives launches (unlike tmp, which Coil uses by default); iOS only purges it
// under storage pressure.
@OptIn(ExperimentalForeignApi::class)
actual fun imageDiskCacheDirectory(context: PlatformContext): Path {
    val caches = NSFileManager.defaultManager.URLForDirectory(
        directory = NSCachesDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    return (requireNotNull(caches?.path) + "/image_cache").toPath()
}
