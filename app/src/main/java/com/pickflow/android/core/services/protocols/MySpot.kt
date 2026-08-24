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
    val rejection: SpotRejection?,
    val recommendationCount: Long,
    val isRecommended: Boolean,
    val source: SpotSource,
    val updatedAt: String,
)

/**
 * 반려 사유 코드. 서버 `rejection.reason` 와 wire 값이 같다.
 * `ETC` 는 `detail` 이 필수다.
 */
enum class RejectionReason { DUPLICATE, LOW_QUALITY, LOCATION_MISMATCH, FILTER_MISMATCH, ETC }

/**
 * 반려 상세. 표시 문구(`reasonLabel`, `guideMessage`)는 서버가 내려주는 값을 그대로 쓴다.
 * 본인에게만 노출되며 타 사용자 응답에는 포함되지 않는다.
 */
data class SpotRejection(
    val reason: RejectionReason,
    val reasonLabel: String,
    val guideMessage: String?,
    val detail: String?,
    val rejectedAt: String,
)

/** 상태를 바꾸는 요청의 공통 응답. */
sealed interface MySpotStatusChange {
    val spotId: Long
    val status: MySpotStatus
    val updatedAt: String
}

data class MySpotTransitionResult(
    override val spotId: Long,
    override val status: MySpotStatus,
    override val updatedAt: String,
) : MySpotStatusChange

/**
 * 공개 해제 응답. `previousStatus` 로 오픈 신청 철회(PENDING·RE_REVIEW_PENDING)와
 * 비공개 전환(PUBLISHED)을 구분한다. 해제 후 상태는 항상 `DRAFT` 다.
 */
data class MySpotUnpublishResult(
    override val spotId: Long,
    val previousStatus: MySpotStatus,
    override val status: MySpotStatus,
    override val updatedAt: String,
) : MySpotStatusChange {
    val wasOpenRequest: Boolean
        get() = previousStatus == MySpotStatus.PENDING ||
            previousStatus == MySpotStatus.RE_REVIEW_PENDING
}

/** 나만의 스팟 수정 응답. 수정만으로는 상태가 바뀌지 않는다. */
data class MySpotUpdateResult(
    val spotId: Long,
    val status: MySpotStatus,
    val imageUrl: String?,
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

    /**
     * 오픈 신청(검수 요청). `DRAFT` 는 `PENDING` 으로, `REJECTED` 는 `RE_REVIEW_PENDING` 으로 전이된다.
     * 반려 후 보완은 [update] 로 저장한 뒤 이 함수로 재신청한다.
     */
    suspend fun requestOpen(spotId: Long): MySpotTransitionResult = unsupported("requestOpen")

    /**
     * 공개 해제. 검수중이면 오픈 신청 철회로, 공개 상태면 비공개 전환으로 처리되며
     * 결과의 `previousStatus` 로 구분한다. `DRAFT` 는 해제할 대상이 없다.
     */
    suspend fun unpublish(spotId: Long): MySpotUnpublishResult = unsupported("unpublish")

    /**
     * 나만의 스팟 수정. 전달한 값으로 전체를 덮어쓰며 **상태는 바뀌지 않는다**.
     * `replacementImage` 가 null 이면 기존 이미지를 유지한다.
     * 수정 가능한 상태는 `DRAFT` 와 `REJECTED` 뿐이다.
     */
    suspend fun update(
        spotId: Long,
        draft: SpotDraft,
        replacementImage: ImagePayload?,
    ): MySpotUpdateResult = unsupported("update")

    suspend fun delete(spotId: Long): Unit = unsupported("delete")

    private fun <T> unsupported(operation: String): T =
        throw UnsupportedOperationException("MySpotService.$operation is not implemented")
}
