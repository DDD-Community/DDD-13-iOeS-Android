package com.pickflow.android.feature.spotdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.common.util.SpotIdCoder
import com.pickflow.android.core.network.ApiException
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.SharePayload
import com.pickflow.android.core.services.protocols.ShareIntentService
import com.pickflow.android.core.services.protocols.SpotDetail
import com.pickflow.android.core.services.protocols.SpotReportService
import com.pickflow.android.core.services.protocols.SpotService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SpotDetailViewModel @Inject constructor(
    private val spotService: SpotService,
    private val bookmarkService: BookmarkService,
    private val shareIntentService: ShareIntentService,
    private val spotReportService: SpotReportService,
    private val authService: AuthService,
) : ViewModel() {

    private val _isLoginRequired = MutableStateFlow(false)
    val isLoginRequired: StateFlow<Boolean> = _isLoginRequired.asStateFlow()

    fun dismissLoginRequired() { _isLoginRequired.value = false }

    private val _spot = MutableStateFlow<LoadState<SpotDetail>>(LoadState.Idle)
    val spot: StateFlow<LoadState<SpotDetail>> = _spot.asStateFlow()

    private val _bookmarked = MutableStateFlow(false)
    val bookmarked: StateFlow<Boolean> = _bookmarked.asStateFlow()

    private val _reportSubmitted = MutableStateFlow(false)
    val reportSubmitted: StateFlow<Boolean> = _reportSubmitted.asStateFlow()

    /** 신고 결과 토스트 메시지. 1회 표시 후 [consumeToast] 로 비운다. */
    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    fun consumeToast() { _toast.value = null }

    /**
     * 신고 시트 입력 초안. 시트 recomposition/재생성에도 유지되도록 ViewModel 에 보관.
     * 5~200자(트림 기준 최소 5자)일 때만 등록 가능.
     */
    private val _reportDraft = MutableStateFlow("")
    val reportDraft: StateFlow<String> = _reportDraft.asStateFlow()

    fun setReportDraft(value: String) { _reportDraft.value = value.take(REPORT_MAX_LENGTH) }
    fun clearReportDraft() { _reportDraft.value = "" }

    /** 스팟 등록 완료 직후 상세 진입 시 노출할 토스트. */
    fun showRegisteredToast() { _toast.value = "나만의 스팟이 등록되었어요!" }

    /**
     * iOS `SpotDetailViewModel.notifyUpdateRequested()` 1:1 fakedoor — "나만의 스팟 오픈" CTA 의
     * 업데이트 알림 신청. 현재 BE 연동 없이 토스트만 띄움 (양 플랫폼 동일).
     */
    fun notifyUpdateRequested() {
        _toast.value = "추후 업데이트 시, 가장 먼저 알림 보내드릴게요!"
    }

    fun load(spotId: String) {
        viewModelScope.launch {
            _spot.value = LoadState.Loading
            val result = runCatching { spotService.spot(spotId) }
            _spot.value = result.fold(
                onSuccess = { LoadState.Loaded(it) },
                onFailure = { LoadState.Failed(it) },
            )
            _bookmarked.value = result.getOrNull()?.isBookmarked
                ?: bookmarkService.isBookmarked(spotId)
        }
    }

    fun toggleBookmark() {
        val current = (_spot.value as? LoadState.Loaded<SpotDetail>)?.value ?: return
        viewModelScope.launch {
            if (!authService.isLoggedIn()) {
                // iOS `toggleBookmark()` 1:1 — 비로그인 시 LoginPrompt 노출 후 무시.
                _isLoginRequired.value = true
                return@launch
            }
            // iOS `SpotDetailViewModel.toggleBookmark` 1:1 — 낙관적 토글 + 실패 시 롤백.
            val previousValue = _bookmarked.value
            _bookmarked.value = !previousValue
            runCatching {
                val spotIdLong = current.id
                if (previousValue) {
                    bookmarkService.remove(spotIdLong.toString())
                } else {
                    bookmarkService.add(spotIdLong.toString())
                }
            }.onFailure {
                _bookmarked.value = previousValue
                _toast.value = "북마크 변경에 실패했어요."
            }
        }
    }

    fun share() {
        val current = (_spot.value as? LoadState.Loaded<SpotDetail>)?.value ?: return
        viewModelScope.launch {
            // iOS `share()` 1:1 — "이름 - 코멘트\nhttps://pickflow-api.us/{SpotIdCoder.encodeSpot(id)}"
            shareIntentService.share(
                SharePayload(
                    title = "${current.name} - ${current.comment}",
                    url = "https://pickflow-api.us/${SpotIdCoder.encodeSpot(current.id)}",
                )
            )
        }
    }

    /**
     * iOS `SpotDetailViewModel.reportInvalidInfo()` 1:1 — `spotReportService.report` 호출 후
     * 성공/실패 토스트 emit. 본문은 ReportSheetBody 에서 입력받은 5~200자 문자열.
     */
    fun reportInvalidInfo(content: String) {
        val current = (_spot.value as? LoadState.Loaded<SpotDetail>)?.value ?: return
        viewModelScope.launch {
            runCatching { spotReportService.report(current.id, content.trim()) }
                .onSuccess {
                    _reportSubmitted.value = true
                    _toast.value = "제보가 접수되었습니다."
                    clearReportDraft()
                }
                .onFailure { error ->
                    // 서버 정책 거부(예: SP002 미승인 스팟)는 사유를 그대로 노출.
                    _toast.value = (error as? ApiException)?.message
                        ?.takeIf { it.isNotBlank() }
                        ?: "제보 접수에 실패했어요."
                }
        }
    }

    companion object {
        const val REPORT_MIN_LENGTH = 5
        const val REPORT_MAX_LENGTH = 200
    }
}
