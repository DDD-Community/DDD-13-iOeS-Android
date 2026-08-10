package com.pickflow.android.feature.spotdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.MySpotDetail
import com.pickflow.android.core.services.protocols.MySpotService
import com.pickflow.android.core.services.protocols.MySpotTransitionConflictException
import com.pickflow.android.core.services.protocols.MySpotTransitionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SpotOpenViewModel @Inject constructor(
    private val mySpotService: MySpotService,
) : ViewModel() {

    private val _detail = MutableStateFlow<LoadState<MySpotDetail>>(LoadState.Idle)
    val detail: StateFlow<LoadState<MySpotDetail>> = _detail.asStateFlow()

    private val _isTransitionInFlight = MutableStateFlow(false)
    val isTransitionInFlight: StateFlow<Boolean> = _isTransitionInFlight.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    private val _deletedSpotIds = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val deletedSpotIds: SharedFlow<Long> = _deletedSpotIds.asSharedFlow()

    fun load(spotId: Long) {
        viewModelScope.launch { loadLatest(spotId) }
    }

    fun requestOpen() {
        transition(successToast = OPEN_REQUESTED_TOAST) { spotId ->
            mySpotService.requestOpen(spotId)
        }
    }

    fun withdrawRequest() {
        transition { spotId -> mySpotService.withdrawRequest(spotId) }
    }

    fun withdrawRejection() {
        transition { spotId -> mySpotService.withdrawRejection(spotId) }
    }

    fun cancelOpen() {
        transition { spotId -> mySpotService.cancelOpen(spotId) }
    }

    fun delete() {
        val spotId = currentDetail()?.id ?: return
        if (_isTransitionInFlight.value) return
        _isTransitionInFlight.value = true

        viewModelScope.launch {
            try {
                mySpotService.delete(spotId)
                _deletedSpotIds.emit(spotId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _toast.value = RETRY_TOAST
            } finally {
                _isTransitionInFlight.value = false
            }
        }
    }

    fun consumeToast() {
        _toast.value = null
    }

    fun showRegisteredToast() {
        _toast.value = "나만의 스팟이 등록되었어요!"
    }

    private fun transition(
        successToast: String? = null,
        command: suspend (Long) -> MySpotTransitionResult,
    ) {
        val current = currentDetail() ?: return
        if (_isTransitionInFlight.value) return
        _isTransitionInFlight.value = true

        viewModelScope.launch {
            try {
                val result = command(current.id)
                _detail.value = LoadState.Loaded(
                    current.copy(
                        status = result.status,
                        updatedAt = result.updatedAt,
                    ),
                )
                if (successToast != null) _toast.value = successToast
            } catch (conflict: MySpotTransitionConflictException) {
                _toast.value = ALREADY_PROCESSED_TOAST
                loadLatest(conflict.spotId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _toast.value = RETRY_TOAST
            } finally {
                _isTransitionInFlight.value = false
            }
        }
    }

    private suspend fun loadLatest(spotId: Long) {
        _detail.value = LoadState.Loading
        _detail.value = try {
            LoadState.Loaded(mySpotService.detail(spotId))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Exception) {
            LoadState.Failed(error)
        }
    }

    private fun currentDetail(): MySpotDetail? =
        (detail.value as? LoadState.Loaded<MySpotDetail>)?.value

    private companion object {
        const val OPEN_REQUESTED_TOAST = "오픈 신청이 접수되었어요"
        const val RETRY_TOAST = "실패했어요, 다시 시도해주세요"
        const val ALREADY_PROCESSED_TOAST = "이미 처리된 신청이에요"
    }
}
