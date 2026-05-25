package com.pickflow.android.core.network.mapper

import com.pickflow.android.core.network.dto.bookmark.SavedSpotItemDto
import com.pickflow.android.core.network.dto.bookmark.SavedSpotListResponseDto
import com.pickflow.android.core.services.protocols.SavedSpot
import com.pickflow.android.core.services.protocols.SavedSpotPage

fun SavedSpotItemDto.toSavedSpot(): SavedSpot = SavedSpot(
    id = spotId,
    name = name,
    theme = parseTheme(theme),
    imageUrl = imageUrl?.takeIf { it.isNotBlank() },
    latitude = latitude,
    longitude = longitude,
    distanceKm = distanceKm,
    savedAt = savedAt,
    deleted = deleted,
)

fun SavedSpotListResponseDto.toSavedSpotPage(): SavedSpotPage = SavedSpotPage(
    items = spots.map { it.toSavedSpot() },
    page = page,
    hasNext = hasNext,
)
