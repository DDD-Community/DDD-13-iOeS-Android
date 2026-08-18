package com.pickflow.android.core.services.protocols

data class Spot(
    val id: String,
    val name: String,
    val theme: SpotTheme,
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String? = null,
    val address: String = "",
    val distanceKm: Double? = null,
    val bookmarkCount: Long = 0L,
    val isBookmarked: Boolean = false,
    val likeCount: Long = 0L,
    val isLiked: Boolean = false,
)

/**
 * 서버 스펙(OpenAPI Photo API v1.0.0)과 정렬된 테마 enum.
 * - SUNSET: 노을
 * - YUNSEUL: 윤슬
 */
enum class SpotTheme { SUNSET, YUNSEUL }

data class SpotPage(
    val items: List<Spot>,
    val page: Int,
    val hasNext: Boolean,
)
