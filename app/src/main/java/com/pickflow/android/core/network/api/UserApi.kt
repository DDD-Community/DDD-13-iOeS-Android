package com.pickflow.android.core.network.api

import com.pickflow.android.core.network.ApiResponse
import com.pickflow.android.core.network.dto.user.MypageHomeResponseDto
import com.pickflow.android.core.network.dto.user.UpdateProfileResponseDto
import com.pickflow.android.core.network.dto.user.WithdrawalReasonRequest
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query

interface UserApi {
    @GET("v1/users/me")
    suspend fun getMyPageHome(): ApiResponse<MypageHomeResponseDto>

    /**
     * 이미지 없는 경우 — Retrofit @Multipart는 최소 1개 part를 요구하므로 image 변경 없이
     * 닉네임만 바꿀 때는 multipart가 아닌 일반 PATCH로 분리.
     */
    @PATCH("v1/users/me")
    suspend fun updateProfileNoImage(
        @Query("nickname") nickname: String? = null,
    ): ApiResponse<UpdateProfileResponseDto>

    @Multipart
    @PATCH("v1/users/me")
    suspend fun updateProfileWithImage(
        @Query("nickname") nickname: String? = null,
        @Part profileImage: MultipartBody.Part,
    ): ApiResponse<UpdateProfileResponseDto>

    @DELETE("v1/users/me")
    suspend fun deleteAccount(): ApiResponse<Unit>

    @POST("v1/users/me/withdrawal-reason")
    suspend fun saveWithdrawalReason(
        @Body request: WithdrawalReasonRequest,
    ): ApiResponse<Unit>
}
