package cobresun.movieclub.app.core.data

import cobresun.movieclub.app.core.domain.DataError
import cobresun.movieclub.app.core.domain.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path

/**
 * The last good server answer for each read people browse, kept on disk as JSON (the DTO,
 * re-encoded), so the app opens with data offline and paints before the network answers.
 *
 * Sits beneath the in-memory [SessionCaches]: repositories wrap their network fetch in
 * [readThrough], and peek at [load] to paint a club before its first request returns.
 * Keys are paths like `club/<slug>/reviews`; [removePrefix] drops a club's entries.
 * Unreadable files (an older DTO shape, a torn write) read as absent and are deleted.
 * Cleared on sign-in and sign-out with the session caches.
 *
 * [staleKeys] holds the keys whose stored answer was served because the fetch failed, until
 * a fetch for them succeeds, so the UI can say it is showing saved data even while online.
 */
class OfflineStore(
    private val directory: Path,
    private val fileSystem: FileSystem = FileSystem.SYSTEM,
) {
    private val mutex = Mutex()

    private val _staleKeys = MutableStateFlow<Set<String>>(emptySet())
    val staleKeys: StateFlow<Set<String>> = _staleKeys.asStateFlow()

    internal fun markStale(key: String, stale: Boolean) =
        _staleKeys.update { if (stale) it + key else it - key }

    /** Forgets which answers were stale, before screens reload (they mark them again if still failing). */
    fun forgetStale() {
        _staleKeys.value = emptySet()
    }

    /** The stored value for [key], or null when there is none (or it no longer decodes). */
    suspend fun <T> load(key: String, serializer: KSerializer<T>): T? = io {
        val path = pathOf(key)
        val text = runCatching { fileSystem.read(path) { readUtf8() } }.getOrNull() ?: return@io null
        runCatching { storeJson.decodeFromString(serializer, text) }
            .onFailure { runCatching { fileSystem.delete(path) } }
            .getOrNull()
    }

    suspend fun <T> save(key: String, serializer: KSerializer<T>, value: T) {
        val text = withContext(Dispatchers.Default) { storeJson.encodeToString(serializer, value) }
        io {
            runCatching {
                fileSystem.createDirectories(directory)
                // Write aside and rename, so a crash mid-write never leaves half a file.
                val target = pathOf(key)
                val temp = directory / "${target.name}.tmp"
                fileSystem.write(temp) { writeUtf8(text) }
                fileSystem.atomicMove(temp, target)
            }
        }
    }

    suspend fun remove(key: String) = io { runCatching { fileSystem.delete(pathOf(key)) }; Unit }

    /** Removes every entry whose key starts with [prefix] (e.g. one club's `club/<slug>/`). */
    suspend fun removePrefix(prefix: String) = io {
        _staleKeys.update { keys -> keys.filterTo(mutableSetOf()) { !it.startsWith(prefix) } }
        val encoded = encodeKey(prefix)
        runCatching {
            fileSystem.list(directory).filter { it.name.startsWith(encoded) }.forEach { fileSystem.delete(it) }
        }
        Unit
    }

    suspend fun clear() = io {
        _staleKeys.value = emptySet()
        runCatching { fileSystem.deleteRecursively(directory) }
        Unit
    }

    private fun pathOf(key: String): Path = directory / (encodeKey(key) + ".json")

    // One file operation at a time: a save and a clear (sign-out) never interleave.
    private suspend fun <R> io(block: () -> R): R = mutex.withLock { withContext(Dispatchers.IO) { block() } }

    companion object {
        /** Bump when stored DTO shapes change incompatibly; the old directory is ignored. */
        const val DIRECTORY_NAME = "offline-v1"

        /**
         * Keeps letters, digits, `-` and `_`; everything else becomes `~XX` (hex). Prefix-preserving,
         * so [removePrefix] can match encoded file names, and collision-free.
         */
        internal fun encodeKey(key: String): String = buildString {
            for (byte in key.encodeToByteArray()) {
                val c = byte.toInt().toChar()
                if (c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' || c == '-' || c == '_') {
                    append(c)
                } else {
                    append('~')
                    append(HEX[(byte.toInt() shr 4) and 0xF])
                    append(HEX[byte.toInt() and 0xF])
                }
            }
        }

        private const val HEX = "0123456789ABCDEF"
    }
}

private val storeJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * Network first: a successful [fetch] is stored under [key] and returned; when it fails for
 * connectivity reasons, the stored answer is returned instead (if there is one).
 */
suspend fun <T> OfflineStore.readThrough(
    key: String,
    serializer: KSerializer<T>,
    fetch: suspend () -> Result<T, DataError.Remote>,
): Result<T, DataError.Remote> = when (val result = fetch()) {
    is Result.Success -> result.also {
        save(key, serializer, it.data)
        markStale(key, false)
    }
    is Result.Error -> {
        val saved = if (result.error.isConnectivityFailure) load(key, serializer) else null
        if (saved != null) {
            markStale(key, true)
            Result.Success(saved)
        } else result
    }
}
