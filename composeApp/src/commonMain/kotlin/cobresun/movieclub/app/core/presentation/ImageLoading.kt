package cobresun.movieclub.app.core.presentation

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.compose.setSingletonImageLoaderFactory
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import okio.Path

/**
 * The app's image loader: posters and avatars seen once stay in a persistent disk cache,
 * so they still show offline. Coil reads the disk cache before the network.
 */
@Composable
fun ConfigureImageLoader() {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .memoryCache { MemoryCache.Builder().maxSizePercent(context, 0.20).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(imageDiskCacheDirectory(context))
                    .maxSizeBytes(IMAGE_DISK_CACHE_BYTES)
                    .build()
            }
            .build()
    }
}

private const val IMAGE_DISK_CACHE_BYTES = 300L * 1024 * 1024

/** A cache directory the OS keeps across launches (not a temp directory). */
expect fun imageDiskCacheDirectory(context: PlatformContext): Path
