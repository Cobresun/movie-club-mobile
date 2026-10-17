package cobresun.movieclub.app.core.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * OkHttp engine that recovers from connections that died while the app or device
 * was suspended (host sleep, Doze, network switches).
 *
 * movie-club.app speaks HTTP/2, so every request shares one pooled connection. If
 * that socket silently dies (NAT entry dropped while asleep: no FIN/RST ever
 * arrives), every new request is multiplexed onto it and hangs until the request
 * timeout. Two defences:
 *  - HTTP/2 pings detect a dead connection within [PING_INTERVAL_S] and fail it,
 *    so the next call opens a fresh one;
 *  - the pool is evicted whenever the default network changes or is lost.
 */
fun createResilientOkHttpEngine(context: Context): HttpClientEngine {
    val pool = ConnectionPool()
    val client = OkHttpClient.Builder()
        .connectionPool(pool)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .pingInterval(PING_INTERVAL_S, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    runCatching {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        connectivity?.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = pool.evictAll()
            override fun onLost(network: Network) = pool.evictAll()
        })
    }

    return OkHttp.create { preconfigured = client }
}

private const val PING_INTERVAL_S = 10L
