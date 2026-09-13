package com.pickflow.android.core.network.dto.bookmark

import kotlinx.serialization.Serializable

@Serializable
data class BookmarkResponseDto(
    val bookmarkCount: Long = 0L,
)

@Serializable
data class SavedSpotListResponseDto(
    val spots: List<SavedSpotItemDto> = emptyList(),
    val page: Int = 0,
    val hasNext: Boolean = false,
)

@Serializable
data class SavedSpotItemDto(
    val spotId: Long = 0L,
    val name: String = "",
    val theme: String = "SUNSET",
    val imageUrl: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val distanceKm: Double? = null,
    /** 서버 `likeCount`(추천 수). 저장 리스트 메타 행의 "추천 N" 표기에 쓴다. */
    val likeCount: Long = 0L,
    val savedAt: String = "",
    val deleted: Boolean = false,
    /**
     * 검수 상태만 본 비공개 여부(status != PUBLISHED). 노출 판단에는 쓰지 않는다 —
     * 등록자가 노출을 끈 PUBLISHED 스팟을 놓친다. [isReleased] 를 볼 것.
     */
    val isPrivate: Boolean = false,
    /**
     * 지도/리스트 노출 여부. 노출 판단은 이 값만 본다 —
     * `isPrivate` 는 검수 상태만 보고 등록자의 노출 토글(rel_yn)을 반영하지 않는다(스웨거 명시).
     */
    val isReleased: Boolean = true,
)
