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
     * @param regionId 지역 필터. **서버 필수 파라미터** — 빠지면 C003 으로 실패한다.
     *   타입은 서버상 List 라 `?regionId=1&regionId=2` 다중 지정도 되지만, 앱은 단일 선택만 쓴다.
     * @param theme 다중 필터 — Retrofit 이 `?theme=A&theme=B` 반복 파라미터로 직렬화한다.
     *   null/빈 리스트면 파라미터 자체가 붙지 않는다(= 전체 조회).
     */
    @GET("v1/spots")
    suspend fun getSpots(
        @Query("regionId") regionId: Long,
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
        @Query("regionId") regionId: Long,
        @Query("theme") theme: List<String>? = null,
    ): ApiResponse<SpotViewportResponseDto>
}
