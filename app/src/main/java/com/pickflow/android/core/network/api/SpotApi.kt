package com.pickflow.android.core.network.api

import com.pickflow.android.core.network.ApiResponse
import com.pickflow.android.core.network.dto.spot.SpotListResponseDto
import com.pickflow.android.core.network.dto.spot.SpotViewportResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface SpotApi {

    @GET("v1/spots")
    suspend fun getSpots(
        @Query("page") page: Int? = null,
        @Query("theme") theme: String? = null,
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null,
        @Query("sort") sort: String? = null,
    ): ApiResponse<SpotListResponseDto>

    @GET("v1/spots/viewport")
    suspend fun getSpotsInViewport(
        @Query("topLeftLat") topLeftLat: Double,
        @Query("topLeftLng") topLeftLng: Double,
        @Query("topRightLat") topRightLat: Double,
        @Query("topRightLng") topRightLng: Double,
        @Query("bottomLeftLat") bottomLeftLat: Double,
        @Query("bottomLeftLng") bottomLeftLng: Double,
        @Query("bottomRightLat") bottomRightLat: Double,
        @Query("bottomRightLng") bottomRightLng: Double,
        @Query("theme") theme: String? = null,
    ): ApiResponse<SpotViewportResponseDto>
}
