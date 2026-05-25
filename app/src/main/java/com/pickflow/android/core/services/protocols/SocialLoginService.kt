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

data class UserProfile(
    val userId: String,
    val email: String?,
    val nickname: String,
    val profileImageUrl: String?,
    val provider: SocialProvider,
)

data class AuthenticatedSession(
    val tokens: SessionTokens,
    val profile: UserProfile,
)

interface SocialLoginService {
    suspend fun loginWith(credential: SocialAuthCredential): AuthenticatedSession
}
