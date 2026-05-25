package com.pickflow.android.core.network.dto.auth

import kotlinx.serialization.Serializable

@Serializable
data class KakaoLoginRequest(
    val accessToken: String,
)

@Serializable
data class AppleLoginRequest(
    val identityToken: String,
    val user: AppleUserDto? = null,
)

@Serializable
data class AppleUserDto(
    val name: AppleNameDto? = null,
    val email: String? = null,
)

@Serializable
data class AppleNameDto(
    val firstName: String? = null,
    val lastName: String? = null,
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
