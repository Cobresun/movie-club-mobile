package cobresun.movieclub.app.core.domain

/**
 * Full metadata for one of a club's works, fetched on demand when its details
 * sheet opens: list payloads carry only a summary (no full cast).
 */
interface WorkDetailsRepository {
    /** Full metadata including cast, or null when the work has no external data. */
    suspend fun getWorkDetails(clubSlug: String, workId: String): Result<WorkDetails?, DataError.Remote>
}
