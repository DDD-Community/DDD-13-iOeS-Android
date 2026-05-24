package com.pickflow.android.feature.spotsearch

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.AddressService
import com.pickflow.android.core.services.protocols.AddressSuggestion
import com.pickflow.android.core.services.protocols.LocationService
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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

    fun onQueryChanged(text: String) {
        _query.value = text
        if (text.isBlank()) {
            _suggestions.value = LoadState.Idle
            return
        }
        viewModelScope.launch {
            _suggestions.value = LoadState.Loading
            _suggestions.value = runCatching { addressService.search(text) }
                .fold(
                    onSuccess = { if (it.isEmpty()) LoadState.Empty else LoadState.Loaded(it) },
                    onFailure = { LoadState.Failed(it) },
                )
        }
    }

    fun useCurrentLocation(onResolved: (AddressSuggestion?) -> Unit) {
        viewModelScope.launch {
            val coords = locationService.currentLocation() ?: run {
                onResolved(null); return@launch
            }
            onResolved(
                AddressSuggestion(
                    displayName = "현재 위치",
                    latitude = coords.latitude,
                    longitude = coords.longitude,
                )
            )
        }
    }
}
