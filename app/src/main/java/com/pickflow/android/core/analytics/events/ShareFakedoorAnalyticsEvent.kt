package com.pickflow.android.core.analytics.events

import com.pickflow.android.core.analytics.AnalyticsEvent

/** iOS `ShareFakedoorAnalyticsEvent` 1:1 — "나만의 스팟 오픈" fakedoor 모달 이벤트. */
enum class ShareFakedoorAnalyticsEvent(override val eventName: String) : AnalyticsEvent {
    /** "업데이트 소식 받기" 버튼 탭. */
    NOTIFY_BUTTON_TAP("modal_share_fakedoor_btn_tap"),
}
