package com.pickflow.android.feature.spotdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.SharePayload
import com.pickflow.android.core.services.protocols.ShareIntentService
import com.pickflow.android.core.services.protocols.SpotDetail
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

    private val _spot = MutableStateFlow<LoadState<SpotDetail>>(LoadState.Idle)
    val spot: StateFlow<LoadState<SpotDetail>> = _spot.asStateFlow()

    private val _bookmarked = MutableStateFlow(false)
    val bookmarked: StateFlow<Boolean> = _bookmarked.asStateFlow()

    private val _reportSubmitted = MutableStateFlow(false)
    val reportSubmitted: StateFlow<Boolean> = _reportSubmitted.asStateFlow()

    fun load(spotId: String) {
        viewModelScope.launch {
            _spot.value = LoadState.Loading
            val result = runCatching { spotService.spot(spotId) }
            _spot.value = result.fold(
                onSuccess = { LoadState.Loaded(it) },
                onFailure = { LoadState.Failed(it) },
            )
            _bookmarked.value = result.getOrNull()?.isBookmarked
                ?: bookmarkService.isBookmarked(spotId)
        }
    }

    fun toggleBookmark() {
        val current = (_spot.value as? LoadState.Loaded<SpotDetail>)?.value ?: return
        viewModelScope.launch {
            // TODO(Phase D-3): BookmarkService.add/remove로 교체 후 서버 bookmarkCount 동기화.
            _bookmarked.value = bookmarkService.toggle(current.id.toString())
        }
    }

    fun share() {
        val current = (_spot.value as? LoadState.Loaded<SpotDetail>)?.value ?: return
        viewModelScope.launch {
            // iOS `share()` 와 동일 포맷: "이름 - 코멘트\nhttps://pickflow.app/spot/{id}"
            shareIntentService.share(
                SharePayload(
                    title = "${current.name} - ${current.comment}",
                    url = "https://pickflow.app/spot/${current.id}",
                )
            )
        }
    }

    fun reportInvalidInfo() {
        _reportSubmitted.value = true
    }
}
