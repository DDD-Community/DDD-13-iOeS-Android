package com.pickflow.android.core.network.api

import com.pickflow.android.core.network.ApiResponse
import com.pickflow.android.core.network.dto.bookmark.BookmarkResponseDto
import com.pickflow.android.core.network.dto.bookmark.SavedSpotListResponseDto
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface BookmarkApi {
    @POST("v1/spots/{spotId}/bookmarks")
    suspend fun addBookmark(@Path("spotId") spotId: Long): ApiResponse<BookmarkResponseDto>

    @DELETE("v1/spots/{spotId}/bookmarks")
    suspend fun removeBookmark(@Path("spotId") spotId: Long): ApiResponse<BookmarkResponseDto>

    @GET("v1/users/me/saved-spots")
    suspend fun getSavedSpots(
        @Query("page") page: Int? = null,
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null,
    ): ApiResponse<SavedSpotListResponseDto>
}
