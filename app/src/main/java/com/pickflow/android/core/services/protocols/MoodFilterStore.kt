package com.pickflow.android.core.services.protocols

import kotlinx.coroutines.flow.StateFlow

/**
 * 탐색 탭(지도 ↔ 리스트)이 **공유하는** 무드 필터 선택 상태.
 *
 * 지도와 리스트는 별개의 ViewModel 이지만 사용자에게는 같은 화면의 두 모드다.
 * 한쪽에서 고른 무드가 다른 쪽에도 그대로 적용돼야 하므로 선택 상태를
 * ViewModel 밖(프로세스 단일 인스턴스)으로 끌어올린다.
 *
 * - **빈 Set = 필터 없음(전체 조회)**. "빈 결과"가 아니다.
 * - 스팟 등록 폼은 이 상태를 쓰지 않는다(단독 선택이며 목적이 다르다).
 */
interface MoodFilterStore {

    /** 현재 선택된 무드. 빈 Set 이면 전체. */
    val selected: StateFlow<Set<SpotTheme>>

    /** 이미 선택돼 있으면 그 하나만 해제하고, 아니면 추가한다. */
    fun toggle(theme: SpotTheme)

    /** 전체 해제 — 필터 없음 상태로 되돌린다. */
    fun clear()
}
