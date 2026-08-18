package com.pickflow.android.core.services.impl

import com.pickflow.android.core.network.api.BookmarkApi
import com.pickflow.android.core.network.mapper.toSavedSpotPage
import com.pickflow.android.core.network.unwrap
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.SavedSpotPage
import javax.inject.Inject
import javax.inject.Singleton

/** 북마크 지정/해제/조회 모두 서버가 단일 출처다. 로컬 캐시를 두지 않는다. */
@Singleton
class DefaultBookmarkService @Inject constructor(
    private val bookmarkApi: BookmarkApi,
) : BookmarkService {
    override suspend fun add(spotId: String): Long {
        val longId = spotId.toLongIdOrThrow()
        return bookmarkApi.addBookmark(longId).unwrap().bookmarkCount
    }

    override suspend fun remove(spotId: String): Long {
        val longId = spotId.toLongIdOrThrow()
        return bookmarkApi.removeBookmark(longId).unwrap().bookmarkCount
    }

    override suspend fun savedSpots(page: Int, coordinates: Coordinates?): SavedSpotPage =
        bookmarkApi.getSavedSpots(
            page = page,
            // 서버 검증: 위/경도 소수점 6자리까지 → truncate (7자리 GPS 시 400 방지).
            latitude = coordinates?.latitude?.toSixDecimal(),
            longitude = coordinates?.longitude?.toSixDecimal(),
        ).unwrap().toSavedSpotPage()

    private fun String.toLongIdOrThrow(): Long = toLongOrNull()
        ?: throw IllegalArgumentException("spotId는 정수여야 합니다: $this")
}
