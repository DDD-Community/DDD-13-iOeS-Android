package com.pickflow.android.core.services.stub

import com.pickflow.android.core.services.protocols.RecommendationResult
import com.pickflow.android.core.services.protocols.RecommendationService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StubRecommendationService @Inject constructor(
    private val backend: StubSpotBackend,
) : RecommendationService {
    override suspend fun recommend(spotId: Long): RecommendationResult = backend.recommend(spotId)

    override suspend fun cancel(spotId: Long): RecommendationResult = backend.cancelRecommendation(spotId)
}
