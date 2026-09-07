package com.pickflow.android.core.network.api

import com.pickflow.android.core.network.ApiResponse
import com.pickflow.android.core.network.dto.address.AddressSearchResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface AddressApi {

    /** 주소 검색. 인증 불필요(swagger `security: []`). `size` 최대 30. */
    @GET("v1/address/search")
    suspend fun searchAddress(
        @Query("query") query: String,
        @Query("size") size: Int,
    ): ApiResponse<AddressSearchResponseDto>
}
