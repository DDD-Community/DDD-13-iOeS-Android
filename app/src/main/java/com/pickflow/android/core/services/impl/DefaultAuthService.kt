package com.pickflow.android.core.services.impl

import com.pickflow.android.core.network.api.AuthApi
import com.pickflow.android.core.network.api.UserApi
import com.pickflow.android.core.network.dto.auth.LogoutRequest
import com.pickflow.android.core.network.unwrapVoid
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.TokenStore
import javax.inject.Inject

class DefaultAuthService @Inject constructor(
    private val authApi: AuthApi,
    private val userApi: UserApi,
    private val tokenStore: TokenStore,
) : AuthService {
    override suspend fun logout() {
        val refresh = tokenStore.refreshToken()
        if (refresh != null) {
            // 서버 호출 실패해도 로컬 토큰은 비워서 사용자가 다시 로그인할 수 있게 한다.
            // 다만 BE 측 invalidation은 누락되므로 예외는 호출자에게 그대로 전파.
            try {
                authApi.logout(LogoutRequest(refreshToken = refresh)).unwrapVoid()
            } finally {
                tokenStore.clear()
            }
        } else {
            tokenStore.clear()
        }
    }

    override suspend fun withdraw() {
        // 서버 호출 실패해도 로컬 토큰은 비워서 사용자가 로그인 화면으로 복귀할 수 있게 한다.
        try {
            userApi.deleteAccount().unwrapVoid()
        } finally {
            tokenStore.clear()
        }
    }

    override suspend fun isLoggedIn(): Boolean = tokenStore.accessToken() != null

    override suspend fun restore(restoreToken: String) {
        userApi.restoreAccount(restoreToken).unwrapVoid()
    }
}
