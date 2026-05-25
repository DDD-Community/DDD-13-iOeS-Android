package com.pickflow.android.core.network.api

import com.pickflow.android.core.network.ApiResponse
import com.pickflow.android.core.network.dto.alarm.SpotAlarmResponseDto
import com.pickflow.android.core.network.dto.alarm.UpdateSpotAlarmRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface MySpotAlarmApi {
    @GET("v1/users/me/my-spots/{spotId}/alarm")
    suspend fun getAlarm(@Path("spotId") spotId: Long): ApiResponse<SpotAlarmResponseDto>

    @PUT("v1/users/me/my-spots/{spotId}/alarm")
    suspend fun updateAlarm(
        @Path("spotId") spotId: Long,
        @Body request: UpdateSpotAlarmRequest,
    ): ApiResponse<SpotAlarmResponseDto>
}
