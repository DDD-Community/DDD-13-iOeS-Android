package com.pickflow.android.core.network.dto.alarm

import kotlinx.serialization.Serializable

@Serializable
data class SpotAlarmResponseDto(
    val spotId: Long = 0L,
    val enabled: Boolean = false,
)

@Serializable
data class UpdateSpotAlarmRequest(
    val enabled: Boolean,
)
