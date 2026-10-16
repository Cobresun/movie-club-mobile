package cobresun.movieclub.app.core.data.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** One work on a club list, as returned by `/list/:id` and `/list/reviews`. */
@Serializable
data class WorkListItemDto(
    val id: String,
    val type: String = "movie",
    val title: String,
    val createdDate: String,
    val imageUrl: String? = null,
    val externalId: String? = null,
    val addedBy: String? = null,
    /** Discriminated union on `kind` ("movie" | "book"); parsed by `toWorkDetails`. */
    val externalData: JsonElement? = null,
)
