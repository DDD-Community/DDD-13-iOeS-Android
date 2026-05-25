package com.pickflow.android.core.network.api

import com.pickflow.android.core.network.ApiResponse
import com.pickflow.android.core.network.dto.auth.KakaoLoginRequest
import com.pickflow.android.core.network.dto.auth.TokenResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    // TODO(BE confirm): 스펙상 Bearer 필요로 표기됐으나 관례상 미필요. 헤더 없이 호출하도록
    //  AuthInterceptor가 토큰 없을 때 알아서 부착 안 함.
    @POST("v1/auth/kakao")
    suspend fun kakaoLogin(@Body request: KakaoLoginRequest): ApiResponse<TokenResponseDto>
}
