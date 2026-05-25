package com.pickflow.android.core.network.mapper

import com.pickflow.android.core.network.dto.spot.SpotSummaryDto
import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.SpotMapMarker

fun SpotSummaryDto.toMapMarker(): SpotMapMarker = SpotMapMarker(
    spotId = spotId,
    imageUrl = spotImageUrl?.takeIf { it.isNotBlank() },
    coordinates = Coordinates(latitude = latitude, longitude = longitude),
    isMySpot = isMySpot,
)
