package com.pickflow.android.core.network.dto.spot

import kotlinx.serialization.Serializable

@Serializable
data class SpotViewportResponseDto(
    val spots: List<SpotSummaryDto> = emptyList(),
)

@Serializable
data class SpotSummaryDto(
    val spotId: Long = 0L,
    val spotImageUrl: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val isMySpot: Boolean = false,
)
