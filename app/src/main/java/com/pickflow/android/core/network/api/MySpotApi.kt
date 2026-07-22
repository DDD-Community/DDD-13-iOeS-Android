package com.pickflow.android.core.network.api

import com.pickflow.android.core.network.ApiResponse
import com.pickflow.android.core.network.dto.myspot.CreateMySpotResponseDto
import com.pickflow.android.core.network.dto.myspot.MySpotListResponseDto
import okhttp3.MultipartBody
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface MySpotApi {
    @GET("v1/users/me/my-spots")
    suspend fun getMySpots(
        @Query("page") page: Int? = null,
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null,
    ): ApiResponse<MySpotListResponseDto>

    /**
     * 나만의 스팟 등록. part 이름은 `image` + `request`(JSON 메타) —
     * iOS `SpotService.registerSpot` 과 동일(BE 검증 완료 형태).
     */
    @Multipart
    @POST("v1/users/me/my-spots")
    suspend fun createMySpot(
        @Part image: MultipartBody.Part,
        @Part meta: MultipartBody.Part,
    ): ApiResponse<CreateMySpotResponseDto>
}
