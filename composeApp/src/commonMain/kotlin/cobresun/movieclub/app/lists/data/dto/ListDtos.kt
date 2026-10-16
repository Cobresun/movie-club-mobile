package cobresun.movieclub.app.lists.data.dto

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class WorkListDto(
    val id: String,
    val title: String,
    val systemType: String? = null,
    val itemCount: Int = 0,
)

@Serializable
data class ListTitleDto(val title: String)

@Serializable
data class ReorderListsDto(val listIds: List<String>)

@Serializable
data class ReorderItemsDto(val workIds: List<String>)

@Serializable
data class MoveItemDto(val destinationListId: String)

@Serializable
data class ListInsertDto(
    val type: String,
    val title: String,
    val externalId: String,
    val imageUrl: String? = null,
)

/** One entry of `GET list/all-items`: a list item tagged with the user list it's on. */
@Serializable
data class SourcedListItemDto(
    val id: String,
    val type: String = "movie",
    val title: String,
    val createdDate: String,
    val imageUrl: String? = null,
    val externalId: String? = null,
    val addedBy: String? = null,
    val externalData: JsonElement? = null,
    val sourceListId: String,
    val sourceListTitle: String,
)

@Serializable
data class IdDto(val id: String)

@Serializable
data class NextWorkDto(val workId: String? = null)
