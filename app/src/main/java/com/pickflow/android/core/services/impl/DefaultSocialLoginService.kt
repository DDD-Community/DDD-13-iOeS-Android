package com.pickflow.android.core.services.impl

import com.pickflow.android.core.services.protocols.SessionTokens
import com.pickflow.android.core.services.protocols.SocialAuthCredential
import com.pickflow.android.core.services.protocols.SocialLoginService
import com.pickflow.android.core.services.protocols.TokenStore
import javax.inject.Inject

class DefaultSocialLoginService @Inject constructor(
    private val tokenStore: TokenStore,
) : SocialLoginService {
    override suspend fun loginWith(credential: SocialAuthCredential): SessionTokens {
        val tokens = SessionTokens(
            accessToken = "session-${credential.provider.name.lowercase()}",
            refreshToken = credential.providerRefreshToken,
        )
        tokenStore.save(tokens.accessToken, tokens.refreshToken)
        return tokens
    }
}
