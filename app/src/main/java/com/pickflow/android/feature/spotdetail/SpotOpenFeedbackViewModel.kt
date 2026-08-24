package com.pickflow.android.feature.spotdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.core.services.protocols.SpotReportService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SpotOpenFeedbackViewModel @Inject constructor(
    private val spotReportService: SpotReportService,
) : ViewModel() {
    private val _draft = MutableStateFlow("")
    val draft: StateFlow<String> = _draft.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    fun setDraft(value: String) {
        _draft.value = value.take(SpotDetailViewModel.REPORT_MAX_LENGTH)
    }

    fun clearDraft() {
        _draft.value = ""
    }

    fun submit(spotId: Long) {
        val content = _draft.value.trim()
        if (content.length < SpotDetailViewModel.REPORT_MIN_LENGTH) return
        viewModelScope.launch {
            runCatching { spotReportService.report(spotId, content) }
                .onSuccess {
                    clearDraft()
                    _toast.value = "제보가 접수되었습니다."
                }
                .onFailure { _toast.value = "제보 접수에 실패했어요." }
        }
    }

    fun consumeToast() {
        _toast.value = null
    }
}
