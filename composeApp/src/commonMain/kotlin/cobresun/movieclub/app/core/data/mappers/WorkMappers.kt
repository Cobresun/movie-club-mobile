@file:OptIn(ExperimentalTime::class)

package cobresun.movieclub.app.core.data.mappers

import cobresun.movieclub.app.core.data.dto.WorkListItemDto
import cobresun.movieclub.app.core.domain.CastMember
import cobresun.movieclub.app.core.domain.Work
import cobresun.movieclub.app.core.domain.WorkDetails
import cobresun.movieclub.app.core.domain.WorkType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

private const val TMDB_IMAGE_BASE = "https://image.tmdb.org/t/p"

fun tmdbImageUrl(path: String?, size: String): String? =
    path?.takeIf { it.isNotBlank() }?.let { "$TMDB_IMAGE_BASE/$size$it" }

fun WorkListItemDto.toWork(timeZone: TimeZone = TimeZone.currentSystemDefault()): Work {
    return Work(
        id = id,
        type = WorkType.from(type),
        title = title,
        addedAt = createdDate,
        addedDate = parseLocalDate(createdDate, timeZone),
        imageUrl = bestPosterUrl(imageUrl, externalData),
        externalId = externalId,
        addedByUserId = addedBy,
        details = externalData.toWorkDetails(),
    )
}

/** Parses an ISO instant into the local calendar date, or null if malformed. */
fun parseLocalDate(iso: String, timeZone: TimeZone): LocalDate? =
    runCatching { Instant.parse(iso).toLocalDateTime(timeZone).date }.getOrNull()

/**
 * The list payload's `imageUrl` is usually a tiny (w154) TMDB thumbnail. Prefer
 * a sharper poster built from `poster_path` when the metadata has one.
 */
private fun bestPosterUrl(imageUrl: String?, externalData: JsonElement?): String? {
    val obj = externalData as? JsonObject
    val url = when (obj?.string("kind")) {
        "movie" -> tmdbImageUrl(obj.string("poster_path"), "w342") ?: imageUrl
        "book" -> imageUrl?.takeIf { it.isNotBlank() } ?: obj.string("coverUrl")
        else -> imageUrl
    }
    return url?.takeIf { it.isNotBlank() && !it.endsWith("null") }?.replace("http://", "https://")
}

fun JsonElement?.toWorkDetails(): WorkDetails? {
    val obj = this as? JsonObject ?: return null
    return when (obj.string("kind")) {
        "movie" -> WorkDetails.Movie(
            overview = obj.string("overview"),
            tagline = obj.string("tagline"),
            releaseDate = obj.string("release_date"),
            runtimeMinutes = obj.int("runtime")?.takeIf { it > 0 },
            genres = obj.stringList("genres"),
            directors = obj.objectList("directors").mapNotNull { it.string("name") },
            castNames = obj.stringList("castNames"),
            majorCastNames = obj.stringList("majorCastNames"),
            voteAverage = obj.double("vote_average"),
            backdropUrl = tmdbImageUrl(obj.string("backdrop_path"), "w780"),
            cast = obj.objectList("actors").mapNotNull { actor ->
                val name = actor.string("name") ?: return@mapNotNull null
                CastMember(
                    name = name,
                    character = actor.string("character"),
                    profileImageUrl = tmdbImageUrl(actor.string("profilePath"), "w185"),
                )
            },
        )

        "book" -> WorkDetails.Book(
            description = obj.string("description"),
            authors = obj.stringList("authors"),
            subjects = obj.stringList("subjects"),
            firstPublishYear = obj.int("firstPublishYear"),
            pageCount = obj.int("numberOfPages"),
            coverUrl = obj.string("coverUrl")?.replace("http://", "https://"),
        )

        else -> null
    }
}

private fun JsonObject.primitive(key: String): JsonPrimitive? =
    (this[key] as? JsonPrimitive)?.takeUnless { it is JsonNull }

private fun JsonObject.string(key: String): String? =
    primitive(key)?.takeIf { it.isString }?.content?.takeIf { it.isNotBlank() }

private fun JsonObject.int(key: String): Int? =
    primitive(key)?.let { it.intOrNull ?: it.doubleOrNull?.toInt() }

private fun JsonObject.double(key: String): Double? =
    primitive(key)?.let { it.doubleOrNull ?: it.content.toDoubleOrNull() }

private fun JsonObject.stringList(key: String): List<String> =
    (this[key] as? JsonArray)?.mapNotNull { el ->
        (el as? JsonPrimitive)?.takeIf { it.isString }?.content
            ?: (el as? JsonObject)?.string("name")
    }.orEmpty()

private fun JsonObject.objectList(key: String): List<JsonObject> =
    (this[key] as? JsonArray)?.filterIsInstance<JsonObject>().orEmpty()
