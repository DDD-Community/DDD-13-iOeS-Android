package com.pickflow.android.core.services.impl

import android.content.Context
import com.kakao.sdk.user.UserApiClient
import com.pickflow.android.core.services.protocols.KakaoAuthProvider
import com.pickflow.android.core.services.protocols.KakaoAuthResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Kakao SDK 기반 실제 로그인. 카카오 계정(웹) 로그인을 사용해
 * KakaoTalk 미설치 환경(에뮬레이터 등)에서도 동작한다.
 */
@Singleton
class RealKakaoAuthProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : KakaoAuthProvider {

    override suspend fun login(): KakaoAuthResult = suspendCancellableCoroutine { cont ->
        UserApiClient.instance.loginWithKakaoAccount(context) { token, error ->
            when {
                error != null -> cont.resumeWithException(error)
                token != null -> cont.resume(
                    KakaoAuthResult(
                        accessToken = token.accessToken,
                        refreshToken = token.refreshToken,
                    )
                )
                else -> cont.resumeWithException(
                    IllegalStateException("Kakao 로그인 결과가 비어 있습니다.")
                )
            }
        }
    }
}
