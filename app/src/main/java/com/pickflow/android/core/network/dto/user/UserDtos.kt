package com.pickflow.android.core.network.dto.user

import kotlinx.serialization.Serializable

@Serializable
data class MypageHomeResponseDto(
    val profileImageUrl: String? = null,
    val nickname: String = "",
    val savedSpotCount: Long = 0L,
    val recordedSpotCount: Long = 0L,
    /** 연결된 소셜 ("KAKAO" / "APPLE"). 서버 미제공 시 null. */
    val provider: String? = null,
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
