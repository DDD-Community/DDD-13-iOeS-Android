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
     * 나만의 스팟 등록. TODO(BE confirm): part 이름(`image`/`meta`)은 추정값.
     * 실제 BE 컨트롤러 시그니처에 맞춰 조정 필요.
     */
    @Multipart
    @POST("v1/users/me/my-spots")
    suspend fun createMySpot(
        @Part image: MultipartBody.Part,
        @Part meta: MultipartBody.Part,
    ): ApiResponse<CreateMySpotResponseDto>
}
