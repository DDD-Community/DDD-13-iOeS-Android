package com.pickflow.android.core.services.protocols

/**
 * 사용자가 등록한 스팟 (GET /v1/users/me/my-spots) 도메인.
 *
 * - id: 서버 Long
 * - status: 검수 단계
 *   - DRAFT: 나만보기 (등록 직후)
 *   - PENDING: 최초 검수 대기
 *   - RE_REVIEW_PENDING: 보완 후 재검수 대기
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

enum class MySpotStatus { DRAFT, PENDING, RE_REVIEW_PENDING, REJECTED, PUBLISHED }

data class MySpotDetail(
    val id: Long,
    val name: String,
    val theme: SpotTheme,
    val imageUrl: String?,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val capturedDate: String,
    val capturedTime: String,
    val comment: String,
    val status: MySpotStatus,
    val rejectionReason: String?,
    val recommendationCount: Long,
    val isRecommended: Boolean,
    val source: SpotSource,
    val updatedAt: String,
)

data class MySpotTransitionResult(
    val spotId: Long,
    val status: MySpotStatus,
    val updatedAt: String,
)

class MySpotTransitionConflictException(
    val spotId: Long,
    val latestStatus: MySpotStatus,
) : IllegalStateException("spot $spotId was already processed as $latestStatus")

/**
 * 등록 결과 모델. Stub-first 정책에서 등록 직후 상태는 DRAFT다.
 */
data class CreateMySpotResult(
    val spotId: Long,
    val status: MySpotStatus,
    val imageUrl: String?,
)

interface MySpotService {
    /**
     * 본인이 등록한 스팟 페이지 (다섯 상태 모두 노출).
     * 좌표 전달 시 distanceKm 포함.
     */
    suspend fun list(page: Int, coordinates: Coordinates? = null): MySpotPage

    /**
     * 새 스팟 등록. Stub-first 범위에서는 등록 후 DRAFT 상태를 반환한다.
     */
    suspend fun create(draft: SpotDraft, image: ImagePayload): CreateMySpotResult

    suspend fun detail(spotId: Long): MySpotDetail = unsupported("detail")

    suspend fun requestOpen(spotId: Long): MySpotTransitionResult = unsupported("requestOpen")

    suspend fun withdrawRequest(spotId: Long): MySpotTransitionResult = unsupported("withdrawRequest")

    suspend fun withdrawRejection(spotId: Long): MySpotTransitionResult = unsupported("withdrawRejection")

    suspend fun reviseAndResubmit(
        spotId: Long,
        draft: SpotDraft,
        replacementImage: ImagePayload?,
    ): MySpotTransitionResult = unsupported("reviseAndResubmit")

    suspend fun cancelOpen(spotId: Long): MySpotTransitionResult = unsupported("cancelOpen")

    suspend fun delete(spotId: Long): Unit = unsupported("delete")

    private fun <T> unsupported(operation: String): T =
        throw UnsupportedOperationException("MySpotService.$operation is not implemented")
}
