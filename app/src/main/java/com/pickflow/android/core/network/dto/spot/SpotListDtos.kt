package com.pickflow.android.core.network.dto.spot

import kotlinx.serialization.Serializable

@Serializable
data class SpotListResponseDto(
    val spots: List<SpotItemDto> = emptyList(),
    val page: Int = 0,
    val hasNext: Boolean = false,
)

@Serializable
data class SpotItemDto(
    val spotId: Long = 0L,
    val name: String = "",
    val theme: String = "SUNSET",
    val thumbnailUrl: String? = null,
    val distanceKm: Double? = null,
)
