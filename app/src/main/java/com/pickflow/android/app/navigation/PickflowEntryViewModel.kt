package com.pickflow.android.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.GuestEntryStore
import com.pickflow.android.core.services.protocols.OnboardingCompletionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 앱 시작 직후 1회 — onboarding 완료 여부, 로그인 여부, 비회원 진입 이력을 읽어
 * 시작 라우트를 결정한다. iOS `AppRootView`의 진입 분기와 동일.
 *
 * 온보딩을 마쳤고 "비회원으로 시작하기"로 탐색 탭에 들어간 적이 있으면 로그인 화면을
 * 건너뛰고 바로 탐색 탭으로 보낸다.
 */
@HiltViewModel
class PickflowEntryViewModel @Inject constructor(
    private val onboardingStore: OnboardingCompletionStore,
    private val authService: AuthService,
    private val guestEntryStore: GuestEntryStore,
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            val onboarded = onboardingStore.isCompleted()
            val loggedIn = authService.isLoggedIn()
            val guestEntered = guestEntryStore.hasEntered()
            _startDestination.value = when {
                !onboarded -> PickflowRoute.ONBOARDING
                loggedIn || guestEntered -> PickflowRoute.HOME
                else -> PickflowRoute.LOGIN
            }
        }
    }
}
