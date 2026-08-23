package com.pickflow.android.core.services.stub

import com.pickflow.android.core.services.protocols.RejectionReason
import com.pickflow.android.core.services.protocols.ReviewDecision

enum class StubOperation {
    MY_SPOT_LIST,
    MY_SPOT_DETAIL,
    CREATE,
    REQUEST_OPEN,
    UNPUBLISH,
    UPDATE,
    DELETE,
    RECOMMEND,
    CANCEL_RECOMMENDATION,
    MAP,
    SPOT_LIST,
    BOOKMARK_ADD,
    BOOKMARK_REMOVE,
    SAVED_SPOTS,
    REVIEW_STATUS,
    ACKNOWLEDGE_REVIEW,
    ACKNOWLEDGE_PUBLISHED_MODAL,
}

sealed interface StubResponse {
    data object Success : StubResponse

    data class Delay(val millis: Long) : StubResponse

    data class Failure(val message: String = "deterministic stub failure") : StubResponse

    data class ReviewRace(
        val decision: ReviewDecision,
        val rejectionReason: RejectionReason? = null,
    ) : StubResponse
}

data class StubScenario(
    val responses: Map<StubOperation, List<StubResponse>> = emptyMap(),
) {
    companion object {
        val Success = StubScenario()

        fun failure(operation: StubOperation, message: String = "deterministic stub failure") =
            StubScenario(mapOf(operation to listOf(StubResponse.Failure(message))))

        fun delayed(operation: StubOperation, millis: Long) =
            StubScenario(mapOf(operation to listOf(StubResponse.Delay(millis))))

        fun withdrawalReviewRace(decision: ReviewDecision) = StubScenario(
            mapOf(
                StubOperation.UNPUBLISH to listOf(StubResponse.ReviewRace(decision)),
            ),
        )
    }
}

class StubServiceException(message: String) : RuntimeException(message)
