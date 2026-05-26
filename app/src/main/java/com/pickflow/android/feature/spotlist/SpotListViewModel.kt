package com.pickflow.android.feature.spotlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.LocationService
import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.SpotListService
import com.pickflow.android.core.services.protocols.SpotSort
import com.pickflow.android.core.services.protocols.SpotTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class SpotListViewModel @Inject constructor(
    private val spotListService: SpotListService,
    private val bookmarkService: BookmarkService,
    private val authService: AuthService,
    private val locationService: LocationService,
) : ViewModel() {

    private val _spots = MutableStateFlow<LoadState<List<Spot>>>(LoadState.Idle)
    val spots: StateFlow<LoadState<List<Spot>>> = _spots.asStateFlow()

    private val _theme = MutableStateFlow<SpotTheme?>(null)
    val theme: StateFlow<SpotTheme?> = _theme.asStateFlow()

    private val _sort = MutableStateFlow(SpotSort.DISTANCE)
    val sort: StateFlow<SpotSort> = _sort.asStateFlow()

    private val _bookmarkedIds = MutableStateFlow<Set<String>>(emptySet())
    val bookmarkedIds: StateFlow<Set<String>> = _bookmarkedIds.asStateFlow()

    private val _showLoginPrompt = MutableStateFlow(false)
    val showLoginPrompt: StateFlow<Boolean> = _showLoginPrompt.asStateFlow()

    // page 기반 페이지네이션 (0-base). nextPage = 다음에 요청할 페이지 번호.
    private var nextPage: Int = 0
    private var hasMore: Boolean = true
    private val accumulated = mutableListOf<Spot>()
    private var currentCoordinates: Coordinates? = null

    fun refresh() {
        nextPage = 0
        hasMore = true
        accumulated.clear()
        loadPage()
    }

    fun selectTheme(theme: SpotTheme?) {
        _theme.value = theme
        refresh()
    }

    fun selectSort(sort: SpotSort) {
        _sort.value = sort
        refresh()
    }

    fun loadNextPage() {
        if (!hasMore) return
        loadPage()
    }

    fun toggleBookmark(spotId: String) {
        viewModelScope.launch {
            if (!authService.isLoggedIn()) {
                _showLoginPrompt.value = true
                return@launch
            }
            bookmarkService.toggle(spotId)
            _bookmarkedIds.value = bookmarkService.bookmarkedIds()
        }
    }

    fun dismissLoginPrompt() {
        _showLoginPrompt.value = false
    }

    private fun loadPage() {
        viewModelScope.launch {
            if (accumulated.isEmpty()) _spots.value = LoadState.Loading
            // iOS와 동일하게 매 호출마다 최신 위치 시도. 권한 없으면 null.
            if (currentCoordinates == null) {
                currentCoordinates = runCatching { locationService.currentLocation() }.getOrNull()
            }
            runCatching {
                spotListService.fetch(
                    theme = _theme.value,
                    page = nextPage,
                    coordinates = currentCoordinates,
                    sort = _sort.value,
                )
            }.onSuccess { page ->
                accumulated.addAll(page.items)
                hasMore = page.hasNext
                nextPage = page.page + 1
                emitState()
            }.onFailure {
                _spots.value = LoadState.Failed(it)
            }
        }
    }

    private fun emitState() {
        _spots.value = when {
            accumulated.isEmpty() -> LoadState.Empty
            else -> LoadState.Loaded(accumulated.toList())
        }
    }
}
