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
     * 프로필 부분 수정 — iOS `UserService.updateProfile` 1:1: multipart PATCH.
     * `nickname`(텍스트 part) / `profileImage`(파일 part) 중 변경분만 담는다.
     * (기존 쿼리 파라미터 방식은 서버가 인식하지 못해 닉네임 저장이 무시되던 원인.)
     */
    @Multipart
    @PATCH("v1/users/me")
    suspend fun updateProfile(
        @Part parts: List<MultipartBody.Part>,
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
