package com.pickflow.android.core.services.impl

import com.pickflow.android.core.network.api.BookmarkApi
import com.pickflow.android.core.network.unwrap
import com.pickflow.android.core.services.protocols.BookmarkService
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase D-3 진행 중 서버 연동 + 레거시 in-memory 캐시를 동시에 유지.
 *
 * - add(): 서버 API 호출 + 로컬 캐시에 추가 (낙관적 토글 UI 지원)
 * - toggle/isBookmarked/bookmarkedIds: 로컬 캐시 기반 (legacy). remove API 통합 후
 *   toggle은 add/remove 분기로, bookmarkedIds는 savedSpots 응답으로 대체 예정.
 */
@Singleton
class DefaultBookmarkService @Inject constructor(
    private val bookmarkApi: BookmarkApi,
) : BookmarkService {
    private val store: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

    override suspend fun add(spotId: String): Long {
        val longId = spotId.toLongOrNull()
            ?: throw IllegalArgumentException("spotId는 정수여야 합니다: $spotId")
        val count = bookmarkApi.addBookmark(longId).unwrap().bookmarkCount
        synchronized(store) { store.add(spotId) }
        return count
    }

    override suspend fun isBookmarked(spotId: String): Boolean = store.contains(spotId)

    override suspend fun toggle(spotId: String): Boolean = synchronized(store) {
        // TODO(Phase D-3 remove): false 분기에서 api.removeBookmark 호출로 교체.
        if (store.contains(spotId)) {
            store.remove(spotId)
            false
        } else {
            store.add(spotId)
            true
        }
    }

    override suspend fun bookmarkedIds(): Set<String> = synchronized(store) { store.toSet() }
}
