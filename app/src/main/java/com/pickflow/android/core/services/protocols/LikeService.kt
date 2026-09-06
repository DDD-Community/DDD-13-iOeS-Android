package com.pickflow.android.core.services.protocols

/**
 * 좋아요(UI 용어로는 "추천"). `BookmarkService` 와 같은 형태 —
 * 서버가 단일 출처이고 로컬 캐시를 두지 않는다.
 */
interface LikeService {
    /** 좋아요 등록. 서버 호출 후 갱신된 좋아요 수를 반환한다. */
    suspend fun add(spotId: String): Long

    /** 좋아요 해제. 서버 호출 후 갱신된 좋아요 수를 반환한다. */
    suspend fun remove(spotId: String): Long
}
