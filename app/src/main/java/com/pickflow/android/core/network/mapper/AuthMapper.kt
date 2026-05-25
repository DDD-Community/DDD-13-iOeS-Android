package com.pickflow.android.core.network.mapper

import com.pickflow.android.core.network.dto.auth.TokenResponseDto
import com.pickflow.android.core.network.dto.auth.UserProfileDto
import com.pickflow.android.core.services.protocols.AuthenticatedSession
import com.pickflow.android.core.services.protocols.SessionTokens
import com.pickflow.android.core.services.protocols.SocialProvider
import com.pickflow.android.core.services.protocols.UserProfile

fun TokenResponseDto.toAuthenticatedSession(): AuthenticatedSession = AuthenticatedSession(
    tokens = SessionTokens(
        accessToken = accessToken,
        refreshToken = refreshToken,
    ),
    profile = profile.toUserProfile(),
)

fun UserProfileDto.toUserProfile(): UserProfile = UserProfile(
    userId = userId,
    email = email?.takeIf { it.isNotBlank() },
    nickname = nickname,
    profileImageUrl = profileImageUrl?.takeIf { it.isNotBlank() },
    provider = when (provider.uppercase()) {
        "APPLE" -> SocialProvider.APPLE
        else -> SocialProvider.KAKAO
    },
)
