package com.pickflow.android.core.network.dto.board

import kotlinx.serialization.Serializable

@Serializable
data class BbsPostListResponseDto(
    val items: List<BbsPostItemDto> = emptyList(),
    val page: Int = 0,
    val hasNext: Boolean = false,
)

@Serializable
data class BbsPostItemDto(
    val postId: Long = 0L,
    val title: String = "",
    val createdAt: String = "",
    val pinned: Boolean = false,
)
