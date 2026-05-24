package com.pickflow.android.core.services.impl

import com.pickflow.android.core.services.protocols.AnalyticsLogger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoopAnalyticsLogger @Inject constructor() : AnalyticsLogger {
    override suspend fun log(event: String, params: Map<String, Any?>) = Unit
}
