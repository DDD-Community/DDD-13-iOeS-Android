package com.pickflow.android.feature.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.common.ui.LoadState
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
) : ViewModel() {

    private val _session = MutableStateFlow<LoadState<AuthenticatedSession>>(LoadState.Idle)
    val session: StateFlow<LoadState<AuthenticatedSession>> = _session.asStateFlow()

    fun loginWithKakao() {
        viewModelScope.launch {
            _session.value = LoadState.Loading
            _session.value = runCatching {
                val kakao = kakaoAuthProvider.login()
                socialLoginService.loginWith(
                    SocialAuthCredential(
                        provider = SocialProvider.KAKAO,
                        providerAccessToken = kakao.accessToken,
                        providerRefreshToken = kakao.refreshToken,
                    )
                )
            }.fold(
                onSuccess = { LoadState.Loaded(it) },
                onFailure = { LoadState.Failed(it) },
            )
        }
    }
}
