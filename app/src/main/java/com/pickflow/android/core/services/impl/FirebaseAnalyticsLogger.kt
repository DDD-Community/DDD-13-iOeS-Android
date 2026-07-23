package com.pickflow.android.core.services.impl

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.pickflow.android.core.analytics.AnalyticsEvent
import com.pickflow.android.core.services.protocols.AnalyticsLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** iOS `FirebaseAnalyticsLogger` 1:1 — Firebase Analytics 로 이벤트 전송. */
@Singleton
class FirebaseAnalyticsLogger @Inject constructor(
    @ApplicationContext context: Context,
) : AnalyticsLogger {

    private val firebaseAnalytics: FirebaseAnalytics = FirebaseAnalytics.getInstance(context)

    override fun log(event: AnalyticsEvent) {
        firebaseAnalytics.logEvent(event.eventName, event.parameters?.toBundle())
    }

    private fun Map<String, Any?>.toBundle(): Bundle = Bundle().apply {
        forEach { (key, value) ->
            when (value) {
                null -> Unit
                is String -> putString(key, value)
                is Int -> putInt(key, value)
                is Long -> putLong(key, value)
                is Double -> putDouble(key, value)
                is Boolean -> putBoolean(key, value)
                else -> putString(key, value.toString())
            }
        }
    }
}
