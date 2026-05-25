package com.pickflow.android.app.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.OnboardingCompletionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 앱 시작 직후 1회 — DataStore에서 onboarding 완료 여부, TokenStore에서 로그인 여부를
 * 읽어 시작 라우트를 결정한다. iOS `AppRootView`의 진입 분기와 동일.
 */
@HiltViewModel
class PickflowEntryViewModel @Inject constructor(
    private val onboardingStore: OnboardingCompletionStore,
    private val authService: AuthService,
) : ViewModel() {

    private val _startDestination = MutableStateFlow<String?>(null)
    val startDestination: StateFlow<String?> = _startDestination.asStateFlow()

    init {
        viewModelScope.launch {
            val onboarded = onboardingStore.isCompleted()
            val loggedIn = authService.isLoggedIn()
            _startDestination.value = when {
                !onboarded -> PickflowRoute.ONBOARDING
                !loggedIn -> PickflowRoute.LOGIN
                else -> PickflowRoute.HOME
            }
        }
    }
}
