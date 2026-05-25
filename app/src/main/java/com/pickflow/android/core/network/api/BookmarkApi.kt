package com.pickflow.android.core.network.api

import com.pickflow.android.core.network.ApiResponse
import com.pickflow.android.core.network.dto.bookmark.BookmarkResponseDto
import retrofit2.http.POST
import retrofit2.http.Path

interface BookmarkApi {
    @POST("v1/spots/{spotId}/bookmarks")
    suspend fun addBookmark(@Path("spotId") spotId: Long): ApiResponse<BookmarkResponseDto>
}
