package com.pickflow.android.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.ReviewResult
import com.pickflow.android.core.services.protocols.ReviewResultService
import com.pickflow.android.core.services.protocols.ReviewResultStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ReviewResultViewModel @Inject constructor(
    private val reviewResultService: ReviewResultService,
) : ViewModel() {

    private val _status = MutableStateFlow<LoadState<ReviewResultStatus>>(LoadState.Idle)
    val status: StateFlow<LoadState<ReviewResultStatus>> = _status.asStateFlow()

    private val _hasIndicator = MutableStateFlow(false)
    val hasIndicator: StateFlow<Boolean> = _hasIndicator.asStateFlow()

    private val _latestUnacknowledgedResult = MutableStateFlow<ReviewResult?>(null)
    val latestUnacknowledgedResult: StateFlow<ReviewResult?> =
        _latestUnacknowledgedResult.asStateFlow()

    private val _isAcknowledgementInFlight = MutableStateFlow(false)
    val isAcknowledgementInFlight: StateFlow<Boolean> = _isAcknowledgementInFlight.asStateFlow()

    private val _acknowledgementError = MutableStateFlow<Throwable?>(null)
    val acknowledgementError: StateFlow<Throwable?> = _acknowledgementError.asStateFlow()

    fun load() {
        viewModelScope.launch {
            _status.value = LoadState.Loading
            runCatching { reviewResultService.status() }
                .onSuccess(::applyStatus)
                .onFailure { error ->
                    _hasIndicator.value = false
                    _latestUnacknowledgedResult.value = null
                    _status.value = LoadState.Failed(error)
                }
        }
    }

    fun acknowledge(resultId: Long) {
        if (_isAcknowledgementInFlight.value) return
        val current = (_status.value as? LoadState.Loaded)?.value ?: return
        if (current.unacknowledgedResults.none { it.resultId == resultId }) return

        viewModelScope.launch {
            _isAcknowledgementInFlight.value = true
            _acknowledgementError.value = null
            runCatching { reviewResultService.acknowledge(resultId) }
                .onSuccess {
                    applyStatus(
                        current.copy(
                            unacknowledgedResults = current.unacknowledgedResults.filterNot {
                                it.resultId == resultId
                            },
                        ),
                    )
                }
                .onFailure { _acknowledgementError.value = it }
            _isAcknowledgementInFlight.value = false
        }
    }

    fun acknowledgePublishedModal(resultId: Long) {
        if (_isAcknowledgementInFlight.value) return
        val current = (_status.value as? LoadState.Loaded)?.value ?: return
        if (current.unacknowledgedResults.none { it.resultId == resultId }) return

        viewModelScope.launch {
            _isAcknowledgementInFlight.value = true
            _acknowledgementError.value = null
            runCatching { reviewResultService.acknowledgePublishedModal(resultId) }
                .onSuccess {
                    applyStatus(
                        current.copy(
                            unacknowledgedResults = current.unacknowledgedResults.map { result ->
                                if (result.resultId == resultId) {
                                    result.copy(publishedModalAcknowledged = true)
                                } else {
                                    result
                                }
                            },
                        ),
                    )
                }
                .onFailure { _acknowledgementError.value = it }
            _isAcknowledgementInFlight.value = false
        }
    }

    fun dismissAcknowledgementError() {
        _acknowledgementError.value = null
    }

    private fun applyStatus(value: ReviewResultStatus) {
        _status.value = LoadState.Loaded(value)
        _hasIndicator.value = value.hasIndicator
        _latestUnacknowledgedResult.value =
            value.unacknowledgedResults
                .filterNot(ReviewResult::isAcknowledged)
                .maxByOrNull(ReviewResult::occurredAt)
    }
}
