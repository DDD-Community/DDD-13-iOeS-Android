package com.pickflow.android.core.services.protocols

/**
 * "스팟 공개 OPEN!" 안내 바텀시트를 **이 계정이** 이미 봤는지.
 *
 * [V2NoticeStore] 와 다른 점이 둘 있다.
 * - **기기가 아니라 계정 단위다.** 같은 기기를 두 사람이 쓰면 각자 한 번씩 봐야 한다.
 * - **로그인이 본질적 전제다.** 안내하는 대상이 "내 스팟" 이라 비로그인에는 띄울 것 자체가 없다.
 *   (V2 안내는 반대다 — 게스트도 받아야 한다. PV-79 §6)
 */
interface SpotOpenGuideStore {
    /** 이미 봤으면 true, 아직이면 false, **비로그인이면 null**(노출 판정 대상이 아니다). */
    suspend fun hasSeen(): Boolean?

    /** 비로그인이면 아무것도 하지 않는다. */
    suspend fun markSeen()
}
