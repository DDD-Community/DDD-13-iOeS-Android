package com.pickflow.android.core.services.stub

import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.SavedSpotPage
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StubBookmarkService @Inject constructor(
    private val backend: StubSpotBackend,
) : BookmarkService {
    override suspend fun add(spotId: String): Long = backend.addBookmark(spotId.asLongId())

    override suspend fun remove(spotId: String): Long = backend.removeBookmark(spotId.asLongId())

    override suspend fun savedSpots(page: Int, coordinates: Coordinates?): SavedSpotPage =
        backend.savedSpots(page, coordinates)

    override suspend fun isBookmarked(spotId: String): Boolean = backend.isBookmarked(spotId.asLongId())

    override suspend fun toggle(spotId: String): Boolean = if (isBookmarked(spotId)) {
        remove(spotId)
        false
    } else {
        add(spotId)
        true
    }

    override suspend fun bookmarkedIds(): Set<String> = backend.bookmarkedIds()

    private fun String.asLongId(): Long = toLongOrNull()
        ?: throw IllegalArgumentException("spotId must be numeric: $this")
}
