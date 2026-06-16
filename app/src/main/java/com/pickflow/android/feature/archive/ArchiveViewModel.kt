package com.pickflow.android.feature.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pickflow.android.common.ui.LoadState
import com.pickflow.android.core.services.protocols.ArchiveService
import com.pickflow.android.core.services.protocols.AuthService
import com.pickflow.android.core.services.protocols.BookmarkService
import com.pickflow.android.core.services.protocols.ImagePayload
import com.pickflow.android.core.services.protocols.LocationService
import com.pickflow.android.core.services.protocols.MySpot
import com.pickflow.android.core.services.protocols.MySpotService
import com.pickflow.android.core.services.protocols.SavedSpot
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * iOS `ArchiveViewModel.LoadState` 1:1 대응.
 *
 * 단순 `LoadState<List<SavedSpot>>`로는 표현할 수 없는 `SignedOut` 상태를 포함하므로
 * 보관함 전용 sealed class로 분리한다.
 */
sealed class ArchiveLoadState {
    data object SignedOut : ArchiveLoadState()
    data object Loading : ArchiveLoadState()
    data object Empty : ArchiveLoadState()
    data class Loaded(val items: List<SavedSpot>, val hasNext: Boolean) : ArchiveLoadState()
    data class Failed(val message: String) : ArchiveLoadState()
}

/**
 * iOS `ArchiveViewModel` 1:1 이식 (소셜 로그인 분기는 Android 컨벤션상
 * `onRequireLogin` callback 으로 위임 — `MyProfileScreen` 패턴 동일).
 *
 * - state / selectedTab / isLoadingNextPage / archiveName / archiveImageUrl /
 *   coverImageBytes / toast 를 개별 StateFlow 로 노출 (CLAUDE.md §4).
 * - 페이지네이션, 북마크 해제 낙관적 업데이트, 보관함 이름/이미지 변경 모두 iOS 동일.
 */
