package com.pickflow.android.core.network.api

import com.pickflow.android.core.network.ApiResponse
import com.pickflow.android.core.network.dto.myspot.MySpotListResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface MySpotApi {
    @GET("v1/users/me/my-spots")
    suspend fun getMySpots(
        @Query("page") page: Int? = null,
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null,
    ): ApiResponse<MySpotListResponseDto>
}
