package com.pickflow.android.core.services.protocols

/**
 * 활성화된 지역 목록의 출처. 서버 `GET /v1/regions` + 로컬 캐시.
 *
 * 지역은 거의 바뀌지 않아 매번 네트워크를 기다릴 이유가 없다. 구현은 캐시를 먼저 돌려주고
 * 뒤에서 서버 값으로 갱신한다.
 */
interface RegionCatalog {

    /** 캐시된 목록. 캐시가 없으면 빈 리스트. */
    suspend fun cached(): List<Region>

    /** 서버에서 받아 캐시에 반영한다. 실패하면 null. */
    suspend fun refresh(): List<Region>?
}
