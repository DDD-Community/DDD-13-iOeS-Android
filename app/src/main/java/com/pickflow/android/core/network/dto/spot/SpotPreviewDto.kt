package com.pickflow.android.core.network.dto.spot

import kotlinx.serialization.Serializable

@Serializable
data class SpotPreviewResponseDto(
    val spotId: Long = 0L,
    val name: String = "",
    val isMySpot: Boolean = false,
    val theme: String = "SUNSET",
    val bookmarkCount: Long = 0L,
    val distanceKm: Double? = null,
    val imageUrl: String? = null,
    val addressSimple: String = "",
    val addressRoad: String? = null,
    val addressJibun: String? = null,
)
