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

@Serializable
data class CreateMySpotResponseDto(
    val spotId: Long = 0L,
    val status: String = "PENDING",
    val imageUrl: String? = null,
)

/**
 * POST /v1/users/me/my-spots 의 `request` part로 직렬화될 JSON 페이로드.
 *
 * iOS `SpotRegisterRequest` 1:1 — name/theme/latitude/longitude/comment/recordedDate/recordedTime.
 * (address 는 서버 스펙에 없어 전송하지 않는다.)
 */
@Serializable
data class CreateMySpotMetaRequest(
    val name: String,
    val theme: String,
    val latitude: Double,
    val longitude: Double,
    val comment: String? = null,
    val recordedDate: String? = null,
    val recordedTime: String? = null,
)
