package com.pickflow.android.feature.accountmanagement

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.core.network.ApiException
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.GuestEntryStore
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
    private val guestEntryStore: GuestEntryStore,
) : ViewModel() {

    private val _signedOut = MutableStateFlow(false)
    val signedOut: StateFlow<Boolean> = _signedOut.asStateFlow()

    /**
     * 로그아웃 후 로그인 화면 대신 탐색 탭으로 돌아갈지. 비회원으로 탐색한 이력이 있으면 true.
     * 앱 시작 분기([com.pickflow.android.app.navigation.PickflowEntryViewModel])와 같은 규칙이다.
     */
    private val _keepBrowsingAfterSignOut = MutableStateFlow(false)
    val keepBrowsingAfterSignOut: StateFlow<Boolean> = _keepBrowsingAfterSignOut.asStateFlow()

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

    /** 저장 결과 토스트("저장되었습니다." 등). 1회 표시 후 [consumeToast] 로 비운다. */
    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    fun consumeToast() { _toast.value = null }

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

    /**
     * 닉네임 유효성 정책 — 한글·영문·숫자 2~12자, 특수문자/공백 불가.
     * 12자 초과는 입력을 허용하되 인라인 에러를 노출하고 저장을 비활성화한다.
     */
    fun updateNickname(new: String) {
        val capped = new.take(MAX_NICKNAME_INPUT)
        _nicknameDraft.value = capped
        _nicknameError.value = when {
            capped == _originalNickname.value || capped.isEmpty() -> null
            capped.length > MAX_NICKNAME -> "닉네임은 12자 이하로 입력해 주세요."
            !isNicknameValid(capped) -> "한글, 영문, 숫자 2~12자로 입력해주세요."
            else -> null
        }
    }

    fun setDraftImage(payload: ImagePayload?, previewUri: String?) {
        _draftImagePayload.value = payload
        _draftImagePreviewUri.value = previewUri
    }

    /**
     * 변경분 저장. 성공 시 "저장되었습니다." 토스트, 닉네임 중복(서버 검증) 시
     * 인라인 에러 "이미 사용 중인 닉네임이에요." 노출.
     */
    fun save() {
        if (!isSaveEnabled.value) return
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
                _nicknameError.value = null
                _toast.value = "저장되었습니다."
            }.onFailure { error ->
                if (nicknameChanged && error is ApiException) {
                    // 서버 측 닉네임 검증 실패 — 정책상 중복이 유일한 케이스.
                    _nicknameError.value = "이미 사용 중인 닉네임이에요."
                } else {
                    _toast.value = "저장에 실패했어요."
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authService.logout()
            // signedOut 을 올리기 전에 목적지를 정해둔다 — 화면은 signedOut 을 보고 한 번에 이동한다.
            _keepBrowsingAfterSignOut.value = guestEntryStore.hasEntered()
            _signedOut.value = true
        }
    }

    private fun isNicknameValid(value: String): Boolean = NICKNAME_REGEX.matches(value)

    companion object {
        private const val MAX_NICKNAME = 12

        /** 12자 초과 에러 문구를 보여주기 위한 입력 상한(하드 컷). */
        private const val MAX_NICKNAME_INPUT = 20
        private val NICKNAME_REGEX = Regex("^[가-힣a-zA-Z0-9]{2,12}$")
    }
}
