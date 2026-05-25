package com.pickflow.android.core.network.api

import com.pickflow.android.core.network.ApiResponse
import com.pickflow.android.core.network.dto.user.MypageHomeResponseDto
import retrofit2.http.GET

interface UserApi {
    @GET("v1/users/me")
    suspend fun getMyPageHome(): ApiResponse<MypageHomeResponseDto>
}
