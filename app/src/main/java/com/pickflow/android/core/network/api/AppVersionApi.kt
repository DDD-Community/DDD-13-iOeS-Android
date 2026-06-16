package com.pickflow.android.core.network.api

import com.pickflow.android.core.network.ApiResponse
import com.pickflow.android.core.network.dto.appversion.AppVersionPolicyDto
import retrofit2.http.GET

/** iOS `AppVersionEndpoint` 1:1. Android 는 `/v1/app/config/android`. */
interface AppVersionApi {
    @GET("v1/app/config/android")
    suspend fun getAndroidVersionPolicy(): ApiResponse<AppVersionPolicyDto>
}
