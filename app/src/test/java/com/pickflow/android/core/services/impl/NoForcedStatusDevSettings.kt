package com.pickflow.android.core.services.impl

import com.pickflow.android.core.services.protocols.ApiEnvironment
import com.pickflow.android.core.services.protocols.DevSettings
import com.pickflow.android.core.services.protocols.MySpotStatus
import kotlinx.coroutines.flow.MutableStateFlow

/** 상태 강제를 쓰지 않는 DevSettings — 서버 응답을 그대로 통과시킨다. */
internal class NoForcedStatusDevSettings : DevSettings {
    override val apiEnvironment = MutableStateFlow(ApiEnvironment.DEFAULT)
    override val badgeEnabled = MutableStateFlow(false)
    override val touchIndicatorEnabled = MutableStateFlow(false)
    override val forcedMySpotStatus = MutableStateFlow<MySpotStatus?>(null)
    override fun setApiEnvironment(environment: ApiEnvironment) = Unit
    override fun setBadgeEnabled(enabled: Boolean) = Unit
    override fun setTouchIndicatorEnabled(enabled: Boolean) = Unit
    override fun setForcedMySpotStatus(status: MySpotStatus?) = Unit
}
