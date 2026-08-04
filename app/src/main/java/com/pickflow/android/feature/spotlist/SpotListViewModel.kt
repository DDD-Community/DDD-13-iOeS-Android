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

    // 기본 정렬은 북마크 순(RECOMMENDED). 가까운 순(DISTANCE)은 위치 권한이 있을 때만 선택 가능.
    private val _sort = MutableStateFlow(SpotSort.RECOMMENDED)
    val sort: StateFlow<SpotSort> = _sort.asStateFlow()

    private val _bookmarkedIds = MutableStateFlow<Set<String>>(emptySet())
    val bookmarkedIds: StateFlow<Set<String>> = _bookmarkedIds.asStateFlow()

    private val _showLoginPrompt = MutableStateFlow(false)
    val showLoginPrompt: StateFlow<Boolean> = _showLoginPrompt.asStateFlow()

    /** iOS `toast: String?` 1:1 — 다음 페이지 실패 등 단발성 안내. */
    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    fun consumeToast() { _toast.value = null }

    // page 기반 페이지네이션 (0-base). nextPage = 다음에 요청할 페이지 번호.
    private var nextPage: Int = 0
    private var hasMore: Boolean = true
    // 페이지 로딩 in-flight 가드 — 빠른 스크롤로 loadNextPage 가 연타돼도 중복 요청을 막는다.
    private var isLoadingPage: Boolean = false
    // refresh/필터 전환마다 증가. 이전 세대의 늦은 응답이 stale 데이터를 섞거나 가드를 잘못 푸는 걸 막는다.
    private var loadGeneration: Int = 0
    private val accumulated = mutableListOf<Spot>()
    // 누적된 spotId 집합 — 서버가 페이지 경계에서 같은 스팟을 겹쳐 내려도 중복 key 크래시를 방지.
    private val accumulatedIds = mutableSetOf<String>()
    private var currentCoordinates: Coordinates? = null

    fun refresh() {
        loadGeneration++
        nextPage = 0
        hasMore = true
        isLoadingPage = false
        accumulated.clear()
        accumulatedIds.clear()
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
        if (!hasMore || isLoadingPage) return
        loadPage()
    }

    fun toggleBookmark(spotId: String) {
        viewModelScope.launch {
            if (!authService.isLoggedIn()) {
                _showLoginPrompt.value = true
                return@launch
            }
            // iOS `SpotListViewModel.bookmarkTapped` 1:1 — 낙관적 토글 + 실패 시 롤백.
            val wasBookmarked = spotId in _bookmarkedIds.value
            _bookmarkedIds.value = if (wasBookmarked) {
                _bookmarkedIds.value - spotId
            } else {
                _bookmarkedIds.value + spotId
            }
            runCatching {
                if (wasBookmarked) bookmarkService.remove(spotId)
                else bookmarkService.add(spotId)
            }.onFailure {
                _bookmarkedIds.value = if (wasBookmarked) {
                    _bookmarkedIds.value + spotId
                } else {
                    _bookmarkedIds.value - spotId
                }
                _toast.value = "북마크 변경에 실패했어요."
            }
        }
    }

    fun dismissLoginPrompt() {
        _showLoginPrompt.value = false
    }

    private fun loadPage() {
        val generation = loadGeneration
        val isFirstPage = accumulated.isEmpty()
        isLoadingPage = true
        viewModelScope.launch {
            if (isFirstPage) _spots.value = LoadState.Loading
            // iOS와 동일하게 매 호출마다 최신 위치 시도. 권한 없으면 null.
            if (currentCoordinates == null) {
                currentCoordinates = runCatching { locationService.currentLocation() }.getOrNull()
            }
            val result = runCatching {
                spotListService.fetch(
                    theme = _theme.value,
                    page = nextPage,
                    coordinates = currentCoordinates,
                    sort = _sort.value,
                )
            }
            // refresh/필터 전환으로 세대가 바뀌었으면 이 응답은 폐기 — stale 누적/가드 해제 방지.
            if (generation != loadGeneration) return@launch
            result.onSuccess { page ->
                // 이미 누적된 id 는 걸러 append → LazyGrid 중복 key 크래시 방지.
                val newItems = page.items.filter { accumulatedIds.add(it.id) }
                accumulated.addAll(newItems)
                hasMore = page.hasNext
                nextPage = page.page + 1
                emitState()
            }.onFailure {
                if (isFirstPage) {
                    _spots.value = LoadState.Failed(it)
                } else {
                    // iOS `loadNextPageIfNeeded` 1:1 — 누적 리스트 유지 + 토스트.
                    _toast.value = "다음 페이지를 불러오지 못했어요."
                }
            }
            isLoadingPage = false
        }
    }

    private fun emitState() {
        _spots.value = when {
            accumulated.isEmpty() -> LoadState.Empty
            else -> LoadState.Loaded(accumulated.toList())
        }
    }
}
