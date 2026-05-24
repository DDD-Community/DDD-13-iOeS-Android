package com.pickflow.android.feature.spotlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.SpotListService
import com.pickflow.android.core.services.protocols.SpotTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SpotSort(val label: String) {
    LATEST("최신순"),
    DISTANCE("거리순"),
    POPULAR("인기순"),
}

@HiltViewModel
class SpotListViewModel @Inject constructor(
    private val spotListService: SpotListService,
    private val bookmarkService: BookmarkService,
    private val authService: AuthService,
) : ViewModel() {

    private val _spots = MutableStateFlow<LoadState<List<Spot>>>(LoadState.Idle)
    val spots: StateFlow<LoadState<List<Spot>>> = _spots.asStateFlow()

    private val _theme = MutableStateFlow<SpotTheme?>(null)
    val theme: StateFlow<SpotTheme?> = _theme.asStateFlow()

    private val _sort = MutableStateFlow(SpotSort.LATEST)
    val sort: StateFlow<SpotSort> = _sort.asStateFlow()

    private val _bookmarkedIds = MutableStateFlow<Set<String>>(emptySet())
    val bookmarkedIds: StateFlow<Set<String>> = _bookmarkedIds.asStateFlow()

    private val _showLoginPrompt = MutableStateFlow(false)
    val showLoginPrompt: StateFlow<Boolean> = _showLoginPrompt.asStateFlow()

    private var nextCursor: String? = null
    private val accumulated = mutableListOf<Spot>()
    private val pageSize: Int = 20

    fun refresh() {
        nextCursor = null
        accumulated.clear()
        loadPage()
    }

    fun selectTheme(theme: SpotTheme?) {
        _theme.value = theme
        refresh()
    }

    fun selectSort(sort: SpotSort) {
        _sort.value = sort
        emitSorted()
    }

    fun loadNextPage() {
        if (nextCursor == null && accumulated.isNotEmpty()) return
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
            runCatching {
                spotListService.fetch(_theme.value, nextCursor, pageSize)
            }.onSuccess { page ->
                accumulated.addAll(page.items)
                nextCursor = page.nextCursor
                emitSorted()
            }.onFailure {
                _spots.value = LoadState.Failed(it)
            }
        }
    }

    private fun emitSorted() {
        _spots.value = when {
            accumulated.isEmpty() -> LoadState.Empty
            else -> LoadState.Loaded(sorted(accumulated))
        }
    }

    private fun sorted(spots: List<Spot>): List<Spot> = when (_sort.value) {
        SpotSort.LATEST -> spots.sortedByDescending { it.id }
        SpotSort.DISTANCE -> spots.sortedBy { it.latitude }
        SpotSort.POPULAR -> spots.sortedBy { it.name }
    }
}
