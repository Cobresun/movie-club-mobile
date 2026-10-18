package cobresun.movieclub.app.core.domain

sealed interface AsyncResult<out T> {
    data class Success<T>(val data: T) : AsyncResult<T>
    data class Error(
        val exception: Throwable? = null,
        val dataError: DataError? = null
    ) : AsyncResult<Nothing>
    data object Loading : AsyncResult<Nothing>
}

fun <T, R> AsyncResult<T>.mapSuccess(transform: (T) -> R): AsyncResult<R> {
    return when (this) {
        is AsyncResult.Loading -> AsyncResult.Loading
        is AsyncResult.Success -> AsyncResult.Success(transform(data))
        is AsyncResult.Error -> AsyncResult.Error(exception, dataError)
    }
}
