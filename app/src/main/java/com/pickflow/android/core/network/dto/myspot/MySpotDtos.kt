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
    /** 서버 미제공 시 null — 목록 셀에서 추천 수를 감춘다. */
    val likeCount: Long? = null,
    /** 공개 이력. 서버 필드가 생기기 전까지 항상 false → DRAFT 는 배지 없음으로 남는다. */
    val wasPublished: Boolean = false,
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

/** PUT /v1/users/me/my-spots/{spotId} 의 `request` part. Create 와 필드가 같다. */
@Serializable
data class UpdateMySpotMetaRequest(
    val name: String,
    val theme: String,
    val latitude: Double,
    val longitude: Double,
    val comment: String? = null,
    val recordedDate: String? = null,
    val recordedTime: String? = null,
)

@Serializable
data class UpdateMySpotResponseDto(
    val spotId: Long = 0L,
    val status: String = "DRAFT",
    val imageUrl: String? = null,
)

/**
 * POST/DELETE /v1/users/me/my-spots/{spotId}/releases.
 * 검수 상태(status)와 무관한 노출 플래그라 응답에 status 가 없다.
 */
@Serializable
data class ReleaseMySpotResponseDto(
    val spotId: Long = 0L,
    val released: Boolean = false,
)

/** POST /v1/users/me/my-spots/{spotId}/open-requests */
@Serializable
data class OpenMySpotResponseDto(
    val spotId: Long = 0L,
    val status: String = "PENDING",
)

/**
 * DELETE /v1/users/me/my-spots/{spotId}/publications.
 * `previousStatus` 로 오픈 신청 철회와 비공개 전환을 구분한다 — 클라 추론 불필요.
 */
@Serializable
data class CancelPublicationResponseDto(
    val spotId: Long = 0L,
    val previousStatus: String = "DRAFT",
    val status: String = "DRAFT",
)
