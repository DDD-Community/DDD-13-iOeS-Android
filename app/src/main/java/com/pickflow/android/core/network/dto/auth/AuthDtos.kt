package com.pickflow.android.core.network.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class KakaoLoginRequest(
    val accessToken: String,
)

@Serializable
data class TokenResponseDto(
    val accessToken: String = "",
    val refreshToken: String? = null,
    val profile: UserProfileDto = UserProfileDto(),
)

@Serializable
data class UserProfileDto(
    val userId: String = "",
    val email: String? = null,
    val nickname: String = "",
    val profileImageUrl: String? = null,
    val provider: String = "KAKAO",
)
