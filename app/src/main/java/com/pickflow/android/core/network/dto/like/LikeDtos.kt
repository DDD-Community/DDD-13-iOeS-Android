package com.pickflow.android.core.network.dto.like

import kotlinx.serialization.Serializable

/**
 * `BookmarkResponseDto` 와 같은 형태. 화면은 낙관값을 유지하므로 `likeCount` 를 UI 에
 * 그대로 반영하지는 않는다 — `SpotRecommendationViewModel` 참고.
 * `isLiked` 는 서버 문서에 응답 필드로 명시돼 있지 않아 optional 로 둔다.
 */
@Serializable
data class LikeResponseDto(
    val spotId: Long = 0L,
    val likeCount: Long = 0L,
    val isLiked: Boolean? = null,
)
