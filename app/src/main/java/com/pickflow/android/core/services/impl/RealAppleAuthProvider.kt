package com.pickflow.android.core.services.impl

import android.content.Context
import android.content.Intent
import com.pickflow.android.BuildConfig
import com.pickflow.android.core.services.protocols.AppleAuthProvider
import com.pickflow.android.core.services.protocols.AppleAuthResult
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.Channel

/**
 * iOS `AppleAuthProvider`(`ASAuthorizationController`) 의 Android 1:1 대응 — Custom Tabs
 * 기반 Apple Sign In Web OAuth flow.
 *
 * 동작:
 * 1. [login] 호출 시 state/nonce 생성 후 [AppleOAuthUrl.build] 로 `appleid.apple.com/auth/authorize`
 *    URL 을 Application context 로 `ACTION_VIEW` Intent 발사 (Custom Tabs).
 * 2. Apple 인증 완료 시 등록된 redirect URI 로 fragment(`id_token`, `state`, ...) 가 돌아옴.
 * 3. AndroidManifest 의 intent-filter 가 `AppleCallbackActivity` 로 라우팅 →
 *    [complete] 호출 → [resultChannel] 에 결과 emit → suspend [login] 함수 resume.
 *
 * BuildConfig.APPLE_SERVICE_ID / APPLE_REDIRECT_URI 가 비어 있으면 즉시 throw — Apple
 * Developer 콘솔 등록 + secrets.properties 주입 후 정상 동작.
 */
@Singleton
class RealAppleAuthProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : AppleAuthProvider {

    private val resultChannel = Channel<Result<AppleAuthResult>>(Channel.RENDEZVOUS)
    @Volatile private var expectedState: String? = null
    @Volatile private var expectedNonce: String? = null

    override suspend fun login(): AppleAuthResult {
        val serviceId = BuildConfig.APPLE_SERVICE_ID
        val redirectUri = BuildConfig.APPLE_REDIRECT_URI
        check(serviceId.isNotBlank() && redirectUri.isNotBlank()) {
            "Apple Sign In 미설정 — secrets.properties 의 APPLE_SERVICE_ID / APPLE_REDIRECT_URI 를 채워주세요."
        }

        val state = newToken()
        val nonce = newToken()
        expectedState = state
        expectedNonce = nonce

        val url = AppleOAuthUrl.build(serviceId, redirectUri, state, nonce)
        val intent = Intent(Intent.ACTION_VIEW, url)
            .apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)

        return resultChannel.receive().getOrThrow()
    }

    /**
     * [AppleCallbackActivity] 에서 redirect URI fragment 를 파싱한 결과를 전달한다.
     * state 가 일치하지 않으면 CSRF 오류로 실패 처리.
     */
    fun complete(state: String?, idToken: String?, error: String?) {
        val expected = expectedState
        expectedState = null
        expectedNonce = null
        if (expected != null && state != expected) {
            resultChannel.trySend(Result.failure(IllegalStateException("Apple OAuth state 불일치")))
            return
        }
        if (!error.isNullOrBlank()) {
            resultChannel.trySend(Result.failure(IllegalStateException("Apple OAuth error: $error")))
            return
        }
        if (idToken.isNullOrBlank()) {
            resultChannel.trySend(Result.failure(IllegalStateException("Apple OAuth id_token 없음")))
            return
        }
        resultChannel.trySend(Result.success(AppleAuthResult(identityToken = idToken)))
    }

    private fun newToken(): String = UUID.randomUUID().toString().replace("-", "")
}
