package com.pickflow.android.feature.accountmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.ImagePayload
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

    /** 서버에 저장된 현재 프로필 이미지 URL (변경 전). PhotosPicker 로 선택 후 [_draftImagePayload] 로 덮어쓴다. */
    private val _profileImageUrl = MutableStateFlow<String?>(null)
    val profileImageUrl: StateFlow<String?> = _profileImageUrl.asStateFlow()

    /** iOS `draftProfileImageData: Data?` 1:1 — 저장 전 임시 보관. null = 변경 없음. */
    private val _draftImagePayload = MutableStateFlow<ImagePayload?>(null)
    val draftImagePayload: StateFlow<ImagePayload?> = _draftImagePayload.asStateFlow()

    /** Compose 측에서 미리보기용으로 사용할 raw Uri (ImagePayload bytes 디코딩 회피). */
    private val _draftImagePreviewUri = MutableStateFlow<String?>(null)
    val draftImagePreviewUri: StateFlow<String?> = _draftImagePreviewUri.asStateFlow()

    val isSaveEnabled: StateFlow<Boolean> =
        combine(_nicknameDraft, _originalNickname, _draftImagePayload) { draft, original, image ->
            val nicknameChanged = draft.isNotBlank() && draft != original
            nicknameChanged || image != null
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
                }
        }
    }

    fun updateNickname(new: String) {
        _nicknameDraft.value = new
    }

    /** iOS `setDraftProfileImage(_:)` 1:1 — PhotosPicker 선택 결과를 ImagePayload + 미리보기 URI 로 보관. */
    fun setDraftImage(payload: ImagePayload?, previewUri: String?) {
        _draftImagePayload.value = payload
        _draftImagePreviewUri.value = previewUri
    }

    fun save() {
        viewModelScope.launch {
            val nicknameChanged = _nicknameDraft.value.isNotBlank() &&
                _nicknameDraft.value != _originalNickname.value
            runCatching {
                userService.updateProfile(
                    nickname = if (nicknameChanged) _nicknameDraft.value else null,
                    profileImage = _draftImagePayload.value,
                )
            }.onSuccess {
                if (nicknameChanged) _originalNickname.value = _nicknameDraft.value
                if (_draftImagePayload.value != null) {
                    // 저장 성공 시 draft 비우고 서버 응답 imageUrl 반영.
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
}
