package com.pickflow.android.feature.spotlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.Coordinates
import com.pickflow.android.core.services.protocols.LocationService
import com.pickflow.android.core.services.protocols.MoodFilterStore
import com.pickflow.android.core.services.protocols.Spot
import com.pickflow.android.core.services.protocols.SpotListService
import com.pickflow.android.core.services.protocols.SpotSort
import com.pickflow.android.core.services.protocols.SpotTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

@HiltViewModel
class SpotListViewModel @Inject constructor(
    private val spotListService: SpotListService,
    private val bookmarkService: BookmarkService,
    private val authService: AuthService,
    private val locationService: LocationService,
    /** 지도와 공유하는 무드 선택. 어느 쪽에서 바꿔도 양쪽이 같이 움직인다. */
    private val moodFilterStore: MoodFilterStore,
) : ViewModel() {

    init {
        // 지도에서 무드를 바꿨을 때 리스트도 따라와야 한다.
        // drop(1) — 최초 값은 화면의 refresh() 가 이미 처리하므로 중복 요청을 막는다.
        viewModelScope.launch {
            moodFilterStore.selected.drop(1).collect { refresh() }
        }
    }

    private val _spots = MutableStateFlow<LoadState<List<Spot>>>(LoadState.Idle)
    val spots: StateFlow<LoadState<List<Spot>>> = _spots.asStateFlow()

    /**
     * 다중선택 무드 필터. 빈 Set = 필터 없음(전체 스팟).
     * 실제 상태는 [MoodFilterStore]가 들고 있어 지도와 공유된다.
     */
    val themes: StateFlow<Set<SpotTheme>> = moodFilterStore.selected

    // 기본 정렬은 추천 순(RECOMMENDED). 가까운 순(DISTANCE)은 위치 권한이 있을 때만 선택 가능.
    private val _sort = MutableStateFlow(SpotSort.RECOMMENDED)
    val sort: StateFlow<SpotSort> = _sort.asStateFlow()

    // 응답의 isBookmarked 로 채운다(서버가 단일 출처). 사용자의 낙관적 토글이 그 위에 얹힌다.
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
        _bookmarkedIds.value = emptySet()
        loadPage()
    }

    /**
     * 무드 다중선택 토글 — 이미 선택돼 있으면 그 하나만 해제한다. 전부 해제하면 전체 조회.
     *
     * [refresh]가 loadGeneration 을 올리므로 직전 필터의 늦은 응답은 폐기된다.
     */
    fun toggleTheme(theme: SpotTheme) = moodFilterStore.toggle(theme)

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
                    themes = moodFilterStore.selected.value,
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
                // 신규 항목의 서버값만 합친다. 이미 화면에 있던 id 를 덮어쓰면
                // 방금 낙관적으로 해제한 북마크가 stale 응답으로 되살아난다.
                _bookmarkedIds.value += newItems.filter { it.isBookmarked }.map { it.id }
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
