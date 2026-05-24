package com.pickflow.android.feature.spotdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.SharePayload
import com.pickflow.android.core.services.protocols.ShareIntentService
import com.pickflow.android.core.services.protocols.Spot
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
) : ViewModel() {

    private val _spot = MutableStateFlow<LoadState<Spot>>(LoadState.Idle)
    val spot: StateFlow<LoadState<Spot>> = _spot.asStateFlow()

    private val _bookmarked = MutableStateFlow(false)
    val bookmarked: StateFlow<Boolean> = _bookmarked.asStateFlow()

    private val _reportSubmitted = MutableStateFlow(false)
    val reportSubmitted: StateFlow<Boolean> = _reportSubmitted.asStateFlow()

    fun load(spotId: String) {
        viewModelScope.launch {
            _spot.value = LoadState.Loading
            _spot.value = runCatching { spotService.spot(spotId) }
                .fold(
                    onSuccess = { LoadState.Loaded(it) },
                    onFailure = { LoadState.Failed(it) },
                )
            _bookmarked.value = bookmarkService.isBookmarked(spotId)
        }
    }

    fun toggleBookmark() {
        val spot = (_spot.value as? LoadState.Loaded<Spot>)?.value ?: return
        viewModelScope.launch {
            _bookmarked.value = bookmarkService.toggle(spot.id)
        }
    }

    fun share() {
        val spot = (_spot.value as? LoadState.Loaded<Spot>)?.value ?: return
        viewModelScope.launch {
            shareIntentService.share(
                SharePayload(title = spot.name, url = "pickflow://spot/${spot.id}")
            )
        }
    }

    fun reportInvalidInfo() {
        _reportSubmitted.value = true
    }
}
