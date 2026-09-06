package com.pickflow.android.core.services.protocols

import kotlinx.coroutines.flow.StateFlow

/**
 * 탐색 탭(지도 ↔ 리스트)이 **공유하는** 지역 선택 상태. [MoodFilterStore] 와 같은 이유로
 * ViewModel 밖에 둔다 — 지도에서 지역을 바꾸면 리스트도 같은 지역을 봐야 한다.
 *
 * 무드와 달리 "선택 없음"이 없다. 서버가 `regionId` 를 필수로 요구하므로 항상 하나가
 * 적용돼 있다(초기값 서울).
 */
interface RegionStore {

    /** 현재 적용 중인 지역. 바텀시트의 [적용하기] 로만 바뀐다. */
    val selected: StateFlow<Region>

    /**
     * 바텀시트에 띄울 지역 목록. [RegionCatalog] 가 준다(캐시 → 서버 순으로 반영).
     * [refreshAvailable] 전(그리고 호출이 실패했을 때)에는 [Region.FALLBACK] 이다.
     */
    val available: StateFlow<List<Region>>

    fun select(region: Region)

    /** 캐시와 서버 목록으로 [available] 을 갱신한다. 둘 다 없으면 조용히 직전 값을 유지한다. */
    suspend fun refreshAvailable()
}
