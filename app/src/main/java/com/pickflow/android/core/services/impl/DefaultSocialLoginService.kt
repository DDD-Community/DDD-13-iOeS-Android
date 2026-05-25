package com.pickflow.android.core.services.impl

import com.pickflow.android.core.network.api.AuthApi
import com.pickflow.android.core.network.dto.auth.AppleLoginRequest
import com.pickflow.android.core.network.dto.auth.AppleNameDto
import com.pickflow.android.core.network.dto.auth.AppleUserDto
import com.pickflow.android.core.network.dto.auth.KakaoLoginRequest
import com.pickflow.android.core.network.mapper.toAuthenticatedSession
import com.pickflow.android.core.network.unwrap
import com.pickflow.android.core.services.protocols.AppleAuthUser
import com.pickflow.android.core.services.protocols.AuthenticatedSession
import com.pickflow.android.core.services.protocols.SocialAuthCredential
import com.pickflow.android.core.services.protocols.SocialLoginService
import com.pickflow.android.core.services.protocols.SocialProvider
import com.pickflow.android.core.services.protocols.TokenStore
import javax.inject.Inject

class DefaultSocialLoginService @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStore: TokenStore,
) : SocialLoginService {
    override suspend fun loginWith(credential: SocialAuthCredential): AuthenticatedSession {
        val session = when (credential.provider) {
            SocialProvider.KAKAO -> authApi
                .kakaoLogin(KakaoLoginRequest(accessToken = credential.providerAccessToken))
                .unwrap()
                .toAuthenticatedSession()
            SocialProvider.APPLE -> authApi
                .appleLogin(
                    AppleLoginRequest(
                        identityToken = credential.providerAccessToken,
                        user = credential.appleUser?.toDto(),
                    )
                )
                .unwrap()
                .toAuthenticatedSession()
        }
        tokenStore.save(session.tokens.accessToken, session.tokens.refreshToken)
        return session
    }
}

private fun AppleAuthUser.toDto(): AppleUserDto = AppleUserDto(
    name = if (firstName != null || lastName != null) {
        AppleNameDto(firstName = firstName, lastName = lastName)
    } else null,
    email = email,
)
