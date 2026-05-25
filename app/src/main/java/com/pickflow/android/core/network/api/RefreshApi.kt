package com.pickflow.android.core.network.api

import com.pickflow.android.core.network.ApiResponse
import com.pickflow.android.core.network.dto.auth.RefreshRequest
import com.pickflow.android.core.network.dto.auth.TokenResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 토큰 갱신 전용 API. TokenAuthenticator가 401 응답 시 호출한다.
 *
 * AuthApi와 분리한 이유:
 * - 일반 OkHttpClient는 AuthInterceptor + TokenAuthenticator를 사용. refresh 요청이
 *   여기로 흐르면 만료된 access token이 부착되거나 401 → refresh → 401 → ...의 사이클이
 *   생길 수 있음.
 * - RefreshApi는 인증/재시도 인터셉터가 빠진 별도 OkHttpClient로 제공된다
 *   (NetworkModule.provideRefreshOkHttp 참고).
 */
interface RefreshApi {
    @POST("v1/auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): ApiResponse<TokenResponseDto>
}
