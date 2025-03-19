package cobresun.movieclub.app.auth.data

import androidx.datastore.core.okio.OkioSerializer
import cobresun.movieclub.app.proto.BearerTokenData
import okio.BufferedSink
import okio.BufferedSource
import okio.IOException

class BearerTokenDataSerializer : OkioSerializer<BearerTokenData> {
    override val defaultValue: BearerTokenData
        get() = BearerTokenData()

    override suspend fun readFrom(source: BufferedSource): BearerTokenData {
        try {
            return BearerTokenData.ADAPTER.decode(source)
        } catch (exception: IOException) {
            throw Exception(exception.message ?: "Serialization Exception")
        }
    }

    override suspend fun writeTo(t: BearerTokenData, sink: BufferedSink) {
        sink.write(t.encode())
    }
}
