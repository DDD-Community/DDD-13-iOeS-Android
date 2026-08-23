package com.pickflow.android.core.network.api

import com.pickflow.android.core.network.ApiResponse
import com.pickflow.android.core.network.dto.spot.SpotDetailResponseDto
import com.pickflow.android.core.network.dto.spot.SpotListResponseDto
import com.pickflow.android.core.network.dto.spot.SpotPreviewResponseDto
import com.pickflow.android.core.network.dto.spot.SpotViewportResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface SpotApi {

    /**
     * @param theme 다중 필터 — Retrofit 이 `?theme=A&theme=B` 반복 파라미터로 직렬화한다.
     *   null/빈 리스트면 파라미터 자체가 붙지 않는다(= 전체 조회).
     *   반복 파라미터 형식은 PV-59 백엔드 확정시 변경 가능성 있음(CSV 가능성).
     */
    @GET("v1/spots")
    suspend fun getSpots(
        @Query("page") page: Int? = null,
        @Query("theme") theme: List<String>? = null,
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null,
        @Query("sort") sort: String? = null,
    ): ApiResponse<SpotListResponseDto>

    @GET("v1/spots/{spotId}")
    suspend fun getSpotDetail(@Path("spotId") spotId: Long): ApiResponse<SpotDetailResponseDto>

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
        @Query("theme") theme: List<String>? = null,
    ): ApiResponse<SpotViewportResponseDto>
}
