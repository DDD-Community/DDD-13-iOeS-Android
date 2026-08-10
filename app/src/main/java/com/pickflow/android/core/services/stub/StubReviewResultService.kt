package com.pickflow.android.core.services.stub

import com.pickflow.android.core.services.protocols.ReviewResultService
import com.pickflow.android.core.services.protocols.ReviewResultStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StubReviewResultService @Inject constructor(
    private val backend: StubSpotBackend,
) : ReviewResultService {
    override suspend fun status(): ReviewResultStatus = backend.reviewStatus()

    override suspend fun acknowledge(resultId: Long) = backend.acknowledgeReview(resultId)

    override suspend fun acknowledgePublishedModal(resultId: Long) =
        backend.acknowledgePublishedModal(resultId)
}
