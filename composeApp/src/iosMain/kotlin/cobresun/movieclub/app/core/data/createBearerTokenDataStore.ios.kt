package cobresun.movieclub.app.core.data

import androidx.datastore.core.DataStore
import cobresun.movieclub.app.proto.BearerTokenData
import kotlinx.cinterop.ExperimentalForeignApi
import okio.FileSystem
import okio.Path.Companion.toPath
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
fun createBearerTokenDataStore(): DataStore<BearerTokenData> {
    val producePath = {
        val directory = NSFileManager.defaultManager.URLForDirectory(
            directory = NSDocumentDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = false,
            error = null
        )

        requireNotNull(directory).path + "/$DATA_STORE_FILE_NAME"
    }

    return createBearerTokenDataStore(
        fileSystem = FileSystem.SYSTEM,
        producePath = { producePath().toPath() }
    )
}
