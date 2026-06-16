package com.pickflow.android.feature.myprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.BuildConfig
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.network.isUnauthorized
import com.pickflow.android.core.services.protocols.AppVersionService
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.ExternalAppLauncher
import com.pickflow.android.core.services.protocols.MyPageHome
import com.pickflow.android.core.services.protocols.TermsPolicy
import com.pickflow.android.core.services.protocols.UserService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MyProfileViewModel @Inject constructor(
    private val userService: UserService,
    private val authService: AuthService,
    private val externalAppLauncher: ExternalAppLauncher,
    private val appVersionService: AppVersionService,
) : ViewModel() {

    private val _loggedIn = MutableStateFlow<Boolean?>(null)
    val loggedIn: StateFlow<Boolean?> = _loggedIn.asStateFlow()

    private val _myPage = MutableStateFlow<LoadState<MyPageHome>>(LoadState.Idle)
    val myPage: StateFlow<LoadState<MyPageHome>> = _myPage.asStateFlow()

    /** iOS `termsPolicies: [TermsPolicy]` 1:1 — 약관/개인정보 문서 N개 (서버 응답). */
    private val _termsPolicies = MutableStateFlow<List<TermsPolicy>>(emptyList())
    val termsPolicies: StateFlow<List<TermsPolicy>> = _termsPolicies.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val isIn = authService.isLoggedIn()
            _loggedIn.value = isIn
            if (!isIn) return@launch

            _myPage.value = LoadState.Loading
            runCatching { userService.fetchMyPage() }
                .onSuccess { _myPage.value = LoadState.Loaded(it) }
                .onFailure { error ->
                    if (error.isUnauthorized()) {
                        // TokenAuthenticator refresh 까지 실패한 401 — signedOut 으로 fallback.
                        runCatching { authService.logout() }
                        _loggedIn.value = false
                        _myPage.value = LoadState.Idle
                    } else {
                        _myPage.value = LoadState.Failed(error)
                    }
                }
            // iOS `MyProfileViewModel.loadAppConfig` 1:1 — 약관/정책 문서 목록 갱신.
            runCatching { appVersionService.fetchAndroidVersionPolicy() }
                .onSuccess { _termsPolicies.value = it.termsPolicies.orEmpty() }
        }
    }

    /**
     * 특정 약관/정책 문서 열기. iOS `MyProfileView` 가 `TermsAndPolicyListView` 셀 탭 시
     * 각 문서의 `url` 로 Custom Tab 진입. 서버 응답에 url 이 비어있으면 무시.
     */
    fun openTermsPolicy(policy: TermsPolicy) {
        if (policy.url.isBlank()) return
        viewModelScope.launch { externalAppLauncher.openCustomTab(policy.url) }
    }

    /**
     * @deprecated iOS 도 `termsPolicies` 서버 응답으로 일원화됨. BuildConfig fallback 은
     * 서버 응답이 비어 있을 때만 사용 — 단일 약관/정책 화면용.
     */
    fun openTerms() {
        viewModelScope.launch { externalAppLauncher.openCustomTab(BuildConfig.TERMS_URL) }
    }

    fun openPrivacy() {
        viewModelScope.launch { externalAppLauncher.openCustomTab(BuildConfig.PRIVACY_URL) }
    }
}
