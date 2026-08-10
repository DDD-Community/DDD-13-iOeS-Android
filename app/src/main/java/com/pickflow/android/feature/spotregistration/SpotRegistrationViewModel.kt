package com.pickflow.android.feature.spotregistration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.AddressSuggestion
import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.ImagePayload
import com.pickflow.android.core.services.protocols.LocationService
import com.pickflow.android.core.services.protocols.MySpotDetail
import com.pickflow.android.core.services.protocols.MySpotService
import com.pickflow.android.core.services.protocols.MySpotStatus
import com.pickflow.android.core.services.protocols.SpotDraft
import com.pickflow.android.core.services.protocols.SpotTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sqrt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class SpotRegistrationMode { CREATE, REVISE }

data class SpotRegistrationSubmissionResult(
    val spotId: Long,
    val status: MySpotStatus,
)

/**
 * iOS `SpotRegistrationViewModel` 1:1 — 입력 + multipart 사진 업로드.
 *
 * - 주소는 [AddressSuggestion]로 보관(좌표 포함). 검색→장소상세에서 [applyAddressSelection]로 확정.
 * - 테마는 nullable(미선택 가능), 날짜/시간은 `LocalDate`/`LocalTime`.
 * - 모든 필수값 입력 시 [isRegisterEnabled] true → 헤더 등록 버튼 활성/색상 변화.
 */
