package cobresun.movieclub.app.core.data

import kotlinx.cinterop.ExperimentalForeignApi
import okio.Path
import okio.Path.Companion.toPath
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** Application Support: kept across launches and never purged by the OS. */
@OptIn(ExperimentalForeignApi::class)
fun offlineStoreDirectory(): Path {
    val support = NSFileManager.defaultManager.URLForDirectory(
        directory = NSApplicationSupportDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = true,
        error = null,
    )
    return (requireNotNull(support?.path) + "/" + OfflineStore.DIRECTORY_NAME).toPath()
}
