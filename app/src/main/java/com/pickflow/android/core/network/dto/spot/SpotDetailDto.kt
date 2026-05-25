package com.pickflow.android.core.network.dto.spot

import kotlinx.serialization.Serializable

@Serializable
data class SpotDetailResponseDto(
    val spotId: Long = 0L,
    val name: String = "",
    val comment: String = "",
    val theme: String = "SUNSET",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val address: String = "",
    val addressRoad: String? = null,
    val addressJibun: String? = null,
    val imageUrl: String? = null,
    val recordedDate: String = "",
    val recordedTime: String = "",
    val weatherSky: String? = null,
    val precipitation: String? = null,
    val precipitationProbability: Int = 0,
    val congestionLevel: String? = null,
    val sunsetTime: String? = null,
    val astronomyDate: String? = null,
    val weatherUpdatedAt: String? = null,
    val congestionUpdatedAt: String? = null,
    val parkingInfo: String? = null,
    val bookmarkCount: Long = 0L,
    val isBookmarked: Boolean = false,
    val isMySpot: Boolean = false,
)
