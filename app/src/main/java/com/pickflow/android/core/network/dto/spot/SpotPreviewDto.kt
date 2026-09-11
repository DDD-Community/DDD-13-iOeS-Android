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
    val isBookmarked: Boolean = false,
    /** 추천 수. prod 미배포 구간에서는 필드가 없어 0 으로 떨어진다. */
    val likeCount: Long = 0L,
)
