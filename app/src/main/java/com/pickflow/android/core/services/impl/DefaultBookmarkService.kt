package com.pickflow.android.core.services.impl

import com.pickflow.android.core.network.api.BookmarkApi
import com.pickflow.android.core.network.unwrap
import com.pickflow.android.core.services.protocols.BookmarkService
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Phase D-3: add/remove는 서버 연동, toggle은 로컬 캐시 기준으로 add/remove 라우팅.
 * isBookmarked/bookmarkedIds는 로컬 캐시 (Phase D-3 savedSpots 통합 시 서버 응답으로 대체).
 */
@Singleton
class DefaultBookmarkService @Inject constructor(
    private val bookmarkApi: BookmarkApi,
) : BookmarkService {
    private val store: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

    override suspend fun add(spotId: String): Long {
        val longId = spotId.toLongIdOrThrow()
        val count = bookmarkApi.addBookmark(longId).unwrap().bookmarkCount
        synchronized(store) { store.add(spotId) }
        return count
    }

    override suspend fun remove(spotId: String): Long {
        val longId = spotId.toLongIdOrThrow()
        val count = bookmarkApi.removeBookmark(longId).unwrap().bookmarkCount
        synchronized(store) { store.remove(spotId) }
        return count
    }

    override suspend fun isBookmarked(spotId: String): Boolean = store.contains(spotId)

    override suspend fun toggle(spotId: String): Boolean {
        // 캐시 기준 분기: 캐시에 있으면 remove, 없으면 add. add/remove가 캐시 갱신을 담당.
        return if (isBookmarked(spotId)) {
            remove(spotId)
            false
        } else {
            add(spotId)
            true
        }
    }

    override suspend fun bookmarkedIds(): Set<String> = synchronized(store) { store.toSet() }

    private fun String.toLongIdOrThrow(): Long = toLongOrNull()
        ?: throw IllegalArgumentException("spotId는 정수여야 합니다: $this")
}
