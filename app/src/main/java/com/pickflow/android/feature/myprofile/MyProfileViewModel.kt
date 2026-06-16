package com.pickflow.android.feature.myprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.network.isUnauthorized
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.MyPageHome
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
) : ViewModel() {

    private val _loggedIn = MutableStateFlow<Boolean?>(null)
    val loggedIn: StateFlow<Boolean?> = _loggedIn.asStateFlow()

    private val _myPage = MutableStateFlow<LoadState<MyPageHome>>(LoadState.Idle)
    val myPage: StateFlow<LoadState<MyPageHome>> = _myPage.asStateFlow()

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
        }
    }
}
