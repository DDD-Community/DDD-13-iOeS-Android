package com.pickflow.android.core.services.protocols

enum class SocialProvider {
    KAKAO, APPLE;

    /** 연결된 소셜 표기용 한글 이름. iOS `SocialProvider.displayName` 1:1. */
    val displayName: String
        get() = when (this) {
            KAKAO -> "카카오"
            APPLE -> "애플"
        }

    companion object {
        fun fromRaw(raw: String?): SocialProvider? = when (raw?.uppercase()) {
            "KAKAO" -> KAKAO
            "APPLE" -> APPLE
            else -> null
        }
    }
}

// AppleAuthUser는 AppleAuthProvider.kt에서 선언됨

data class SocialAuthCredential(
    val provider: SocialProvider,
    val providerAccessToken: String,
    val providerRefreshToken: String?,
    /**
     * Apple 최초 로그인 시에만 비어 있지 않음 (이름/이메일).
     * KAKAO provider에서는 항상 null.
     */
    val appleUser: AppleAuthUser? = null,
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
