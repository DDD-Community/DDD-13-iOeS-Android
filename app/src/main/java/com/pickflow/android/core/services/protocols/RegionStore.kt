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

    fun select(region: Region)
}
