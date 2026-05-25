package com.pickflow.android.core.network.dto.bookmark

import kotlinx.serialization.Serializable

@Serializable
data class BookmarkResponseDto(
    val bookmarkCount: Long = 0L,
)
