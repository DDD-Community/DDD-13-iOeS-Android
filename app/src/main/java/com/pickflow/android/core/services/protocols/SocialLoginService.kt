package com.pickflow.android.core.services.protocols

enum class SocialProvider { KAKAO, APPLE }

data class SocialAuthCredential(
    val provider: SocialProvider,
    val providerAccessToken: String,
    val providerRefreshToken: String?,
)

data class SessionTokens(
    val accessToken: String,
    val refreshToken: String?,
)

interface SocialLoginService {
    suspend fun loginWith(credential: SocialAuthCredential): SessionTokens
}
