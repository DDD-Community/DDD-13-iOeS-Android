package com.pickflow.android.core.services.protocols

interface SpotListService {
    suspend fun fetch(
        theme: SpotTheme?,
        cursor: String?,
        limit: Int,
    ): SpotPage
}
