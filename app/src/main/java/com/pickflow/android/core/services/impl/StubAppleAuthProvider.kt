package com.pickflow.android.core.services.impl

import com.pickflow.android.core.services.protocols.AppleAuthProvider
import com.pickflow.android.core.services.protocols.AppleAuthResult
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Apple Login은 PLAN.md §4에 따라 v1 제외(Android는 Sign in with Apple JS / OAuth 웹뷰
 * 우회가 필요). 본 stub은 호출 시 명시적으로 실패하여 화면 측에서 LoadState.Failed로 흐르게
 * 한다. AuthApi.appleLogin 자체는 실 BE와 연동되어 있어, 추후 OAuth 결과로 받은
 * identityToken을 주입하는 Real 구현으로 교체하면 즉시 동작한다.
 */
@Singleton
class StubAppleAuthProvider @Inject constructor() : AppleAuthProvider {
    override suspend fun login(): AppleAuthResult {
        // TODO(post-v1): Sign in with Apple OAuth 웹 플로우 결과로 identityToken 생성
        throw UnsupportedOperationException("Apple 로그인은 v1에서 지원되지 않습니다.")
    }
}
