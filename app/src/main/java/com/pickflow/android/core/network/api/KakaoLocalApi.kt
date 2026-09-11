package com.pickflow.android.core.network.api

import com.pickflow.android.core.network.dto.kakao.KakaoCoord2AddressResponse
import com.pickflow.android.core.network.dto.kakao.KakaoLocalSearchResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * iOS `AddressService` 가 직접 호출하는 Kakao Local API 1:1.
 * Base URL: `https://dapi.kakao.com/`. 인증 헤더 형식: `KakaoAK {KAKAO_REST_API_KEY}`.
 */
interface KakaoLocalApi {
    @GET("v2/local/search/keyword.json")
    suspend fun searchKeyword(
        @Header("Authorization") auth: String,
        @Query("query") query: String,
    ): KakaoLocalSearchResponse

    @GET("v2/local/geo/coord2address.json")
    suspend fun coord2Address(
        @Header("Authorization") auth: String,
        @Query("x") longitude: String,
        @Query("y") latitude: String,
    ): KakaoCoord2AddressResponse
}
