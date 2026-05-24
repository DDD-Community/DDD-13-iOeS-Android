package com.pickflow.android.core.services.impl

import com.pickflow.android.core.services.protocols.BookmarkService
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InMemoryBookmarkService @Inject constructor() : BookmarkService {
    private val store: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())

    override suspend fun isBookmarked(spotId: String): Boolean = store.contains(spotId)

    override suspend fun toggle(spotId: String): Boolean = synchronized(store) {
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
