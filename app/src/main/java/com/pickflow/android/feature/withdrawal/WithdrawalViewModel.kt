package com.pickflow.android.feature.withdrawal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.feature.withdrawal.model.WithdrawalReason
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * iOS `WithdrawalViewModel` 1:1 — 사유 dropdown + 동의 체크 + 탈퇴 처리.
 * step 은 단순 sealed class 가 아니라 입력 상태 + processing/done/failed 만 의미.
 */
@HiltViewModel
class WithdrawalViewModel @Inject constructor(
    private val authService: AuthService,
) : ViewModel() {

    sealed interface Step {
        data object Input : Step
        data object Processing : Step
        data object Done : Step
        data class Failed(val message: String) : Step
    }

    private val _step = MutableStateFlow<Step>(Step.Input)
    val step: StateFlow<Step> = _step.asStateFlow()

    private val _selectedReason = MutableStateFlow<WithdrawalReason?>(null)
    val selectedReason: StateFlow<WithdrawalReason?> = _selectedReason.asStateFlow()

    private val _isDropdownOpen = MutableStateFlow(false)
    val isDropdownOpen: StateFlow<Boolean> = _isDropdownOpen.asStateFlow()

    private val _otherFeedback = MutableStateFlow("")
    val otherFeedback: StateFlow<String> = _otherFeedback.asStateFlow()

    private val _didAgree = MutableStateFlow(false)
    val didAgree: StateFlow<Boolean> = _didAgree.asStateFlow()

    fun toggleDropdown() { _isDropdownOpen.value = !_isDropdownOpen.value }

    fun selectReason(reason: WithdrawalReason) {
        _selectedReason.value = reason
        _isDropdownOpen.value = false
    }

    fun updateOtherFeedback(text: String) {
        _otherFeedback.value = if (text.length > 200) text.take(200) else text
    }

    fun toggleAgreement() { _didAgree.value = !_didAgree.value }

    fun canSubmit(): Boolean {
        val reason = _selectedReason.value ?: return false
        if (!_didAgree.value) return false
        if (reason == WithdrawalReason.Other && _otherFeedback.value.trim().isEmpty()) return false
        return true
    }

    fun submitWithdrawal() {
        if (!canSubmit()) return
        _step.value = Step.Processing
        viewModelScope.launch {
            runCatching { authService.withdraw() }
                .onSuccess { _step.value = Step.Done }
                .onFailure { _step.value = Step.Failed(it.message ?: "탈퇴 처리에 실패했어요.") }
        }
    }
}
