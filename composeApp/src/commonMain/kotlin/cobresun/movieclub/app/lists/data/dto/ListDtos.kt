package cobresun.movieclub.app.lists.data.dto

import kotlinx.serialization.Serializable

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

@Serializable
data class IdDto(val id: String)

@Serializable
data class NextWorkDto(val workId: String? = null)
