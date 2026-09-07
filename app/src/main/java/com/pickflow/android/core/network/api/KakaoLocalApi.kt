package com.pickflow.android.core.network.api

import com.pickflow.android.core.network.dto.kakao.KakaoCoord2AddressResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * 좌표→주소(coord2address) 전용. 키워드 검색은 서버 `/v1/address/search` 로 옮겼다.
 * Base URL: `https://dapi.kakao.com/`. 인증 헤더 형식: `KakaoAK {KAKAO_REST_API_KEY}`.
 */
interface KakaoLocalApi {
    @GET("v2/local/geo/coord2address.json")
    suspend fun coord2Address(
        @Header("Authorization") auth: String,
        @Query("x") longitude: String,
        @Query("y") latitude: String,
    ): KakaoCoord2AddressResponse
}
