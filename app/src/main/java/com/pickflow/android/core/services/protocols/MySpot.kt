package com.pickflow.android.core.services.protocols

/**
 * 사용자가 등록한 스팟 (GET /v1/users/me/my-spots) 도메인.
 *
 * - id: 서버 Long
 * - status: 검수 단계
 *   - PENDING: 검수 대기 (등록 직후)
 *   - PUBLISHED: 공개 (운영자 승인)
 *   - REJECTED: 반려
 * - distanceKm: 좌표 미전달 시 null
 * - createdAt: 등록 시각 (ISO-8601 date-time)
 */
data class MySpot(
    val id: Long,
    val name: String,
    val theme: SpotTheme,
    val imageUrl: String?,
    val latitude: Double,
    val longitude: Double,
    val distanceKm: Double?,
    val createdAt: String,
    val status: MySpotStatus,
    val bookmarkCount: Long,
)

data class MySpotPage(
    val items: List<MySpot>,
    val page: Int,
    val hasNext: Boolean,
)

enum class MySpotStatus { PENDING, PUBLISHED, REJECTED }

/**
 * POST /v1/users/me/my-spots 응답 모델. 등록 직후 상태는 항상 PENDING.
 */
data class CreateMySpotResult(
    val spotId: Long,
    val status: MySpotStatus,
    val imageUrl: String?,
)

interface MySpotService {
    /**
     * 본인이 등록한 스팟 페이지 (PENDING/PUBLISHED/REJECTED 모두 노출).
     * 좌표 전달 시 distanceKm 포함.
     */
    suspend fun list(page: Int, coordinates: Coordinates? = null): MySpotPage

    /**
     * 새 스팟 등록 (multipart). 이미지 + 메타데이터 동시 업로드. 등록 후 항상 PENDING 상태.
     *
     * 향후 기존 SpotService.register(SpotDraft)는 deprecated 후 본 메서드로 일원화 예정.
     */
    suspend fun create(draft: SpotDraft, image: ImagePayload): CreateMySpotResult
}
