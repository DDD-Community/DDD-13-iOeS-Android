package com.pickflow.android.core.services.protocols

interface BookmarkService {
    suspend fun isBookmarked(spotId: String): Boolean
    suspend fun toggle(spotId: String): Boolean
    suspend fun bookmarkedIds(): Set<String>
}
