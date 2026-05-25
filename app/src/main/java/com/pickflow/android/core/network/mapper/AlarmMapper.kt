package com.pickflow.android.core.network.mapper

import com.pickflow.android.core.network.dto.alarm.SpotAlarmResponseDto
import com.pickflow.android.core.services.protocols.SpotAlarm

fun SpotAlarmResponseDto.toSpotAlarm(): SpotAlarm = SpotAlarm(
    spotId = spotId,
    enabled = enabled,
)
