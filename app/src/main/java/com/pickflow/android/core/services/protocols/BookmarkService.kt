package com.pickflow.android.core.services.protocols

interface BookmarkService {
    /**
     * 북마크 지정. 서버 호출 후 갱신된 북마크 수를 반환한다.
     * Phase D-3 신규. 기존 toggle() 보다 우선 사용 권장.
     */
    suspend fun add(spotId: String): Long

    /**
     * 북마크 해제. 서버 호출 후 갱신된 북마크 수를 반환한다.
     * Phase D-3 신규.
     */
    suspend fun remove(spotId: String): Long

    // ---- Legacy (Phase D-3 진행 중 — savedSpots 통합 후 제거 예정) ----
    suspend fun isBookmarked(spotId: String): Boolean
    suspend fun toggle(spotId: String): Boolean
    suspend fun bookmarkedIds(): Set<String>
}
