package com.pickflow.android.feature.spotdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.core.services.protocols.MySpotService
import com.pickflow.android.core.services.protocols.MySpotStatus
import com.pickflow.android.core.services.protocols.MySpotStatusChange
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

/**
 * 내 스팟 오픈 상태 전이 전담. 상세 조회(`SpotDetailViewModel`)와 분리해 둔다 —
 * `SpotDetailActionsViewModel`(길안내/외부링크)과 같은 갈래다.
 *
 * 전이 성공 후 갱신된 상태는 [statusChanges] 로 흘려보내고, 화면이 상세를 다시 읽는다.
 * 낙관적 경합 감지는 없다 — 서버가 `updatedAt` 을 주지 않는다(`docs/PV-41/09-api-mapping.md` B6).
 */
@HiltViewModel
class SpotOpenActionsViewModel @Inject constructor(
    private val mySpotService: MySpotService,
) : ViewModel() {

    private val _isInFlight = MutableStateFlow(false)
    val isInFlight: StateFlow<Boolean> = _isInFlight.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    /** 전이 성공 시 갱신된 상태. 화면이 받아 상세를 재조회한다. */
    private val _statusChanges = MutableSharedFlow<MySpotStatus>(extraBufferCapacity = 1)
    val statusChanges: SharedFlow<MySpotStatus> = _statusChanges.asSharedFlow()

    /**
     * 지도/리스트 노출 여부. 서버가 상세·목록 응답에 노출 플래그를 주지 않아
     * 진입 시점 값은 알 수 없다 — PUBLISHED 면 켜져 있다고 보고 시작한다.
     */
    private val _isReleased = MutableStateFlow(true)
    val isReleased: StateFlow<Boolean> = _isReleased.asStateFlow()

    /** 삭제 완료. 화면이 받아 뒤로 나간다. */
    private val _deleted = MutableSharedFlow<Long>(extraBufferCapacity = 1)
    val deleted: SharedFlow<Long> = _deleted.asSharedFlow()

    fun consumeToast() { _toast.value = null }

    fun requestOpen(spotId: Long) = transition(spotId, OPEN_REQUESTED_TOAST) {
        mySpotService.requestOpen(it)
    }

    /**
     * 오픈 신청 철회와 비공개 전환은 서버에서 같은 요청이다.
     * 응답의 `previousStatus` 로 어느 쪽이었는지 구분해 안내 문구를 정한다.
     */
    fun unpublish(spotId: Long) {
        run(spotId) {
            val result = mySpotService.unpublish(it)
            val message = if (result.wasOpenRequest) WITHDRAWN_TOAST else UNPUBLISHED_TOAST
            result.status to message
        }
    }

    /**
     * 공개 토글. `POST/DELETE .../releases` 는 status 를 건드리지 않아 재검수 없이 되돌릴 수 있다.
     * 공개 해제([unpublish], DRAFT 전환)와 다른 동작이다.
     */
    fun setReleased(spotId: Long, released: Boolean) {
        if (_isInFlight.value) return
        _isInFlight.value = true
        _isReleased.value = released
        viewModelScope.launch {
            try {
                _isReleased.value = mySpotService.setReleased(spotId, released)
                _toast.value = if (released) RELEASED_TOAST else UNRELEASED_TOAST
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _isReleased.value = !released
                _toast.value = RETRY_TOAST
            } finally {
                _isInFlight.value = false
            }
        }
    }

    fun delete(spotId: Long) {
        if (_isInFlight.value) return
        _isInFlight.value = true
        viewModelScope.launch {
            try {
                mySpotService.delete(spotId)
                _deleted.emit(spotId)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _toast.value = RETRY_TOAST
            } finally {
                _isInFlight.value = false
            }
        }
    }

    private fun transition(
        spotId: Long,
        successToast: String,
        command: suspend (Long) -> MySpotStatusChange,
    ) = run(spotId) { command(it).status to successToast }

    private fun run(spotId: Long, command: suspend (Long) -> Pair<MySpotStatus, String>) {
        if (_isInFlight.value) return
        _isInFlight.value = true
        viewModelScope.launch {
            try {
                val (status, message) = command(spotId)
                _statusChanges.emit(status)
                _toast.value = message
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                _toast.value = RETRY_TOAST
            } finally {
                _isInFlight.value = false
            }
        }
    }

    private companion object {
        const val OPEN_REQUESTED_TOAST = "오픈 신청이 접수되었어요."
        const val WITHDRAWN_TOAST = "오픈 신청을 철회했어요."
        const val UNPUBLISHED_TOAST = "스팟을 비공개로 전환했어요."
        const val RELEASED_TOAST = "스팟을 다시 공개했어요."
        const val UNRELEASED_TOAST = "스팟 노출을 껐어요."
        const val RETRY_TOAST = "잠시 후 다시 시도해주세요."
    }
}
