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
 * POST /v1/users/me/my-spots 의 `meta` part로 직렬화될 JSON 페이로드 (추정).
 *
 * TODO(BE confirm): OpenAPI 스펙상 multipart schema 가 깨져 있어 (integer로 표기) 정확한
 * 필드 이름/구조 미확정. 본 DTO는 description("이미지 + 메타데이터 JSON")과 SpotAdminCreateRequest.Item
 * 스키마를 참고한 추정. BE 컨트롤러 시그니처 확인 후 필드명/형태 조정 필요.
 */
@Serializable
data class CreateMySpotMetaRequest(
    val name: String,
    val theme: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val comment: String? = null,
    val recordedDate: String? = null,
    val recordedTime: String? = null,
)
