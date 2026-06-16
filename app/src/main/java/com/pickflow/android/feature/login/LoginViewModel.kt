package com.pickflow.android.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.network.ApiException
import com.pickflow.android.core.network.isWithdrawalRestoreRequired
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.AuthenticatedSession
import com.pickflow.android.core.services.protocols.KakaoAuthProvider
import com.pickflow.android.core.services.protocols.SocialAuthCredential
import com.pickflow.android.core.services.protocols.SocialLoginService
import com.pickflow.android.core.services.protocols.SocialProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val kakaoAuthProvider: KakaoAuthProvider,
    private val socialLoginService: SocialLoginService,
    private val authService: AuthService,
) : ViewModel() {

    /** 재가입 안내 팝업 상태(서버 message + 복구 토큰). null = 미표시. */
    data class RestorePrompt(val message: String?, val restoreToken: String?)

    private val _session = MutableStateFlow<LoadState<AuthenticatedSession>>(LoadState.Idle)
    val session: StateFlow<LoadState<AuthenticatedSession>> = _session.asStateFlow()

    private val _restorePrompt = MutableStateFlow<RestorePrompt?>(null)
    val restorePrompt: StateFlow<RestorePrompt?> = _restorePrompt.asStateFlow()

    /** 재가입 확정 후 재로그인에 사용할 자격 증명. */
    private var lastCredential: SocialAuthCredential? = null

    fun loginWithKakao() {
        viewModelScope.launch {
            _session.value = LoadState.Loading
            runCatching {
                val kakao = kakaoAuthProvider.login()
                val credential = SocialAuthCredential(
                    provider = SocialProvider.KAKAO,
                    providerAccessToken = kakao.accessToken,
                    providerRefreshToken = kakao.refreshToken,
                )
                lastCredential = credential
                socialLoginService.loginWith(credential)
            }.fold(
                onSuccess = { _session.value = LoadState.Loaded(it) },
                onFailure = { error ->
                    if (error.isWithdrawalRestoreRequired()) {
                        // 탈퇴 이력 계정 → 재가입 안내 팝업.
                        _session.value = LoadState.Idle
                        _restorePrompt.value = RestorePrompt(
                            message = error.message,
                            restoreToken = (error as? ApiException)?.restoreToken,
                        )
                    } else {
                        _session.value = LoadState.Failed(error)
                    }
                },
            )
        }
    }

    fun dismissRestorePrompt() {
        _restorePrompt.value = null
    }

    /** 재가입 확정 — 계정 복구 후 동일 자격으로 재로그인. */
    fun confirmRestore() {
        val credential = lastCredential ?: return
        val token = _restorePrompt.value?.restoreToken
        _restorePrompt.value = null
        viewModelScope.launch {
            _session.value = LoadState.Loading
            _session.value = runCatching {
                if (token != null) authService.restore(token)
                socialLoginService.loginWith(credential)
            }.fold(
                onSuccess = { LoadState.Loaded(it) },
                onFailure = { LoadState.Failed(it) },
            )
        }
    }
}