@HiltViewModel
class ArchiveViewModel @Inject constructor(
    private val archiveService: ArchiveService,
    private val bookmarkService: BookmarkService,
    private val authService: AuthService,
    private val locationService: LocationService,
    private val mySpotService: MySpotService,
) : ViewModel() {

    private val _state = MutableStateFlow<ArchiveLoadState>(ArchiveLoadState.Loading)
    val state: StateFlow<ArchiveLoadState> = _state.asStateFlow()

    private val _selectedTab = MutableStateFlow(ArchiveTab.SavedSpots)
    val selectedTab: StateFlow<ArchiveTab> = _selectedTab.asStateFlow()

    private val _isLoadingNextPage = MutableStateFlow(false)
    val isLoadingNextPage: StateFlow<Boolean> = _isLoadingNextPage.asStateFlow()

    private val _archiveName = MutableStateFlow("나의 보관함")
    val archiveName: StateFlow<String> = _archiveName.asStateFlow()

    private val _archiveImageUrl = MutableStateFlow<String?>(null)
    val archiveImageUrl: StateFlow<String?> = _archiveImageUrl.asStateFlow()

    private val _coverImageBytes = MutableStateFlow<ByteArray?>(null)
    val coverImageBytes: StateFlow<ByteArray?> = _coverImageBytes.asStateFlow()

    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()

    /**
     * "나만의 스팟" 탭 — `MySpotService.list()` 결과. SignedOut 분기는 상위 [state] 가
     * 처리하므로 표준 [LoadState] 사용. tabChanged(MySpots) 진입 시 lazy fetch.
     */
    private val _mySpots = MutableStateFlow<LoadState<List<MySpot>>>(LoadState.Idle)
    val mySpots: StateFlow<LoadState<List<MySpot>>> = _mySpots.asStateFlow()

    private var currentPage: Int = 0
    private var hasNext: Boolean = false
    private var myCurrentPage: Int = 0
    private var myHasNext: Boolean = false
    private val myAccumulated = mutableListOf<MySpot>()
    private var currentCoordinates: com.pickflow.android.core.services.protocols.Coordinates? = null

    fun onAppear() {
        viewModelScope.launch {
            if (!authService.isLoggedIn()) {
                _state.value = ArchiveLoadState.SignedOut
                return@launch
            }
            currentCoordinates = runCatching { locationService.currentLocation() }.getOrNull()
            coroutineScope {
                val a = async { fetchArchiveInfo() }
                val b = async { fetchArchive() }
                awaitAll(a, b)
            }
        }
    }

    fun tabChanged(tab: ArchiveTab) {
        _selectedTab.value = tab
        // MySpots 탭 진입 시 한 번만 lazy fetch — 이미 Loaded/Empty/Failed 면 재요청 X.
        if (tab == ArchiveTab.MySpots && _mySpots.value is LoadState.Idle) {
            fetchMySpots()
        }
    }

    /** 나만의 스팟 페이지네이션 — 끝에서 3번째 아이템에 도달하면 다음 페이지 호출. */
    fun loadNextMySpotPageIfNeeded(currentItem: MySpot) {
        val loaded = _mySpots.value as? LoadState.Loaded ?: return
        if (!myHasNext || _isLoadingNextPage.value) return

        val items = loaded.value
        val triggerIndex = (items.size - 3).coerceAtLeast(0)
        val index = items.indexOfFirst { it.id == currentItem.id }
        if (index < 0 || index < triggerIndex) return

        _isLoadingNextPage.value = true
        viewModelScope.launch {
            runCatching {
                mySpotService.list(page = myCurrentPage + 1, coordinates = currentCoordinates)
            }.onSuccess { page ->
                myCurrentPage = page.page
                myHasNext = page.hasNext
                myAccumulated.addAll(page.items)
                _mySpots.value = LoadState.Loaded(myAccumulated.toList())
            }.onFailure {
                showToast("다음 페이지를 불러오지 못했어요.")
            }
            _isLoadingNextPage.value = false
        }
    }

    private fun fetchMySpots() {
        _mySpots.value = LoadState.Loading
        myCurrentPage = 0
        myHasNext = false
        myAccumulated.clear()
        viewModelScope.launch {
            runCatching {
                mySpotService.list(page = 0, coordinates = currentCoordinates)
            }.onSuccess { page ->
                myCurrentPage = page.page
                myHasNext = page.hasNext
                myAccumulated.addAll(page.items)
                _mySpots.value = if (page.items.isEmpty()) LoadState.Empty
                else LoadState.Loaded(myAccumulated.toList())
            }.onFailure {
                _mySpots.value = LoadState.Failed(it)
            }
        }
    }

    fun renameArchive(name: String) {
        val trimmed = name.take(MAX_NAME_LENGTH)
        if (trimmed.isBlank()) return
        val previous = _archiveName.value
        _archiveName.value = trimmed
        viewModelScope.launch {
            runCatching { archiveService.updateName(trimmed) }
                .onSuccess { _archiveName.value = it.name }
                .onFailure {
                    _archiveName.value = previous
                    showToast("이름 변경에 실패했어요.")
                }
        }
    }

    fun updateCoverImage(payload: ImagePayload) {
        _coverImageBytes.value = payload.bytes
        viewModelScope.launch {
            runCatching { archiveService.updateImage(payload) }
                .onSuccess { archive ->
                    _archiveName.value = archive.name
                    _archiveImageUrl.value = archive.imageUrl
                    showToast("커버 이미지가 변경되었습니다.")
                }
                .onFailure {
                    _coverImageBytes.value = null
                    showToast("이미지 업로드에 실패했어요.")
                }
        }
    }

    fun showToast(message: String) {
        _toast.value = message
        viewModelScope.launch {
            delay(TOAST_DURATION_MS)
            if (_toast.value == message) _toast.value = null
        }
    }

    fun dismissToast() {
        _toast.value = null
    }

    fun loadNextPageIfNeeded(currentItem: SavedSpot) {
        val loaded = _state.value as? ArchiveLoadState.Loaded ?: return
        if (!loaded.hasNext || _isLoadingNextPage.value) return

        val items = loaded.items
        val triggerIndex = (items.size - 3).coerceAtLeast(0)
        val index = items.indexOfFirst { it.id == currentItem.id }
        if (index < 0 || index < triggerIndex) return

        _isLoadingNextPage.value = true
        viewModelScope.launch {
            runCatching {
                bookmarkService.savedSpots(page = currentPage + 1, coordinates = currentCoordinates)
            }.onSuccess { page ->
                currentPage = page.page
                hasNext = page.hasNext
                _state.value = ArchiveLoadState.Loaded(items = items + page.items, hasNext = page.hasNext)
            }.onFailure {
                showToast("다음 페이지를 불러오지 못했어요.")
            }
            _isLoadingNextPage.value = false
        }
    }

    fun bookmarkTapped(spotId: Long) {
        val loaded = _state.value as? ArchiveLoadState.Loaded ?: return
        val removedIndex = loaded.items.indexOfFirst { it.id == spotId }
        if (removedIndex < 0) return
        val removedItem = loaded.items[removedIndex]

        val remaining = loaded.items.toMutableList().apply { removeAt(removedIndex) }
        _state.value =
            if (remaining.isEmpty()) ArchiveLoadState.Empty
            else ArchiveLoadState.Loaded(items = remaining.toList(), hasNext = loaded.hasNext)

        viewModelScope.launch {
            runCatching { bookmarkService.remove(spotId.toString()) }
                .onFailure {
                    val restored = remaining.toMutableList().apply {
                        add(removedIndex.coerceAtMost(size), removedItem)
                    }
                    _state.value = ArchiveLoadState.Loaded(items = restored.toList(), hasNext = loaded.hasNext)
                    showToast("북마크 해제에 실패했어요.")
                }
        }
    }

    private suspend fun fetchArchiveInfo() {
        runCatching { archiveService.fetch() }
            .onSuccess { archive ->
                _archiveName.value = archive.name
                _archiveImageUrl.value = archive.imageUrl
            }
        // 실패는 iOS와 동일하게 조용히 무시 — 기본값 유지.
    }

    private suspend fun fetchArchive() {
        _state.value = ArchiveLoadState.Loading
        currentPage = 0
        hasNext = false
        runCatching {
            bookmarkService.savedSpots(page = 0, coordinates = currentCoordinates)
        }.onSuccess { page ->
            currentPage = page.page
            hasNext = page.hasNext
            _state.value = when {
                page.items.isEmpty() -> ArchiveLoadState.Empty
                else -> ArchiveLoadState.Loaded(items = page.items, hasNext = page.hasNext)
            }
        }.onFailure {
            _state.value = ArchiveLoadState.Failed(it.message ?: "알 수 없는 오류가 발생했어요.")
        }
    }

    private companion object {
        const val MAX_NAME_LENGTH = 15
        const val TOAST_DURATION_MS = 2_000L
    }
}
