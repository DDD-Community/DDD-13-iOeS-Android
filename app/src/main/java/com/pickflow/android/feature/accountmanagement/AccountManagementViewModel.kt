package com.pickflow.android.feature.accountmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.ImagePayload
import com.pickflow.android.core.services.protocols.SocialProvider
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

    private val _nicknameDraft = MutableStateFlow("")
    val nicknameDraft: StateFlow<String> = _nicknameDraft.asStateFlow()

    private val _originalNickname = MutableStateFlow("")

    /** 닉네임 검증 에러 메시지(없으면 null). */
    private val _nicknameError = MutableStateFlow<String?>(null)
    val nicknameError: StateFlow<String?> = _nicknameError.asStateFlow()

    /** 연결된 소셜. */
    private val _provider = MutableStateFlow<SocialProvider?>(null)
    val provider: StateFlow<SocialProvider?> = _provider.asStateFlow()

    private val _profileImageUrl = MutableStateFlow<String?>(null)
    val profileImageUrl: StateFlow<String?> = _profileImageUrl.asStateFlow()

    private val _draftImagePayload = MutableStateFlow<ImagePayload?>(null)
    val draftImagePayload: StateFlow<ImagePayload?> = _draftImagePayload.asStateFlow()

    private val _draftImagePreviewUri = MutableStateFlow<String?>(null)
    val draftImagePreviewUri: StateFlow<String?> = _draftImagePreviewUri.asStateFlow()

    val isSaveEnabled: StateFlow<Boolean> =
        combine(_nicknameDraft, _originalNickname, _draftImagePayload) { draft, original, image ->
            val nicknameChanged = draft != original
            val nicknameValid = isNicknameValid(draft)
            // 닉네임을 바꿨다면 유효해야 저장 가능. 이미지만 바꿨다면 닉네임은 원본 유지.
            (nicknameChanged && nicknameValid) || (image != null && (!nicknameChanged || nicknameValid))
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
                    _profileImageUrl.value = myPage.profileImageUrl
                    _provider.value = myPage.provider
                }
        }
    }

    /** 한글·영문·숫자 2~12자만 허용. 12자 초과 입력 차단, 특수문자·공백 시 에러. */
    fun updateNickname(new: String) {
        val capped = new.take(MAX_NICKNAME)
        _nicknameDraft.value = capped
        _nicknameError.value = when {
            capped == _originalNickname.value || capped.isEmpty() -> null
            !isNicknameValid(capped) -> "한글, 영문, 숫자 2~12자로 입력해주세요."
            else -> null
        }
    }

    fun setDraftImage(payload: ImagePayload?, previewUri: String?) {
        _draftImagePayload.value = payload
        _draftImagePreviewUri.value = previewUri
    }

    fun save() {
        viewModelScope.launch {
            val nicknameChanged = _nicknameDraft.value != _originalNickname.value &&
                isNicknameValid(_nicknameDraft.value)
            runCatching {
                userService.updateProfile(
                    nickname = if (nicknameChanged) _nicknameDraft.value else null,
                    profileImage = _draftImagePayload.value,
                )
            }.onSuccess {
                if (nicknameChanged) _originalNickname.value = _nicknameDraft.value
                if (_draftImagePayload.value != null) {
                    _profileImageUrl.value = it.profileImageUrl ?: _profileImageUrl.value
                    _draftImagePayload.value = null
                    _draftImagePreviewUri.value = null
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authService.logout()
            _signedOut.value = true
        }
    }

    private fun isNicknameValid(value: String): Boolean = NICKNAME_REGEX.matches(value)

    companion object {
        private const val MAX_NICKNAME = 12
        private val NICKNAME_REGEX = Regex("^[가-힣a-zA-Z0-9]{2,12}$")
    }
}
