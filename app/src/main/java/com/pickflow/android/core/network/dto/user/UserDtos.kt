package com.pickflow.android.core.network.dto.user

import kotlinx.serialization.Serializable

@Serializable
data class MypageHomeResponseDto(
    val profileImageUrl: String? = null,
    val nickname: String = "",
    val savedSpotCount: Long = 0L,
    val recordedSpotCount: Long = 0L,
)

@Serializable
data class UpdateProfileResponseDto(
    val displayName: String = "",
    val profileImageUrl: String? = null,
)

@Serializable
data class WithdrawalReasonRequest(
    val reasonType: String,
    val content: String? = null,
)
