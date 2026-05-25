package com.pickflow.android.core.services.protocols

/**
 * 게시판(공지사항 등) API. masterId 로 게시판을 구분한다.
 *
 * - postId: 게시글 번호 (Long)
 * - pinned: 상단 고정 여부 (true면 최신순 상단 표시)
 * - createdAt: 작성일 (yyyy-MM-dd)
 */
data class BoardPost(
    val postId: Long,
    val title: String,
    val createdAt: String,
    val pinned: Boolean,
)

data class BoardPostPage(
    val items: List<BoardPost>,
    val page: Int,
    val hasNext: Boolean,
)

interface BoardService {
    /**
     * 게시판 목록 (20개/page). 고정 공지가 최신순 상단.
     * page: 0-base.
     */
    suspend fun posts(masterId: Long, page: Int = 0): BoardPostPage
}
