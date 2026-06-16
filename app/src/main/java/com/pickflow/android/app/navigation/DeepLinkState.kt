package com.pickflow.android.app.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS `DeepLinkRouter.pendingSpotId` 1:1.
 * MainActivity 의 intent 핸들러가 set, [PickflowNavHost] 가 consume 후 [clear] 한다.
 */
object DeepLinkState {
    private val _pendingSpotId = MutableStateFlow<Long?>(null)
    val pendingSpotId: StateFlow<Long?> = _pendingSpotId.asStateFlow()

    fun setPendingSpotId(id: Long) { _pendingSpotId.value = id }
    fun clear() { _pendingSpotId.value = null }
}
