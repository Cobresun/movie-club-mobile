package cobresun.movieclub.app.lists.data.mappers

import cobresun.movieclub.app.core.data.dto.WorkListItemDto
import cobresun.movieclub.app.core.data.mappers.toWork
import cobresun.movieclub.app.lists.data.dto.SourcedListItemDto
import cobresun.movieclub.app.lists.data.dto.WorkListDto
import cobresun.movieclub.app.lists.domain.ListedWork
import cobresun.movieclub.app.lists.domain.WorkList

fun WorkListDto.toWorkList(): WorkList = WorkList(id = id, title = title, itemCount = itemCount)

fun SourcedListItemDto.toListedWork(): ListedWork = ListedWork(
    work = WorkListItemDto(id, type, title, createdDate, imageUrl, externalId, addedBy, externalData).toWork(),
    listId = sourceListId,
    listTitle = sourceListTitle,
)
