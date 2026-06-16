package com.pickflow.android.core.network.api

import com.pickflow.android.core.network.ApiResponse
import com.pickflow.android.core.network.dto.appversion.AppVersionPolicyDto
import retrofit2.http.GET

/**
 * iOS `AppVersionEndpoint` 1:1.
 *
 * TODO: `/v1/app/config/android` 미오픈 → 임시로 `/v1/app/config/ios` 사용(약관/정책 수신용).
 *   서버에서 android config 가 열리면 아래 ios 호출을 android 로 되돌린다.
 */
interface AppVersionApi {
    // @GET("v1/app/config/android")
    // suspend fun getAndroidVersionPolicy(): ApiResponse<AppVersionPolicyDto>

    @GET("v1/app/config/ios")
    suspend fun getIosVersionPolicy(): ApiResponse<AppVersionPolicyDto>
}
