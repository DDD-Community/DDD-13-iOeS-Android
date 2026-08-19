package com.pickflow.android.core.network.api

import com.pickflow.android.core.network.ApiResponse
import com.pickflow.android.core.network.dto.spot.SpotDetailResponseDto
import com.pickflow.android.core.network.dto.spot.SpotListResponseDto
import com.pickflow.android.core.network.dto.spot.SpotPreviewResponseDto
import com.pickflow.android.core.network.dto.spot.SpotViewportResponseDto
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
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

    @GET("v1/spots/{spotId}")
    suspend fun getSpotDetail(@Path("spotId") spotId: Long): ApiResponse<SpotDetailResponseDto>

    // 응답 body 는 성공 여부만 쓰므로 Unit 으로 받는다(서버가 어떤 data 를 주든 무시).
    @POST("v1/spots/{spotId}/likes")
    suspend fun addLike(@Path("spotId") spotId: Long): ApiResponse<Unit>

    @DELETE("v1/spots/{spotId}/likes")
    suspend fun removeLike(@Path("spotId") spotId: Long): ApiResponse<Unit>

    @GET("v1/spots/{spotId}/preview")
    suspend fun getSpotPreview(
        @Path("spotId") spotId: Long,
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null,
    ): ApiResponse<SpotPreviewResponseDto>

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
