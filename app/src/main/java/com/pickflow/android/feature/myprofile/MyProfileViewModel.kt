package com.pickflow.android.feature.myprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.common.ui.LoadState
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
            if (isIn) {
                _myPage.value = LoadState.Loading
                _myPage.value = runCatching { userService.fetchMyPage() }
                    .fold(
                        onSuccess = { LoadState.Loaded(it) },
                        onFailure = { LoadState.Failed(it) },
                    )
            }
        }
    }
}
