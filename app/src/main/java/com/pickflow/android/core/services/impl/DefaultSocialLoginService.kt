package com.pickflow.android.core.services.impl

import com.pickflow.android.core.network.api.AuthApi
import com.pickflow.android.core.network.dto.auth.KakaoLoginRequest
import com.pickflow.android.core.network.mapper.toAuthenticatedSession
import com.pickflow.android.core.network.unwrap
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
            // TODO(Phase B/apple): /v1/auth/apple 연동 시 교체.
            SocialProvider.APPLE -> error("Apple 로그인은 다음 iteration에서 구현됩니다.")
        }
        tokenStore.save(session.tokens.accessToken, session.tokens.refreshToken)
        return session
    }
}
