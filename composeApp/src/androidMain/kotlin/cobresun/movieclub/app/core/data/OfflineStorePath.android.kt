package cobresun.movieclub.app.core.data

import android.content.Context
import okio.Path
import okio.Path.Companion.toOkioPath

/** App-private files (not cache): the OS doesn't clear it under storage pressure. */
fun offlineStoreDirectory(context: Context): Path =
    context.filesDir.resolve(OfflineStore.DIRECTORY_NAME).toOkioPath()
