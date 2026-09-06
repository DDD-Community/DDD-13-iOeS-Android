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
    val savedAt: String = "",
    val deleted: Boolean = false,
    /** 작성자가 비공개로 돌린 유저 스팟. 서버는 이때 imageUrl 을 null 로 마스킹한다. */
    val isPrivate: Boolean = false,
)
