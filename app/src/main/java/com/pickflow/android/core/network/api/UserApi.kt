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

    /**
     * 탈퇴 계정 복구. 본인 인증 없이 restoreToken 만으로 호출하므로 Bearer 헤더는
     * AuthInterceptor가 토큰 없을 때 자동 미부착. 복구 성공 후 소셜 로그인 재시도.
     */
    @PATCH("v1/users/restore")
    suspend fun restoreAccount(
        @Query("restoreToken") restoreToken: String,
    ): ApiResponse<Unit>
}
