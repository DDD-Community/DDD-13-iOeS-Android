package com.pickflow.android.feature.spotregistration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.SpotDraft
import com.pickflow.android.core.services.protocols.SpotService
import com.pickflow.android.core.services.protocols.SpotTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SpotRegistrationViewModel @Inject constructor(
    private val spotService: SpotService,
) : ViewModel() {

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _theme = MutableStateFlow(SpotTheme.SUNSET)
    val theme: StateFlow<SpotTheme> = _theme.asStateFlow()

    private val _address = MutableStateFlow("")
    val address: StateFlow<String> = _address.asStateFlow()

    private val _capturedDate = MutableStateFlow("")
    val capturedDate: StateFlow<String> = _capturedDate.asStateFlow()

    private val _capturedTime = MutableStateFlow("")
    val capturedTime: StateFlow<String> = _capturedTime.asStateFlow()

    private val _comment = MutableStateFlow("")
    val comment: StateFlow<String> = _comment.asStateFlow()

    private val _coordinates = MutableStateFlow<Pair<Double, Double>?>(null)

    private val _imageUrl = MutableStateFlow<String?>(null)

    private val _submission = MutableStateFlow<LoadState<Spot>>(LoadState.Idle)
    val submission: StateFlow<LoadState<Spot>> = _submission.asStateFlow()

    fun setName(value: String) { _name.value = value.take(MAX_NAME_LENGTH) }
    fun setTheme(value: SpotTheme) { _theme.value = value }
    fun setAddress(value: String) { _address.value = value }
    fun setCapturedDate(value: String) { _capturedDate.value = value }
    fun setCapturedTime(value: String) { _capturedTime.value = value }
    fun setComment(value: String) { _comment.value = value.take(MAX_COMMENT_LENGTH) }
    fun setCoordinates(latitude: Double, longitude: Double) {
        _coordinates.value = latitude to longitude
    }
    fun setImageUrl(url: String?) { _imageUrl.value = url }

    fun isValid(): Boolean =
        _name.value.isNotBlank() &&
            _address.value.isNotBlank() &&
            _coordinates.value != null &&
            _capturedDate.value.isNotBlank() &&
            _capturedTime.value.isNotBlank()

    fun submit() {
        if (!isValid()) {
            _submission.value = LoadState.Failed(IllegalStateException("입력값이 부족합니다."))
            return
        }
        val (lat, lng) = _coordinates.value!!
        val draft = SpotDraft(
            name = _name.value,
            theme = _theme.value,
            latitude = lat,
            longitude = lng,
            address = _address.value,
            capturedDate = _capturedDate.value,
            capturedTime = _capturedTime.value,
            comment = _comment.value,
            imageUrl = _imageUrl.value,
        )
        viewModelScope.launch {
            _submission.value = LoadState.Loading
            _submission.value = runCatching { spotService.register(draft) }
                .fold(
                    onSuccess = { LoadState.Loaded(it) },
                    onFailure = { LoadState.Failed(it) },
                )
        }
    }

    companion object {
        const val MAX_NAME_LENGTH = 20
        const val MAX_COMMENT_LENGTH = 50
    }
}
