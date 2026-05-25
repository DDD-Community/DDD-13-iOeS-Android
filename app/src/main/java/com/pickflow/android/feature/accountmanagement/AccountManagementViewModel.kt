package com.pickflow.android.feature.accountmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.UserService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class AccountManagementViewModel @Inject constructor(
    private val authService: AuthService,
    private val userService: UserService,
) : ViewModel() {

    private val _signedOut = MutableStateFlow(false)
    val signedOut: StateFlow<Boolean> = _signedOut.asStateFlow()

    private val _withdrawDialogVisible = MutableStateFlow(false)
    val withdrawDialogVisible: StateFlow<Boolean> = _withdrawDialogVisible.asStateFlow()

    private val _nicknameDraft = MutableStateFlow("")
    val nicknameDraft: StateFlow<String> = _nicknameDraft.asStateFlow()

    private val _originalNickname = MutableStateFlow("")

    val isSaveEnabled: StateFlow<Boolean> =
        combine(_nicknameDraft, _originalNickname) { draft, original ->
            draft.isNotBlank() && draft != original
        }.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            runCatching { userService.fetchMyPage() }
                .onSuccess { myPage ->
                    _originalNickname.value = myPage.nickname
                    _nicknameDraft.value = myPage.nickname
                }
        }
    }

    fun updateNickname(new: String) {
        _nicknameDraft.value = new
    }

    fun save() {
        viewModelScope.launch {
            runCatching { userService.updateProfile(nickname = _nicknameDraft.value) }
                .onSuccess {
                    _originalNickname.value = _nicknameDraft.value
                }
        }
    }

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
