package com.pickflow.android.core.services.protocols

data class RecommendationResult(
    val spotId: Long,
    val recommendationCount: Long,
    val isRecommended: Boolean,
)

interface RecommendationService {
    suspend fun recommend(spotId: Long): RecommendationResult

    suspend fun cancel(spotId: Long): RecommendationResult
}
