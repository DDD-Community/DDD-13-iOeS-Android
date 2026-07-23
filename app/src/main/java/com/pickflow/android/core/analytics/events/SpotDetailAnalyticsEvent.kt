package com.pickflow.android.core.analytics.events

import com.pickflow.android.core.analytics.AnalyticsEvent

/** iOS `SpotDetailAnalyticsEvent` 1:1 — 스팟 상세 화면 이벤트. */
enum class SpotDetailAnalyticsEvent(override val eventName: String) : AnalyticsEvent {
    /** 스팟 상세 상단(X 옆) share 버튼 탭. */
    SHARE_BUTTON_TAP("spot_detail_share_btn_tap"),
}