@HiltViewModel
class SpotRegistrationViewModel @Inject constructor(
    private val mySpotService: MySpotService,
    private val locationService: LocationService,
) : ViewModel() {

    private val _mode = MutableStateFlow(SpotRegistrationMode.CREATE)
    val mode: StateFlow<SpotRegistrationMode> = _mode.asStateFlow()

    private val _revisionLoadState = MutableStateFlow<LoadState<MySpotDetail>>(LoadState.Idle)
    val revisionLoadState: StateFlow<LoadState<MySpotDetail>> = _revisionLoadState.asStateFlow()

    private val _existingImageUrl = MutableStateFlow<String?>(null)
    val existingImageUrl: StateFlow<String?> = _existingImageUrl.asStateFlow()

    private var revisionSpotId: Long? = null

    private val _imagePayload = MutableStateFlow<ImagePayload?>(null)
    val imagePayload: StateFlow<ImagePayload?> = _imagePayload.asStateFlow()

    private val _selectedImageUri = MutableStateFlow<String?>(null)
    val selectedImageUri: StateFlow<String?> = _selectedImageUri.asStateFlow()

    private val _selectedAddress = MutableStateFlow<AddressSuggestion?>(null)
    val selectedAddress: StateFlow<AddressSuggestion?> = _selectedAddress.asStateFlow()

    private val _distanceText = MutableStateFlow("")
    val distanceText: StateFlow<String> = _distanceText.asStateFlow()

    /** 검색 결과 탭 시 장소상세로 넘길 후보(확정 전). */
    private val _pendingAddress = MutableStateFlow<AddressSuggestion?>(null)
    val pendingAddress: StateFlow<AddressSuggestion?> = _pendingAddress.asStateFlow()

    private val _spotName = MutableStateFlow("")
    val spotName: StateFlow<String> = _spotName.asStateFlow()

    private val _theme = MutableStateFlow<SpotTheme?>(null)
    val theme: StateFlow<SpotTheme?> = _theme.asStateFlow()

    private val _capturedDate = MutableStateFlow<LocalDate?>(null)
    val capturedDate: StateFlow<LocalDate?> = _capturedDate.asStateFlow()

    private val _capturedTime = MutableStateFlow<LocalTime?>(null)
    val capturedTime: StateFlow<LocalTime?> = _capturedTime.asStateFlow()

    private val _comment = MutableStateFlow("")
    val comment: StateFlow<String> = _comment.asStateFlow()

    private val _submission =
        MutableStateFlow<LoadState<SpotRegistrationSubmissionResult>>(LoadState.Idle)
    val submission: StateFlow<LoadState<SpotRegistrationSubmissionResult>> = _submission.asStateFlow()

    /** 모든 필수값 충족 + 업로드 중 아님 → 등록 가능. iOS `isRegisterEnabled` 1:1. */
    val isRegisterEnabled: StateFlow<Boolean> =
        combine(
            _imagePayload,
            _existingImageUrl,
            _selectedAddress,
            _spotName,
            _theme,
            _capturedDate,
            _capturedTime,
            _submission,
            _mode,
        ) { values ->
            val image = values[0] as ImagePayload?
            val existingImageUrl = values[1] as String?
            val address = values[2] as AddressSuggestion?
            val name = values[3] as String
            val theme = values[4] as SpotTheme?
            val date = values[5] as LocalDate?
            val time = values[6] as LocalTime?
            val submission = values[7] as LoadState<*>
            val mode = values[8] as SpotRegistrationMode
            (image != null || mode == SpotRegistrationMode.REVISE && existingImageUrl != null) &&
                name.isNotBlank() &&
                address != null &&
                theme != null &&
                date != null &&
                time != null &&
                submission !is LoadState.Loading
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun loadRevision(spotId: Long) {
        _mode.value = SpotRegistrationMode.REVISE
        revisionSpotId = spotId
        _revisionLoadState.value = LoadState.Loading
        viewModelScope.launch {
            _revisionLoadState.value = runCatching {
                val detail = mySpotService.detail(spotId)
                require(detail.status == MySpotStatus.REJECTED) {
                    "Only rejected spots can be revised."
                }
                val date = LocalDate.parse(detail.capturedDate, DATE_FORMATTER)
                val time = LocalTime.parse(detail.capturedTime, TIME_FORMATTER)

                _spotName.value = detail.name
                _theme.value = detail.theme
                val selectedAddress = AddressSuggestion(
                    name = detail.name,
                    fullAddress = detail.address,
                    latitude = detail.latitude,
                    longitude = detail.longitude,
                )
                _selectedAddress.value = selectedAddress
                _capturedDate.value = date
                _capturedTime.value = time
                _comment.value = detail.comment
                _existingImageUrl.value = detail.imageUrl
                _imagePayload.value = null
                _selectedImageUri.value = detail.imageUrl
                _submission.value = LoadState.Idle

                val current = runCatching { locationService.currentLocation() }.getOrNull()
                _distanceText.value = current?.let { distanceText(it, selectedAddress) }.orEmpty()
                detail
            }.fold(
                onSuccess = { LoadState.Loaded(it) },
                onFailure = { LoadState.Failed(it) },
            )
        }
    }

    fun setImagePayload(payload: ImagePayload?, previewUri: String?) {
        _imagePayload.value = payload
        _selectedImageUri.value = previewUri
    }

    fun setPendingAddress(address: AddressSuggestion) {
        _pendingAddress.value = address
    }

    /** 장소상세 "이 장소가 맞아요" 확정. iOS `applyAddressSelection` 1:1 + 거리 계산. */
    fun applyAddressSelection(address: AddressSuggestion) {
        _selectedAddress.value = address
        viewModelScope.launch {
            val current = runCatching { locationService.currentLocation() }.getOrNull()
            _distanceText.value = current?.let { distanceText(it, address) } ?: ""
        }
    }

    fun setSpotName(value: String) { _spotName.value = value.take(MAX_NAME_LENGTH) }
    fun setComment(value: String) { _comment.value = value.take(MAX_COMMENT_LENGTH) }

    fun toggleTheme(value: SpotTheme) {
        _theme.value = if (_theme.value == value) null else value
    }

    /** 미래 날짜 방지 — 오늘 이후면 오늘로 clamp. */
    fun setCapturedDate(date: LocalDate) {
        _capturedDate.value = if (date.isAfter(LocalDate.now())) LocalDate.now() else date
    }

    fun setCapturedTime(time: LocalTime) { _capturedTime.value = time }

    fun submit() {
        val image = _imagePayload.value
        val mode = _mode.value
        val reviseSpotId = revisionSpotId
        val address = _selectedAddress.value
        val date = _capturedDate.value
        val time = _capturedTime.value
        val hasImage =
            image != null || mode == SpotRegistrationMode.REVISE && _existingImageUrl.value != null
        if (!hasImage || address == null || _spotName.value.isBlank() ||
            _theme.value == null || date == null || time == null
        ) {
            _submission.value = LoadState.Failed(IllegalStateException("입력값이 부족합니다."))
            return
        }
        val draft = SpotDraft(
            name = _spotName.value.trim(),
            theme = _theme.value!!,
            latitude = address.latitude,
            longitude = address.longitude,
            address = address.fullAddress,
            capturedDate = date.format(DATE_FORMATTER),
            capturedTime = time.format(TIME_FORMATTER),
            comment = _comment.value.trim(),
            imageUrl = null,
        )
        viewModelScope.launch {
            _submission.value = LoadState.Loading
            _submission.value = runCatching {
                when (mode) {
                    SpotRegistrationMode.CREATE -> {
                        val created = mySpotService.create(draft, checkNotNull(image))
                        SpotRegistrationSubmissionResult(created.spotId, created.status)
                    }
                    SpotRegistrationMode.REVISE -> {
                        val revised = mySpotService.reviseAndResubmit(
                            spotId = checkNotNull(reviseSpotId),
                            draft = draft,
                            replacementImage = image,
                        )
                        SpotRegistrationSubmissionResult(revised.spotId, revised.status)
                    }
                }
            }
                .fold(
                    onSuccess = { LoadState.Loaded(it) },
                    onFailure = { LoadState.Failed(it) },
                )
        }
    }

    private fun distanceText(from: Coordinates, to: AddressSuggestion): String {
        // 단순 평면 근사(서울권 수 km 표시용). 위도 1도≈111km.
        val dLatKm = (to.latitude - from.latitude) * 111.0
        val dLngKm = (to.longitude - from.longitude) * 111.0 * cos(Math.toRadians(from.latitude))
        val km = sqrt(dLatKm * dLatKm + dLngKm * dLngKm)
        return "%.1fkm".format(km)
    }

    companion object {
        const val MAX_NAME_LENGTH = 20
        const val MAX_COMMENT_LENGTH = 50
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
