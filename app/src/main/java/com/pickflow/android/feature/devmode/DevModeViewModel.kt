package com.pickflow.android.feature.devmode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.core.services.protocols.ApiEnvironment
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.DevSettings
import com.pickflow.android.core.services.protocols.GuestEntryStore
import com.pickflow.android.core.services.protocols.OnboardingCompletionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DevModeViewModel @Inject constructor(
    private val devSettings: DevSettings,
    private val authService: AuthService,
    private val onboardingStore: OnboardingCompletionStore,
    private val guestEntryStore: GuestEntryStore,
) : ViewModel() {

    val apiEnvironment = devSettings.apiEnvironment
    val badgeEnabled = devSettings.badgeEnabled
    val touchIndicatorEnabled = devSettings.touchIndicatorEnabled

    /** 온보딩 확인 여부. off 로 내리면 다음 앱 실행 때 온보딩이 다시 뜬다. */
    private val _onboardingCompleted = MutableStateFlow(false)
    val onboardingCompleted: StateFlow<Boolean> = _onboardingCompleted.asStateFlow()

    /** 비회원 진입 이력. on 이면 앱 실행 시 로그인 화면을 건너뛰고 탐색 탭으로 간다. */
    private val _guestEntered = MutableStateFlow(false)
    val guestEntered: StateFlow<Boolean> = _guestEntered.asStateFlow()

    /** 확인 대기 중인 환경 전환. null 이면 다이얼로그를 띄우지 않는다. */
    private val _pendingEnvironment = MutableStateFlow<ApiEnvironment?>(null)
    val pendingEnvironment: StateFlow<ApiEnvironment?> = _pendingEnvironment.asStateFlow()

    init {
        viewModelScope.launch { _onboardingCompleted.value = onboardingStore.isCompleted() }
        viewModelScope.launch { _guestEntered.value = guestEntryStore.hasEntered() }
    }

    /** 환경은 바로 바꾸지 않는다 — 로그아웃을 안내하고 확인을 받은 뒤 [confirmEnvironmentChange] 에서 적용. */
    fun selectEnvironment(environment: ApiEnvironment) {
        if (environment == devSettings.apiEnvironment.value) return
        _pendingEnvironment.value = environment
    }

    fun confirmEnvironmentChange() {
        val target = _pendingEnvironment.value ?: return
        _pendingEnvironment.value = null
        viewModelScope.launch {
            // 아직 이전 환경이므로 로그아웃 요청이 원래 서버로 나간다(세션 정리).
            // 실패해도 로컬 토큰은 AuthService 가 비우므로 그대로 진행한다.
            runCatching { authService.logout() }
            devSettings.setApiEnvironment(target)
        }
    }

    fun setBadgeEnabled(enabled: Boolean) = devSettings.setBadgeEnabled(enabled)

    fun setTouchIndicatorEnabled(enabled: Boolean) = devSettings.setTouchIndicatorEnabled(enabled)

    fun setOnboardingCompleted(completed: Boolean) {
        // 저장 후에 상태를 올린다 — 낙관적 갱신을 하면 init 의 초기 로드가
        // 나중에 완료되며 토글 결과를 덮어쓸 수 있다.
        viewModelScope.launch {
            onboardingStore.setCompleted(completed)
            _onboardingCompleted.value = completed
        }
    }

    fun setGuestEntered(entered: Boolean) {
        viewModelScope.launch {
            guestEntryStore.setEntered(entered)
            _guestEntered.value = entered
        }
    }
}

/** 마이 탭 연타로 여는 Dev Mode 진입 코드. */
const val DEV_MODE_PASSCODE = "1123"

/** 진입 알럿이 뜨기까지 필요한 마이 탭 연속 탭 횟수. */
const val DEV_MODE_TAP_THRESHOLD = 7
