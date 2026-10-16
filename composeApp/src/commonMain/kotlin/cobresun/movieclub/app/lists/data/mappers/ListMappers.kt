package cobresun.movieclub.app.lists.data.mappers

import cobresun.movieclub.app.lists.data.dto.WorkListDto
import cobresun.movieclub.app.lists.domain.WorkList

fun WorkListDto.toWorkList(): WorkList = WorkList(id = id, title = title, itemCount = itemCount)
