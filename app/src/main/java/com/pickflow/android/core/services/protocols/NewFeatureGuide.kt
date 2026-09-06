package com.pickflow.android.core.services.protocols

import kotlinx.coroutines.flow.StateFlow

/**
 * "이 신규 기능 안내를 지금 띄워도 되는가" 를 판정한다 — 노출 여부는 서버(Remote Config)가 정한다.
 *
 * "봤는지" 는 여기 없다. 그건 기능마다 다르게 저장돼서(예: [V2NoticeStore] 는 기기 단위)
 * 각 화면의 ViewModel 이 `isActive && !seen` 으로 합친다.
 */
interface NewFeatureGuide {
    /**
     * 마지막으로 받아온(또는 캐시된) 설정. 최초 fetch 전에는 캐시가 없으면 null 이다.
     *
     * [refresh] 가 끝나면 값이 바뀌므로, 이걸 구독하면 fetch 완료 시 노출 상태가 자연히 재평가된다.
     */
    val config: StateFlow<NewFeatureConfig?>

    /**
     * [key] 가 [now] 시점에 노출 대상인지. 설정에 없는 key 는 항상 false 다.
     *
     * 유저 기준 판정(`startAt` 없음)이면 **최초 호출 시각을 여기서 심는다** — 최초 "노출" 이
     * 아니라 최초 "판정" 시각이 기준이다(iOS 와 동일).
     */
    fun isActive(key: String, now: Long = System.currentTimeMillis()): Boolean

    /** 원격 설정을 새로 받아온다. 실패는 삼키고 마지막 캐시를 그대로 둔다(오프라인에서도 판정은 돈다). */
    suspend fun refresh()
}
