package com.pickflow.android.core.services.impl

import com.pickflow.android.core.network.api.MySpotAlarmApi
import com.pickflow.android.core.network.dto.alarm.UpdateSpotAlarmRequest
import com.pickflow.android.core.network.mapper.toSpotAlarm
import com.pickflow.android.core.network.unwrap
import com.pickflow.android.core.services.protocols.MySpotAlarmService
import com.pickflow.android.core.services.protocols.SpotAlarm
import javax.inject.Inject

class DefaultMySpotAlarmService @Inject constructor(
    private val mySpotAlarmApi: MySpotAlarmApi,
) : MySpotAlarmService {
    override suspend fun get(spotId: Long): SpotAlarm =
        mySpotAlarmApi.getAlarm(spotId).unwrap().toSpotAlarm()

    override suspend fun update(spotId: Long, enabled: Boolean): SpotAlarm =
        mySpotAlarmApi.updateAlarm(spotId, UpdateSpotAlarmRequest(enabled = enabled))
            .unwrap()
            .toSpotAlarm()
}
