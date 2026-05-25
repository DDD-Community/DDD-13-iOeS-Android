package com.pickflow.android.core.network.mapper

import com.pickflow.android.core.network.dto.board.BbsPostItemDto
import com.pickflow.android.core.network.dto.board.BbsPostListResponseDto
import com.pickflow.android.core.services.protocols.BoardPost
import com.pickflow.android.core.services.protocols.BoardPostPage

fun BbsPostItemDto.toBoardPost(): BoardPost = BoardPost(
    postId = postId,
    title = title,
    createdAt = createdAt,
    pinned = pinned,
)

fun BbsPostListResponseDto.toBoardPostPage(): BoardPostPage = BoardPostPage(
    items = items.map { it.toBoardPost() },
    page = page,
    hasNext = hasNext,
)
