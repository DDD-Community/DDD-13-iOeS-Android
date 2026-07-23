package com.pickflow.android.core.analytics

/** iOS `AnalyticsEvent` 1:1 — GA 이벤트 이름과 선택적 파라미터를 노출한다. */
interface AnalyticsEvent {
    val eventName: String
    val parameters: Map<String, Any?>? get() = null
}
