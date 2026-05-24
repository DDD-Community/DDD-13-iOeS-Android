package com.pickflow.android.core.services.impl

import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.TokenStore
import javax.inject.Inject

class DefaultAuthService @Inject constructor(
    private val tokenStore: TokenStore,
) : AuthService {
    override suspend fun logout() {
        tokenStore.clear()
    }

    override suspend fun withdraw() {
        tokenStore.clear()
    }

    override suspend fun isLoggedIn(): Boolean = tokenStore.accessToken() != null
}
