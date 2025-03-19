package cobresun.movieclub.app.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import cobresun.movieclub.app.proto.BearerTokenData
import okio.FileSystem
import okio.Path.Companion.toPath

fun createBearerTokenDataStore(context: Context): DataStore<BearerTokenData> {
    return createBearerTokenDataStore(
        fileSystem = FileSystem.SYSTEM,
        producePath = {
            context.filesDir.resolve(DATA_STORE_FILE_NAME).absolutePath.toPath()
        }
    )
}
