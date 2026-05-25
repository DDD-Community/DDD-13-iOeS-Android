package com.pickflow.android.core.network.dto.myspot

import kotlinx.serialization.Serializable

@Serializable
data class MySpotListResponseDto(
    val spots: List<MySpotItemDto> = emptyList(),
    val page: Int = 0,
    val hasNext: Boolean = false,
)

@Serializable
data class MySpotItemDto(
    val spotId: Long = 0L,
    val name: String = "",
    val theme: String = "SUNSET",
    val imageUrl: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val distanceKm: Double? = null,
    val createdAt: String = "",
    val status: String = "PENDING",
    val bookmarkCount: Long = 0L,
)
