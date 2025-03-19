package cobresun.movieclub.app.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.okio.OkioStorage
import cobresun.movieclub.app.auth.data.BearerTokenDataSerializer
import cobresun.movieclub.app.proto.BearerTokenData
import okio.FileSystem
import okio.Path

fun createBearerTokenDataStore(
    fileSystem: FileSystem,
    producePath: () -> Path
): DataStore<BearerTokenData> {
    return DataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = fileSystem,
            producePath = producePath,
            serializer = BearerTokenDataSerializer(),
        ),
    )
}

internal const val DATA_STORE_FILE_NAME = "proto_bearer_token.preferences_pb"
