package com.pickflow.android.core.services.protocols

import com.pickflow.android.core.analytics.AnalyticsEvent

/** iOS `AnalyticsLoggerProtocol` 1:1 — GA 이벤트 로깅 단일 추상화. */
interface AnalyticsLogger {
    fun log(event: AnalyticsEvent)
}
