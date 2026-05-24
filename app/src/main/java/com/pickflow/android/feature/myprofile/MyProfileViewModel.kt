package com.pickflow.android.feature.myprofile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.common.ui.LoadState
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
) : ViewModel() {

    private val _userName = MutableStateFlow<LoadState<String>>(LoadState.Idle)
    val userName: StateFlow<LoadState<String>> = _userName.asStateFlow()

    fun loadUserName() {
        viewModelScope.launch {
            _userName.value = LoadState.Loading
            _userName.value = runCatching { userService.fetchUserName() }
                .fold(
                    onSuccess = { name ->
                        if (name.isBlank()) LoadState.Empty else LoadState.Loaded(name)
                    },
                    onFailure = { LoadState.Failed(it) },
                )
        }
    }
}
