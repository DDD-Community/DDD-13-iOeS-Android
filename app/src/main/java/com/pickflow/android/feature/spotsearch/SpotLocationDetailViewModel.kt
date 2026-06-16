package com.pickflow.android.feature.spotsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.core.services.protocols.AddressService
import com.pickflow.android.core.services.protocols.AddressSuggestion
import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.LocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * iOS `SpotLocationDetailViewModel` 1:1 — 핀 이동 후 좌표 확정.
 * 핀이 원래 좌표와 같으면 후보 그대로, 다르면 reverseGeocode 로 주소 갱신(실패 시 좌표만 교체).
 */
@HiltViewModel
class SpotLocationDetailViewModel @Inject constructor(
    private val addressService: AddressService,
    private val locationService: LocationService,
) : ViewModel() {

    private val _userLocation = MutableStateFlow<Coordinates?>(null)
    val userLocation: StateFlow<Coordinates?> = _userLocation.asStateFlow()

    private val _isConfirming = MutableStateFlow(false)
    val isConfirming: StateFlow<Boolean> = _isConfirming.asStateFlow()

    fun refreshUserLocation() {
        viewModelScope.launch {
            _userLocation.value = runCatching { locationService.currentLocation() }.getOrNull()
        }
    }

    /** 현재 위치를 요청하고 결과를 콜백(카메라 이동용)으로 전달. */
    fun moveToCurrentLocation(onResolved: (Coordinates?) -> Unit) {
        viewModelScope.launch {
            val coords = runCatching { locationService.currentLocation() }.getOrNull()
            if (coords != null) _userLocation.value = coords
            onResolved(coords)
        }
    }

    fun confirm(
        markerLatitude: Double,
        markerLongitude: Double,
        candidate: AddressSuggestion,
        onConfirmed: (AddressSuggestion) -> Unit,
    ) {
        viewModelScope.launch {
            _isConfirming.value = true
            val unchanged = abs(markerLatitude - candidate.latitude) < 1e-6 &&
                abs(markerLongitude - candidate.longitude) < 1e-6
            val result = if (unchanged) {
                candidate
            } else {
                runCatching { addressService.reverseGeocode(markerLatitude, markerLongitude) }
                    .getOrNull()
                    ?: candidate.copy(latitude = markerLatitude, longitude = markerLongitude)
            }
            _isConfirming.value = false
            onConfirmed(result)
        }
    }
}
