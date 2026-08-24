package com.pickflow.android.core.services.protocols

enum class ReviewDecision { APPROVED, REJECTED }

data class ReviewResult(
    val resultId: Long,
    val spotId: Long,
    val decision: ReviewDecision,
    val occurredAt: String,
    val isAcknowledged: Boolean = false,
    val publishedModalAcknowledged: Boolean = false,
)

data class ReviewResultStatus(
    val pendingRequestCount: Int,
    /** 스낵바 미확인 또는 승인 완료 모달 미확인 결과. */
    val unacknowledgedResults: List<ReviewResult>,
) {
    val hasIndicator: Boolean
        get() = pendingRequestCount > 0 || unacknowledgedResults.any { !it.isAcknowledged }
}

interface ReviewResultService {
    suspend fun status(): ReviewResultStatus

    suspend fun acknowledge(resultId: Long)

    suspend fun acknowledgePublishedModal(resultId: Long)
}
