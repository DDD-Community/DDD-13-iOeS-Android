package com.pickflow.android.feature.forceupdate

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.BuildConfig
import com.pickflow.android.common.util.SemanticVersion
import com.pickflow.android.core.services.protocols.AppVersionService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * iOS `ForceUpdateViewModel` 1:1 — 앱 시작 시 버전 정책 확인 + 강제 업데이트 판정.
 *
 * 판단 기준: `현재 앱 버전 < minimumVersion && forceUpdate == true`.
 * 어떤 사유든 판정 불가하면 진입 허용 (fail-open) — 서버 장애로 모든 사용자 진입 차단 회피.
 */
@HiltViewModel
class ForceUpdateViewModel @Inject constructor(
    private val service: AppVersionService,
) : ViewModel() {

    sealed interface AppLaunchState {
        data object Checking : AppLaunchState
        data class NeedsForceUpdate(val storeUrl: String) : AppLaunchState
        data object Available : AppLaunchState
    }

    private val _state = MutableStateFlow<AppLaunchState>(AppLaunchState.Checking)
    val state: StateFlow<AppLaunchState> = _state.asStateFlow()

    private val currentVersion: String = BuildConfig.VERSION_NAME

    fun checkForUpdate() {
        viewModelScope.launch {
            runCatching { service.fetchAndroidVersionPolicy() }
                .onSuccess { policy ->
                    val current = SemanticVersion.parse(currentVersion)
                    val minimum = SemanticVersion.parse(policy.minimumVersion)
                    _state.value = if (
                        policy.forceUpdate &&
                        current != null &&
                        minimum != null &&
                        current < minimum &&
                        policy.storeUrl.isNotBlank()
                    ) {
                        AppLaunchState.NeedsForceUpdate(storeUrl = policy.storeUrl)
                    } else {
                        AppLaunchState.Available
                    }
                }
                .onFailure { _state.value = AppLaunchState.Available }
        }
    }
}
