package com.pickflow.android.feature.spotsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.AddressService
import com.pickflow.android.core.services.protocols.AddressSuggestion
import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.LocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.sqrt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SpotSearchViewModel @Inject constructor(
    private val addressService: AddressService,
    private val locationService: LocationService,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _suggestions = MutableStateFlow<LoadState<List<AddressSuggestion>>>(LoadState.Idle)
    val suggestions: StateFlow<LoadState<List<AddressSuggestion>>> = _suggestions.asStateFlow()

    private var currentCoordinates: Coordinates? = null

    fun onQueryChanged(text: String) {
        _query.value = text
        if (text.isBlank()) {
            _suggestions.value = LoadState.Idle
            return
        }
        viewModelScope.launch {
            _suggestions.value = LoadState.Loading
            if (currentCoordinates == null) {
                currentCoordinates = runCatching { locationService.currentLocation() }.getOrNull()
            }
            _suggestions.value = runCatching { addressService.search(text) }
                .fold(
                    onSuccess = { if (it.isEmpty()) LoadState.Empty else LoadState.Loaded(it) },
                    onFailure = { LoadState.Failed(it) },
                )
        }
    }

    /** 현재 위치 기준 거리. 위치 미보유 시 null(미표시). iOS `distanceText(for:)` 1:1. */
    fun distanceText(item: AddressSuggestion): String? {
        val current = currentCoordinates ?: return null
        val dLatKm = (item.latitude - current.latitude) * 111.0
        val dLngKm = (item.longitude - current.longitude) * 111.0 * cos(Math.toRadians(current.latitude))
        val km = sqrt(dLatKm * dLatKm + dLngKm * dLngKm)
        return "%.1fkm".format(km)
    }
}
