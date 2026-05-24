package com.pickflow.android.feature.accountmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.core.services.protocols.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AccountManagementViewModel @Inject constructor(
    private val authService: AuthService,
) : ViewModel() {

    private val _signedOut = MutableStateFlow(false)
    val signedOut: StateFlow<Boolean> = _signedOut.asStateFlow()

    private val _withdrawDialogVisible = MutableStateFlow(false)
    val withdrawDialogVisible: StateFlow<Boolean> = _withdrawDialogVisible.asStateFlow()

    fun logout() {
        viewModelScope.launch {
            authService.logout()
            _signedOut.value = true
        }
    }

    fun requestWithdraw() {
        _withdrawDialogVisible.value = true
    }

    fun dismissWithdraw() {
        _withdrawDialogVisible.value = false
    }

    fun confirmWithdraw() {
        viewModelScope.launch {
            authService.withdraw()
            _withdrawDialogVisible.value = false
            _signedOut.value = true
        }
    }
}
