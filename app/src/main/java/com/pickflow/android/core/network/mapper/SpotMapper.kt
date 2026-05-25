package com.pickflow.android.core.network.mapper

import com.pickflow.android.core.network.dto.spot.SpotItemDto
import com.pickflow.android.core.network.dto.spot.SpotListResponseDto
import com.pickflow.android.core.network.dto.spot.SpotSummaryDto
import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.SpotMapMarker
import com.pickflow.android.core.services.protocols.SpotPage
import com.pickflow.android.core.services.protocols.SpotTheme

fun SpotSummaryDto.toMapMarker(): SpotMapMarker = SpotMapMarker(
    spotId = spotId,
    imageUrl = spotImageUrl?.takeIf { it.isNotBlank() },
    coordinates = Coordinates(latitude = latitude, longitude = longitude),
    isMySpot = isMySpot,
)

fun SpotItemDto.toSpot(): Spot = Spot(
    id = spotId.toString(),
    name = name,
    theme = parseTheme(theme),
    latitude = 0.0,
    longitude = 0.0,
    imageUrl = thumbnailUrl?.takeIf { it.isNotBlank() },
    address = "",
    distanceKm = distanceKm,
)

fun SpotListResponseDto.toSpotPage(): SpotPage = SpotPage(
    items = spots.map { it.toSpot() },
    page = page,
    hasNext = hasNext,
)

internal fun parseTheme(value: String): SpotTheme = when (value.uppercase()) {
    "YUNSEUL" -> SpotTheme.YUNSEUL
    else -> SpotTheme.SUNSET
}
