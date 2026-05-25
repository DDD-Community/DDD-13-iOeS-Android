package com.pickflow.android.core.network.mapper

import com.pickflow.android.core.network.dto.myspot.MySpotItemDto
import com.pickflow.android.core.network.dto.myspot.MySpotListResponseDto
import com.pickflow.android.core.services.protocols.MySpot
import com.pickflow.android.core.services.protocols.MySpotPage
import com.pickflow.android.core.services.protocols.MySpotStatus

fun MySpotItemDto.toMySpot(): MySpot = MySpot(
    id = spotId,
    name = name,
    theme = parseTheme(theme),
    imageUrl = imageUrl?.takeIf { it.isNotBlank() },
    latitude = latitude,
    longitude = longitude,
    distanceKm = distanceKm,
    createdAt = createdAt,
    status = parseMySpotStatus(status),
    bookmarkCount = bookmarkCount,
)

fun MySpotListResponseDto.toMySpotPage(): MySpotPage = MySpotPage(
    items = spots.map { it.toMySpot() },
    page = page,
    hasNext = hasNext,
)

internal fun parseMySpotStatus(value: String): MySpotStatus = when (value.uppercase()) {
    "PUBLISHED" -> MySpotStatus.PUBLISHED
    "REJECTED" -> MySpotStatus.REJECTED
    else -> MySpotStatus.PENDING
}
