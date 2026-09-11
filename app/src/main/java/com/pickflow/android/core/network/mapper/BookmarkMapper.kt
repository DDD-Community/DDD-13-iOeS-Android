package com.pickflow.android.core.network.mapper

import com.pickflow.android.core.network.dto.bookmark.SavedSpotItemDto
import com.pickflow.android.core.network.dto.bookmark.SavedSpotListResponseDto
import com.pickflow.android.core.services.protocols.SavedSpot
import com.pickflow.android.core.services.protocols.SavedSpotAvailability
import com.pickflow.android.core.services.protocols.SavedSpotPage

fun SavedSpotItemDto.toSavedSpot(): SavedSpot = SavedSpot(
    id = spotId,
    name = name,
    theme = parseTheme(theme),
    imageUrl = imageUrl?.takeIf { it.isNotBlank() },
    latitude = latitude,
    longitude = longitude,
    distanceKm = distanceKm,
    likeCount = likeCount,
    savedAt = savedAt,
    deleted = deleted,
    // 운영 삭제가 작성자 비공개보다 우선한다 — 둘 다면 삭제로 표시.
    availability = when {
        deleted -> SavedSpotAvailability.DELETED
        isPrivate -> SavedSpotAvailability.AUTHOR_PRIVATE
        else -> SavedSpotAvailability.AVAILABLE
    },
)

fun SavedSpotListResponseDto.toSavedSpotPage(): SavedSpotPage = SavedSpotPage(
    items = spots.map { it.toSavedSpot() },
    page = page,
    hasNext = hasNext,
)
