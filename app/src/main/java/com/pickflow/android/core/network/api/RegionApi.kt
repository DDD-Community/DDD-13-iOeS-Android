package com.pickflow.android.core.network.api

import com.pickflow.android.core.network.ApiResponse
import com.pickflow.android.core.network.dto.region.RegionListResponseDto
import retrofit2.http.GET

interface RegionApi {

    /** 활성화된 지역 목록. 인증 불필요(swagger `security: []`). */
    @GET("v1/regions")
    suspend fun getActiveRegions(): ApiResponse<RegionListResponseDto>
}
