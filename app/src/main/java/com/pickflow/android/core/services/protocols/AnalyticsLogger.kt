package com.pickflow.android.core.services.protocols

interface AnalyticsLogger {
    suspend fun log(event: String, params: Map<String, Any?> = emptyMap())
}
