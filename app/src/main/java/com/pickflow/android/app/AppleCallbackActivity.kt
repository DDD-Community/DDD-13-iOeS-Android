package com.pickflow.android.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.pickflow.android.core.services.impl.RealAppleAuthProvider
import com.pickflow.android.core.services.protocols.AppleAuthProvider
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Apple Sign In OAuth redirect URI 수신 전용 Activity.
 *
 * `AndroidManifest` 의 intent-filter (`<data android:scheme="https" android:host="...">`) 가
 * Apple authorize 서버의 fragment redirect 를 본 Activity 로 라우팅.
 * fragment 에서 `id_token` / `state` / `error` 를 추출하여 [RealAppleAuthProvider.complete] 에 전달.
 *
 * UI 미표시 — `finish()` 직후 종료. 호출자 [RealAppleAuthProvider.login] 의 suspend 결과가 resume.
 */
@AndroidEntryPoint
class AppleCallbackActivity : ComponentActivity() {
    @Inject lateinit var appleAuthProvider: AppleAuthProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val provider = appleAuthProvider
        val uri = intent?.data
        if (provider is RealAppleAuthProvider && uri != null) {
            // Apple 은 response_mode=fragment 로 `#id_token=...&state=...` 형태를 돌려준다.
            // fragment 가 없으면 query 도 시도 (response_mode=query 호환).
            val raw = uri.fragment.orEmpty().ifBlank { uri.query.orEmpty() }
            val params = raw.split("&")
                .mapNotNull {
                    val parts = it.split("=", limit = 2)
                    if (parts.size != 2) null else parts[0] to parts[1]
                }
                .toMap()
            provider.complete(
                state = params["state"],
                idToken = params["id_token"],
                error = params["error"],
            )
        }
        finish()
    }
}
